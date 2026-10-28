package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.lanzr.hexranthought.network.ArcNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A straight cut from a position through the displacement given by the second iota. */
public final class SpatialBladeAction implements SpellAction {
    private static final double EPSILON = 1.0e-7;
    private static final double BASE_DAMAGE = 25.0;
    private static final float MAX_DAMAGE = 200.0f;

    @Override public int getArgc() { return 2; }

    @Override public boolean hasCastingSound(CastingEnvironment env) {
        return SpellAction.DefaultImpls.hasCastingSound(this, env);
    }

    @Override public boolean awardsCastingStat(CastingEnvironment env) {
        return SpellAction.DefaultImpls.awardsCastingStat(this, env);
    }

    @Override public Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userData) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userData);
    }

    @Override public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return SpellAction.DefaultImpls.operate(this, env, image, continuation);
    }

    @Override public Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Vec3 start = OperatorUtils.getVec3(args, 0, getArgc());
        Vec3 direction = OperatorUtils.getVec3(args, 1, getArgc());
        Vec3 end = start.add(direction);
        double length = direction.length();
        if (!finite(start) || !finite(direction) || !finite(end) || !Double.isFinite(length) || length <= EPSILON)
            throw MishapInvalidIota.ofType(args.get(1), 1, "non-zero finite vector");

        env.assertVecInWorld(start);
        env.assertVecInWorld(end);
        env.assertVecInRange(start);
        env.assertVecInRange(end);

        long cost = costFor(length);
        return new Result(new BladeSpell(start, end), cost, List.of(), 0);
    }

    static long costFor(double length) {
        return (long) Math.ceil(length * 3.0 * MediaConstants.DUST_UNIT);
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private record BladeSpell(Vec3 start, Vec3 end) implements RenderedSpell {
        @Override public void cast(CastingEnvironment env) {
            ServerLevel level = env.getWorld();
            double length = start.distanceTo(end);
            AABB sweep = new AABB(start, end).inflate(EPSILON);
            Map<LivingEntity, Set<AABB>> targets = new LinkedHashMap<>();
            for (Entity candidate : level.getEntities((Entity) null, sweep,
                    entity -> entity instanceof LivingEntity || entity instanceof PartEntity<?>)) {
                if (candidate instanceof PartEntity<?> part) {
                    if (part.getParent() instanceof LivingEntity living && validTarget(living))
                        targets.computeIfAbsent(living, ignored -> new LinkedHashSet<>()).add(part.getBoundingBox());
                } else if (candidate instanceof LivingEntity living && validTarget(living)) {
                    Set<AABB> boxes = targets.computeIfAbsent(living, ignored -> new LinkedHashSet<>());
                    if (living.isMultipartEntity() && living.getParts() != null && living.getParts().length > 0) {
                        for (PartEntity<?> part : living.getParts()) boxes.add(part.getBoundingBox());
                    } else {
                        boxes.add(living.getBoundingBox());
                    }
                }
            }

            Map<LivingEntity, Float> damageByTarget = new LinkedHashMap<>();
            for (Map.Entry<LivingEntity, Set<AABB>> entry : targets.entrySet()) {
                double damage = 0;
                for (AABB box : entry.getValue()) {
                    damage += damageForBox(start, end, length, box);
                }
                if (damage > 0) damageByTarget.put(entry.getKey(), (float) Math.min(MAX_DAMAGE, damage));
            }

            int color = env.getPigment().getColorProvider()
                    .getColor((float) level.getGameTime(), start) & 0xFFFFFF;
            ArcNetwork.sendStraight(level, start, end, color);
            if (damageByTarget.size() > 1) {
                ServerPlayer caster = env.getCaster();
                if (caster != null) caster.hurt(level.damageSources().magic(), 2.0f);
                return;
            }
            Entity caster = env.getCastingEntity();
            for (Map.Entry<LivingEntity, Float> entry : damageByTarget.entrySet()) {
                entry.getKey().hurt(caster == null ? level.damageSources().magic()
                        : level.damageSources().indirectMagic(caster, caster), entry.getValue());
            }
        }

        @Override public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }
    }

    private static boolean validTarget(LivingEntity living) {
        return living.isAlive() && !living.isSpectator() && !(living instanceof ArmorStand);
    }

    static double damageForBox(Vec3 start, Vec3 end, double length, AABB box) {
        double penetration = penetrationLength(start, end, length, box);
        double boxLength = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        return penetration > 0 && boxLength > EPSILON ? BASE_DAMAGE * penetration / boxLength : 0;
    }

    /** Returns a chord only when both entry and exit lie on this segment. */
    static double penetrationLength(Vec3 start, Vec3 end, double length, AABB box) {
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double[] origins = {start.x, start.y, start.z};
        double[] deltas = {end.x - start.x, end.y - start.y, end.z - start.z};
        double[] mins = {box.minX, box.minY, box.minZ};
        double[] maxs = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(deltas[axis]) <= EPSILON) {
                if (origins[axis] <= mins[axis] || origins[axis] >= maxs[axis]) return 0;
                continue;
            }
            double first = (mins[axis] - origins[axis]) / deltas[axis];
            double second = (maxs[axis] - origins[axis]) / deltas[axis];
            enter = Math.max(enter, Math.min(first, second));
            exit = Math.min(exit, Math.max(first, second));
            if (exit <= enter) return 0;
        }
        return enter >= 0 && exit <= 1 && exit - enter > EPSILON ? (exit - enter) * length : 0;
    }
}
