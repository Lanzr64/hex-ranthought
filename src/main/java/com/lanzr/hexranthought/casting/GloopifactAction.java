package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem;
import com.lanzr.hexranthought.items.GloopifactItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class GloopifactAction implements ConstMediaAction {
    private final boolean write;
    private final boolean check;

    public GloopifactAction(boolean write, boolean check) {
        this.write = write;
        this.check = check;
    }

    @Override public int getArgc() { return write ? 1 : 0; }
    @Override public long getMediaCost() { return 0; }

    @Override public ConstMediaAction.CostMediaActionResult executeWithOpCount(List<? extends Iota> args, CastingEnvironment env) {
        return ConstMediaAction.DefaultImpls.executeWithOpCount(this, args, env);
    }

    @Override public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) {
        return ConstMediaAction.DefaultImpls.operate(this, env, image, continuation);
    }

    @Override public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
        if (env.getCaster() == null || env.getCastingHand() == null)
            throw MishapBadOffhandItem.of(ItemStack.EMPTY, "gloopifact");
        ItemStack stack = env.getCaster().getItemInHand(env.getCastingHand());
        if (!(stack.getItem() instanceof GloopifactItem item))
            throw new MishapBadOffhandItem(stack,
                    Component.translatable("item.hex_ranthought.gloopifact"));

        if (write) {
            Iota datum = args.get(0);
            boolean canWrite = item.canWrite(stack, datum);
            if (check) return List.of(new BooleanIota(canWrite));
            if (!canWrite) throw MishapBadOffhandItem.of(stack, "iota.write");
            item.writeDatum(stack, datum);
            return List.of();
        }

        Iota datum = item.readIota(stack, env.getWorld());
        if (check) return List.of(new BooleanIota(datum != null));
        if (datum == null) throw MishapBadOffhandItem.of(stack, "iota.read");
        return List.of(datum);
    }
}
