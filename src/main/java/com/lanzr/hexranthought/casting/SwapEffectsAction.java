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
import at.petrak.hexcasting.api.misc.MediaConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Exchanges the active status effects of two living entity iotas. */
public final class SwapEffectsAction implements SpellAction {
    private static final long COST = 4 * MediaConstants.SHARD_UNIT;

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
        LivingEntity first = OperatorUtils.getLivingEntityButNotArmorStand(args, 0, getArgc());
        LivingEntity second = OperatorUtils.getLivingEntityButNotArmorStand(args, 1, getArgc());
        if (first == second)
            throw new MishapBadEntity(second, Component.translatable("hex_ranthought.mishap.distinct_entities"));
        env.assertEntityInRange(first);
        env.assertEntityInRange(second);
        assertCanExchange(first, second);

        return new Result(new SwapEffectsSpell(first, second), COST,
                List.of(ParticleSpray.cloud(first.position(), 2, 10),
                        ParticleSpray.cloud(second.position(), 2, 10)), 0);
    }

    private static void assertCanExchange(LivingEntity first, LivingEntity second) {
        for (MobEffectInstance effect : first.getActiveEffects()) {
            if (!second.canBeAffected(effect))
                throw new MishapBadEntity(second, Component.translatable("hex_ranthought.mishap.effect_immune"));
        }
        for (MobEffectInstance effect : second.getActiveEffects()) {
            if (!first.canBeAffected(effect))
                throw new MishapBadEntity(first, Component.translatable("hex_ranthought.mishap.effect_immune"));
        }
    }

    private record SwapEffectsSpell(LivingEntity first, LivingEntity second) implements RenderedSpell {
        @Override public void cast(CastingEnvironment env) {
            if (!first.isAlive() || !second.isAlive() || first == second
                    || first.level() != env.getWorld() || second.level() != env.getWorld()) return;

            for (MobEffectInstance effect : first.getActiveEffects())
                if (!second.canBeAffected(effect)) return;
            for (MobEffectInstance effect : second.getActiveEffects())
                if (!first.canBeAffected(effect)) return;

            List<MobEffectInstance> firstEffects = new ArrayList<>(first.getActiveEffects());
            List<MobEffectInstance> secondEffects = new ArrayList<>(second.getActiveEffects());
            first.removeAllEffects();
            second.removeAllEffects();
            for (MobEffectInstance effect : secondEffects) first.forceAddEffect(effect, second);
            for (MobEffectInstance effect : firstEffects) second.forceAddEffect(effect, first);
        }

        @Override public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }
    }
}
