package com.lanzr.hexranthought.items;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex;
import at.petrak.hexcasting.common.items.storage.ItemFocus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public final class GloopifactItem extends ItemPackagedHex implements IotaHolderItem {
    public GloopifactItem(Properties properties) { super(properties); }

    @Override public boolean breakAfterDepletion() { return false; }
    @Override public int cooldown() { return 0; }
    @Override public boolean canProvideMedia(ItemStack stack) { return true; }
    @Override public boolean canDrawMediaFromInventory(ItemStack stack) { return true; }
    @Override public boolean writeable(ItemStack stack) { return true; }

    @Override public void clearHex(ItemStack stack) {
        if (readIotaTag(stack) == null) super.clearHex(stack);
    }

    @Override public CompoundTag readIotaTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ItemFocus.TAG_DATA, 10)
                ? tag.getCompound(ItemFocus.TAG_DATA) : null;
    }

    @Override public boolean canWrite(ItemStack stack, Iota iota) {
        return iota == null || hasHex(stack);
    }

    @Override public void writeDatum(ItemStack stack, Iota iota) {
        if (iota == null) stack.removeTagKey(ItemFocus.TAG_DATA);
        else stack.getOrCreateTag().put(ItemFocus.TAG_DATA, IotaType.serialize(iota));
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        IotaHolderItem.appendHoverText(this, stack, tooltip, flag);
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.hex_ranthought.source_hexgloop")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
