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
import at.petrak.hexcasting.api.misc.MediaConstants;
import com.lanzr.hexranthought.HexRanthought;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Kills a phantom and adds one raw chicken to its normal death drops. */
@Mod.EventBusSubscriber(modid = HexRanthought.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KillPhantomAction implements SpellAction {
    private static final long COST = 2 * MediaConstants.SHARD_UNIT;
    private static final ThreadLocal<Phantom> ACTIVE_KILL = new ThreadLocal<>();

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
        Entity entity = OperatorUtils.getEntity(args, 0, getArgc());
        if (!(entity instanceof Phantom phantom))
            return new Result(new KillPhantomSpell(null), COST, List.of(), 0);

        env.assertEntityInRange(phantom);
        return new Result(new KillPhantomSpell(phantom), COST,
                List.of(ParticleSpray.cloud(phantom.position(), 2, 10)), 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addChickenDrop(LivingDropsEvent event) {
        Phantom phantom = ACTIVE_KILL.get();
        if (phantom == null || event.getEntity() != phantom) return;
        if (!phantom.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;

        event.getDrops().add(new ItemEntity(phantom.level(), phantom.getX(), phantom.getY(), phantom.getZ(),
                new ItemStack(Items.CHICKEN)));
    }

    private record KillPhantomSpell(Phantom phantom) implements RenderedSpell {
        @Override public void cast(CastingEnvironment env) {
            if (phantom == null || !phantom.isAlive() || phantom.level() != env.getWorld()) return;

            ACTIVE_KILL.set(phantom);
            try {
                phantom.kill();
            } finally {
                ACTIVE_KILL.remove();
            }
        }

        @Override public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }
    }
}
