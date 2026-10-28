package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import com.lanzr.hexranthought.network.ArcNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Damages each distinct living target in a nearest-neighbor chain. */
public final class ThaumicShockAction implements SpellAction {
    private static final long COST = 4 * MediaConstants.SHARD_UNIT;

    @Override public int getArgc() { return 1; }

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
        SpellList input = OperatorUtils.getList(args, 0, getArgc());
        LinkedHashSet<LivingEntity> distinct = new LinkedHashSet<>();
        for (Iota iota : input) {
            if (!(iota instanceof EntityIota entityIota))
                throw MishapInvalidIota.ofType(iota, 0, "entity");
            Entity entity = entityIota.getEntity();
            if (entity instanceof LivingEntity living && !(entity instanceof ArmorStand))
                distinct.add(living);
        }
        if (distinct.size() >= 2)
            for (LivingEntity target : distinct) env.assertEntityInRange(target);
        ServerPlayer caster = env.getCaster();
        if (caster == null || lightningRodSlot(caster.getInventory()) < 0)
            throw new MissingLightningRodMishap();
        return new Result(new ShockSpell(List.copyOf(distinct)), COST, List.of(), 0);
    }

    private static int lightningRodSlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.LIGHTNING_ROD)) return slot;
        }
        return -1;
    }

    private static boolean consumeLightningRod(ServerPlayer caster) {
        Inventory inventory = caster.getInventory();
        int slot = lightningRodSlot(inventory);
        if (slot < 0) return false;
        inventory.getItem(slot).shrink(1);
        inventory.setChanged();
        return true;
    }

    static float damageFor(int targets) {
        return targets < 2 ? 0 : Math.min(30, 5L + 2L * (targets - 2));
    }

    private static Vec3 center(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    private record ShockSpell(List<LivingEntity> targets) implements RenderedSpell {
        @Override public void cast(CastingEnvironment env) {
            ServerPlayer player = env.getCaster();
            if (player == null || !consumeLightningRod(player)) return;
            ServerLevel level = env.getWorld();
            List<LivingEntity> remaining = new ArrayList<>();
            for (LivingEntity target : targets) {
                if (target.isAlive() && target.level() == level) remaining.add(target);
            }
            if (remaining.size() < 2) return;

            List<LivingEntity> ordered = new ArrayList<>(remaining.size());
            LivingEntity current = remaining.remove(0);
            ordered.add(current);
            while (!remaining.isEmpty()) {
                LivingEntity from = current;
                LivingEntity next = remaining.stream().min((a, b) ->
                        Double.compare(from.distanceToSqr(a), from.distanceToSqr(b))).orElseThrow();
                remaining.remove(next);
                ordered.add(next);
                current = next;
            }

            List<Vec3> points = ordered.stream().map(ThaumicShockAction::center).toList();
            int color = env.getPigment().getColorProvider()
                    .getColor((float) level.getGameTime(), points.get(0)) & 0xFFFFFF;
            float damage = damageFor(ordered.size());
            Entity caster = env.getCastingEntity();
            for (LivingEntity target : ordered) {
                target.hurt(level.damageSources().thrown(caster, caster), damage);
            }
            ArcNetwork.send(level, points, color);
        }

        @Override public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }
    }

    private static final class MissingLightningRodMishap extends Mishap {
        @Override public FrozenPigment accentColor(CastingEnvironment env, Context context) {
            return dyeColor(DyeColor.RED);
        }

        @Override public void execute(CastingEnvironment env, Context context, List<Iota> stack) {
        }

        @Override protected Component errorMessage(CastingEnvironment env, Context context) {
            return Component.translatable("hex_ranthought.mishap.missing_lightning_rod");
        }
    }
}
