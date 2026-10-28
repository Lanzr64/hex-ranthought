package com.lanzr.hexranthought.casting;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem;
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.lanzr.hexranthought.items.GloopifactItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Transfers the active Hex's ravenmind between a Gloopifact and staff casting. */
public final class GloopimindAction implements Action {
    private final boolean toStaff;
    public GloopimindAction(boolean toStaff) { this.toStaff = toStaff; }

    @Override public OperationResult operate(CastingEnvironment env, CastingImage image,
                                              SpellContinuation continuation) {
        ServerPlayer caster = env.getCaster();
        if (caster == null || env.getCastingHand() == null)
            throw MishapBadOffhandItem.of(ItemStack.EMPTY, "gloopifact");
        ItemStack stack = caster.getItemInHand(env.getCastingHand());
        if (!(stack.getItem() instanceof GloopifactItem))
            throw MishapBadOffhandItem.of(stack, "gloopifact");

        CastingVM staffVM = IXplatAbstractions.INSTANCE.getStaffcastVM(caster, env.getOtherHand());
        if (staffVM == null) throw MishapBadOffhandItem.of(stack, "gloopifact");
        CastingImage staffImage = staffVM.getImage();
        CastingImage source = toStaff ? image : staffImage;
        CastingImage destination = toStaff ? staffImage : image;
        CompoundTag sourceData = source.getUserData();
        CompoundTag newData = destination.getUserData().copy();
        if (sourceData.contains(HexAPI.RAVENMIND_USERDATA, 10)) {
            CompoundTag datumTag = sourceData.getCompound(HexAPI.RAVENMIND_USERDATA);
            Iota datum = IotaType.deserialize(datumTag, env.getWorld());
            Player trueName = datum == null ? null : MishapOthersName.getTrueNameFromDatum(datum, caster);
            if (trueName != null && trueName != caster) throw new MishapOthersName(trueName);
            newData.put(HexAPI.RAVENMIND_USERDATA, datumTag.copy());
        } else {
            newData.remove(HexAPI.RAVENMIND_USERDATA);
        }
        CastingImage changed = destination.copy(destination.getStack(), destination.getParenCount(),
                destination.getParenthesized(), destination.getEscapeNext(),
                destination.getOpsConsumed(), newData);
        if (toStaff) {
            IXplatAbstractions.INSTANCE.setStaffcastImage(caster, changed);
            return new OperationResult(image.withUsedOp(), List.of(), continuation, HexEvalSounds.NORMAL_EXECUTE);
        }
        return new OperationResult(changed.withUsedOp(), List.of(), continuation, HexEvalSounds.NORMAL_EXECUTE);
    }
}
