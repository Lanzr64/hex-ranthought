package com.lanzr.hexranthought.network;

import com.lanzr.hexranthought.HexRanthought;
import com.lanzr.hexranthought.items.FidgetItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class FidgetNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HexRanthought.ID, "main"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    private FidgetNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, SelectSlot.class, SelectSlot::encode,
                SelectSlot::decode, SelectSlot::handle);
    }

    public static void select(InteractionHand hand, int slot) {
        CHANNEL.sendToServer(new SelectSlot(hand == InteractionHand.MAIN_HAND, slot));
    }

    private record SelectSlot(boolean mainHand, int slot) {
        private void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(mainHand);
            buf.writeVarInt(slot);
        }

        private static SelectSlot decode(FriendlyByteBuf buf) {
            return new SelectSlot(buf.readBoolean(), buf.readVarInt());
        }

        private static void handle(SelectSlot message, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                InteractionHand hand = message.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof FidgetItem fidget) {
                    int slot = fidget.select(stack, message.slot);
                    FidgetItem.setOrchard(player, slot);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
