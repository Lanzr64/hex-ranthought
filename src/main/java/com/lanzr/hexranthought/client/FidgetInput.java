package com.lanzr.hexranthought.client;

import com.lanzr.hexranthought.HexRanthought;
import com.lanzr.hexranthought.items.FidgetItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import com.lanzr.hexranthought.network.FidgetNetwork;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HexRanthought.ID, value = Dist.CLIENT)
public final class FidgetInput {
    private FidgetInput() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !HexRanthoughtClient.FIDGET_WHEEL.consumeClick()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) return;
        ItemStack main = client.player.getMainHandItem();
        if (main.getItem() instanceof FidgetItem fidget) {
            client.setScreen(new FidgetWheelScreen(InteractionHand.MAIN_HAND, main, fidget));
            return;
        }
        ItemStack off = client.player.getOffhandItem();
        if (off.getItem() instanceof FidgetItem fidget)
            client.setScreen(new FidgetWheelScreen(InteractionHand.OFF_HAND, off, fidget));
    }

    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!Screen.hasShiftDown() || client.player == null || client.screen != null) return;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack stack = client.player.getMainHandItem();
        if (!(stack.getItem() instanceof FidgetItem)) {
            hand = InteractionHand.OFF_HAND;
            stack = client.player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof FidgetItem fidget)) return;
        int next = fidget.select(stack, fidget.selected(stack) + (event.getScrollDelta() < 0 ? 1 : -1));
        FidgetNetwork.select(hand, next);
        event.setCanceled(true);
    }
}
