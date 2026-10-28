package com.lanzr.hexranthought.client;

import com.lanzr.hexranthought.items.FidgetItem;
import com.lanzr.hexranthought.network.FidgetNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Minimal 1.20 replacement for the old WNBOI context wheel. */
public final class FidgetWheelScreen extends Screen {
    private final InteractionHand hand;
    private final ItemStack stack;
    private final FidgetItem item;
    private int mouseX;
    private int mouseY;

    public FidgetWheelScreen(InteractionHand hand, ItemStack stack, FidgetItem item) {
        super(Component.translatable("screen.hex_ranthought.fidget"));
        this.hand = hand;
        this.stack = stack;
        this.item = item;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        renderBackground(graphics);
        int cx = width / 2;
        int cy = height / 2;
        int radius = Math.min(width, height) / 4;
        int hovered = hoveredSlot(mouseX, mouseY);
        graphics.drawCenteredString(font, title, cx, cy - 10, 0xffffff);
        graphics.drawCenteredString(font, Component.literal(item.selected(stack) + " / " + item.slots()),
                cx, cy + 6, 0xaaaaaa);
        for (int slot = 1; slot <= item.slots(); slot++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * (slot - 1) / item.slots();
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            int color = slot == hovered ? 0xffb88cff
                    : slot == item.selected(stack) ? 0xff8050bb : 0xff3a3048;
            graphics.fill(x - 15, y - 15, x + 15, y + 15, color);
            graphics.drawCenteredString(font, Integer.toString(slot), x, y - 4, 0xffffff);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int hoveredSlot(int x, int y) {
        int radius = Math.min(width, height) / 4;
        for (int slot = 1; slot <= item.slots(); slot++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * (slot - 1) / item.slots();
            double sx = width / 2.0 + Math.cos(angle) * radius;
            double sy = height / 2.0 + Math.sin(angle) * radius;
            if (Math.hypot(x - sx, y - sy) <= 20) return slot;
        }
        return -1;
    }

    private void choose(int slot) {
        if (slot > 0) {
            item.select(stack, slot);
            FidgetNetwork.select(hand, slot);
        }
        onClose();
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            choose(hoveredSlot((int) x, (int) y));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == HexRanthoughtClient.FIDGET_WHEEL.getKey().getValue()) {
            choose(hoveredSlot(mouseX, mouseY));
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override public boolean isPauseScreen() { return false; }
}
