package com.lanzr.hexranthought.network;

import com.lanzr.hexranthought.HexRanthought;
import com.lanzr.hexranthought.client.ArcClientEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Sends short-lived chain geometry to clients near each section of the arc. */
public final class ArcNetwork {
    private static final int MAX_POINTS_PER_PACKET = 256;
    private static final double VIEW_DISTANCE_SQR = 96 * 96;
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HexRanthought.ID, "arc"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    private ArcNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, ArcPacket.class, ArcPacket::encode, ArcPacket::decode, ArcPacket::handle);
        CHANNEL.registerMessage(1, BladePacket.class, BladePacket::encode, BladePacket::decode, BladePacket::handle);
    }

    public static void send(ServerLevel level, List<Vec3> points, int color) {
        for (int start = 0; start < points.size() - 1; start += MAX_POINTS_PER_PACKET - 1) {
            int end = Math.min(start + MAX_POINTS_PER_PACKET, points.size());
            List<Vec3> section = List.copyOf(points.subList(start, end));
            ArcPacket packet = new ArcPacket(section, color);
            for (ServerPlayer player : level.players()) {
                if (section.stream().anyMatch(point -> player.distanceToSqr(point) <= VIEW_DISTANCE_SQR))
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    public static void sendStraight(ServerLevel level, Vec3 start, Vec3 end, int color) {
        BladePacket packet = new BladePacket(start, end, color);
        Vec3 direction = end.subtract(start);
        double lengthSqr = direction.lengthSqr();
        for (ServerPlayer player : level.players()) {
            double progress = lengthSqr > 0
                    ? Math.max(0, Math.min(1, player.position().subtract(start).dot(direction) / lengthSqr)) : 0;
            if (player.distanceToSqr(start.add(direction.scale(progress))) <= VIEW_DISTANCE_SQR)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private record ArcPacket(List<Vec3> points, int color) {
        private void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(points.size());
            for (Vec3 point : points) {
                buf.writeDouble(point.x);
                buf.writeDouble(point.y);
                buf.writeDouble(point.z);
            }
            buf.writeInt(color);
        }

        private static ArcPacket decode(FriendlyByteBuf buf) {
            int count = buf.readVarInt();
            if (count < 2 || count > MAX_POINTS_PER_PACKET)
                throw new IllegalArgumentException("Invalid arc point count: " + count);
            List<Vec3> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
            return new ArcPacket(points, buf.readInt());
        }

        private static void handle(ArcPacket message, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ArcClientEffects.show(message.points, message.color)));
            ctx.setPacketHandled(true);
        }
    }

    private record BladePacket(Vec3 start, Vec3 end, int color) {
        private void encode(FriendlyByteBuf buf) {
            buf.writeDouble(start.x);
            buf.writeDouble(start.y);
            buf.writeDouble(start.z);
            buf.writeDouble(end.x);
            buf.writeDouble(end.y);
            buf.writeDouble(end.z);
            buf.writeInt(color);
        }

        private static BladePacket decode(FriendlyByteBuf buf) {
            Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            return new BladePacket(start, end, buf.readInt());
        }

        private static void handle(BladePacket message, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ArcClientEffects.showStraight(List.of(message.start, message.end), message.color)));
            ctx.setPacketHandled(true);
        }
    }
}
