package com.lanzr.hexranthought.client;

import com.lanzr.hexranthought.HexRanthought;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/** Short-lived, additive, two-layer arc adapted from Thaumcraft's FXBolt. */
public final class ArcClientEffects {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HexRanthought.ID, "textures/effects/arc_essentia.png");
    private static final ResourceLocation BLADE_TEXTURE =
            new ResourceLocation(HexRanthought.ID, "textures/effects/spatial_blade_line.png");

    private ArcClientEffects() {}

    public static void show(List<Vec3> points, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && points.size() > 1)
            mc.particleEngine.add(new ArcParticle(mc.level, points, color, false));
    }

    public static void showStraight(List<Vec3> points, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && points.size() == 2)
            mc.particleEngine.add(new ArcParticle(mc.level, points, color, true));
    }

    private static final class ArcParticle extends Particle {
        private final List<Vec3> anchors;
        private final long seed;
        private final boolean straight;

        private ArcParticle(ClientLevel level, List<Vec3> points, int color, boolean straight) {
            super(level, points.get(0).x, points.get(0).y, points.get(0).z);
            this.anchors = List.copyOf(points);
            this.seed = random.nextLong();
            this.straight = straight;
            this.rCol = ((color >> 16) & 255) / 255.0f;
            this.gCol = ((color >> 8) & 255) / 255.0f;
            this.bCol = (color & 255) / 255.0f;
            this.lifetime = 3;
            this.hasPhysics = false;
        }

        @Override public ParticleRenderType getRenderType() {
            return ParticleRenderType.CUSTOM;
        }

        @Override public boolean shouldCull() {
            return false;
        }

        @Override public void render(VertexConsumer ignored, Camera camera, float partialTick) {
            float opacity = Math.max(0.05f, 1.0f - (age + partialTick) / lifetime);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0, straight ? BLADE_TEXTURE : TEXTURE);

            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buffer = tessellator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            Vec3 cameraPos = camera.getPosition();
            for (int segment = 0; segment < anchors.size() - 1; segment++)
                if (straight)
                    renderStraightSegment(buffer, cameraPos, anchors.get(segment), anchors.get(segment + 1), opacity);
                else
                    renderSegment(buffer, cameraPos, anchors.get(segment), anchors.get(segment + 1), segment, opacity);
            tessellator.end();

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        private void renderStraightSegment(BufferBuilder buffer, Vec3 camera, Vec3 start, Vec3 end,
                                           float opacity) {
            double length = start.distanceTo(end);
            if (length < 0.001) return;
            int steps = Math.max(1, (int) Math.ceil(length));
            for (int i = 0; i < steps; i++) {
                Vec3 from = start.lerp(end, (double) i / steps);
                Vec3 to = start.lerp(end, (double) (i + 1) / steps);
                drawRibbon(buffer, camera, from, to, 0.055, opacity * 0.7f);
                drawRibbon(buffer, camera, from, to, 0.018, opacity);
            }
        }

        private void renderSegment(BufferBuilder buffer, Vec3 camera, Vec3 start, Vec3 end,
                                   int segment, float opacity) {
            double length = start.distanceTo(end);
            if (length < 0.001) return;
            int steps = Math.max(2, Math.min(160, (int) Math.ceil(length * Math.PI)));
            Random jitter = new Random(seed + segment * 1000003L + age * 8191L);
            double phase = jitter.nextDouble() * 50.0;
            double amplitude = 0.10 + (age * 0.03);
            Vec3 previous = start;
            for (int i = 1; i <= steps; i++) {
                double progress = (double) i / steps;
                Vec3 next = start.lerp(end, progress);
                if (i < steps) {
                    double t = progress * length * Math.PI + phase;
                    next = next.add(Math.sin(t / 4.0) * amplitude + (jitter.nextDouble() - 0.5) * 0.1,
                            Math.sin(t / 3.0) * amplitude + (jitter.nextDouble() - 0.5) * 0.1,
                            Math.sin(t / 2.0) * amplitude + (jitter.nextDouble() - 0.5) * 0.1);
                }
                drawRibbon(buffer, camera, previous, next, 0.066, opacity * 0.8f);
                drawRibbon(buffer, camera, previous, next, 0.022, opacity);
                previous = next;
            }
        }

        private void drawRibbon(BufferBuilder buffer, Vec3 camera, Vec3 from, Vec3 to,
                                double halfWidth, float opacity) {
            Vec3 direction = to.subtract(from);
            Vec3 view = camera.subtract(from.add(to).scale(0.5));
            Vec3 side = direction.cross(view);
            if (side.lengthSqr() < 1.0e-8) side = direction.cross(new Vec3(0, 1, 0));
            if (side.lengthSqr() < 1.0e-8) side = direction.cross(new Vec3(1, 0, 0));
            side = side.normalize().scale(halfWidth);

            vertex(buffer, from.add(side).subtract(camera), 0, 0, opacity);
            vertex(buffer, from.subtract(side).subtract(camera), 0, 1, opacity);
            vertex(buffer, to.subtract(side).subtract(camera), 1, 1, opacity);
            vertex(buffer, to.add(side).subtract(camera), 1, 0, opacity);
        }

        private void vertex(BufferBuilder buffer, Vec3 pos, float u, float v, float opacity) {
            buffer.vertex(pos.x, pos.y, pos.z).color(rCol, gCol, bCol, opacity)
                    .uv(u, v).endVertex();
        }
    }
}
