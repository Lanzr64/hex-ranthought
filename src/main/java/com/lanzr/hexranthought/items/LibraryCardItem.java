package com.lanzr.hexranthought.items;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.common.blocks.akashic.BlockAkashicRecord;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class LibraryCardItem extends Item implements IotaHolderItem {
    public static final String TAG_DIMENSION = "bound_dimension";

    public LibraryCardItem(Properties properties) { super(properties); }

    public static ResourceKey<Level> getDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DIMENSION, 8)) return null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_DIMENSION));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof BlockAkashicRecord))
            return super.useOn(context);
        if (!level.isClientSide) context.getItemInHand().getOrCreateTag()
                .putString(TAG_DIMENSION, level.dimension().location().toString());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public CompoundTag readIotaTag(ItemStack stack) { return null; }
    @Override public boolean writeable(ItemStack stack) { return true; }
    @Override public boolean canWrite(ItemStack stack, Iota iota) { return iota == null; }
    @Override public void writeDatum(ItemStack stack, Iota iota) {
        if (iota == null) stack.removeTagKey(TAG_DIMENSION);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceKey<Level> dimension = getDimension(stack);
        if (dimension != null) tooltip.add(Component.translatable(
                "item.hex_ranthought.library_card.tooltip_dimension", dimension.location().toString()));
        tooltip.add(Component.translatable("tooltip.hex_ranthought.source_hexgloop")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
