package com.lanzr.hexranthought.items;

import at.petrak.hexcasting.common.items.storage.ItemSpellbook;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

/** A small selection wheel whose active slot can be read by Mind's Orchard. */
public class FidgetItem extends Item {
    public static final String ORCHARD_TAG = "hex_ranthought:orchard";
    private final int slots;

    public FidgetItem(Properties properties, int slots) {
        super(properties);
        this.slots = slots;
    }

    public int slots() { return slots; }

    public int selected(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? 1 : Mth.clamp(tag.getInt(ItemSpellbook.TAG_SELECTED_PAGE), 1, slots);
    }

    public int select(ItemStack stack, int index) {
        int selected = Math.floorMod(index - 1, slots) + 1;
        stack.getOrCreateTag().putInt(ItemSpellbook.TAG_SELECTED_PAGE, selected);
        return selected;
    }

    public static void setOrchard(ServerPlayer player, double value) {
        player.getPersistentData().putDouble(ORCHARD_TAG, value);
    }

    public static double getOrchard(ServerPlayer player) {
        return player.getPersistentData().getDouble(ORCHARD_TAG);
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int next = select(stack, selected(stack) + (player.isShiftKeyDown() ? -1 : 1));
        if (player instanceof ServerPlayer serverPlayer) {
            setOrchard(serverPlayer, next);
            serverPlayer.displayClientMessage(Component.translatable("item.hex_ranthought.fidget.selection", next, slots), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hex_ranthought.fidget.selection", selected(stack), slots)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.hex_ranthought.fidget.hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.hex_ranthought.source_hexgloop")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
