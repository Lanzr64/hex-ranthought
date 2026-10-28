package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.casting.actions.spells.great.OpTeleport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Exchanges the positions of the two entity iotas at the top of the stack. */
public final class SwapEntitiesAction implements SpellAction {
    private static final long COST = 2 * MediaConstants.SHARD_UNIT;

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
        Entity first = OperatorUtils.getEntity(args, 0, getArgc());
        Entity second = OperatorUtils.getEntity(args, 1, getArgc());
        if (first == second)
            throw new MishapBadEntity(second, Component.translatable("hex_ranthought.mishap.distinct_entities"));
        env.assertEntityInRange(first);
        env.assertEntityInRange(second);
        assertTeleportable(first);
        assertTeleportable(second);

        Vec3 firstPos = first.position();
        Vec3 secondPos = second.position();
        if (!HexConfig.server().canTeleportInThisDimension(env.getWorld().dimension()))
            throw new MishapBadLocation(secondPos, "bad_dimension");
        assertDestination(env, firstPos);
        assertDestination(env, secondPos);

        return new Result(new SwapSpell(first, second), COST,
                List.of(ParticleSpray.cloud(firstPos, 2, 10), ParticleSpray.cloud(secondPos, 2, 10)), 0);
    }

    private static void assertTeleportable(Entity entity) {
        if (entity.getType().is(HexTags.Entities.CANNOT_TELEPORT))
            throw new MishapImmuneEntity(entity);
        if (entity.getType().is(HexTags.Entities.STICKY_TELEPORTERS)) {
            for (Entity passenger : entity.getPassengers()) {
                if (passenger.getType().is(HexTags.Entities.CANNOT_TELEPORT))
                    throw new MishapImmuneEntity(passenger);
            }
        }
    }

    private static void assertDestination(CastingEnvironment env, Vec3 position) {
        env.assertVecInWorld(position);
        if (!env.isVecInWorld(position.subtract(0, 1, 0)))
            throw new MishapBadLocation(position, "too_close_to_out");
    }

    private record SwapSpell(Entity first, Entity second) implements RenderedSpell {
        @Override public void cast(CastingEnvironment env) {
            if (!first.isAlive() || !second.isAlive() || first == second
                    || first.level() != env.getWorld() || second.level() != env.getWorld()) return;

            Vec3 firstPos = first.position();
            Vec3 secondPos = second.position();
            OpTeleport.INSTANCE.teleportRespectSticky(first, secondPos.subtract(firstPos), env.getWorld());
            OpTeleport.INSTANCE.teleportRespectSticky(second, firstPos.subtract(second.position()), env.getWorld());
        }

        @Override public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }
    }
}
