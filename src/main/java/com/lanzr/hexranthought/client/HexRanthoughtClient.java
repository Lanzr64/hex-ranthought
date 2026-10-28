package com.lanzr.hexranthought.client;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.lanzr.hexranthought.HexRanthought;
import com.lanzr.hexranthought.items.LibraryCardItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HexRanthought.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HexRanthoughtClient {
    private HexRanthoughtClient() {}
    public static final KeyMapping FIDGET_WHEEL = new KeyMapping("key.hex_ranthought.fidget_wheel",
            InputConstants.Type.KEYSYM, 86, "key.categories.hex_ranthought");

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(FIDGET_WHEEL);
    }

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(HexRanthought.GLOOPIFACT.get(),
                    new ResourceLocation("hexcasting", "overlay_layer"),
                    (stack, level, holder, seed) -> {
                        if (!HexRanthought.GLOOPIFACT.get().hasHex(stack)) return 0;
                        return HexRanthought.GLOOPIFACT.get().readIotaTag(stack) == null ? 0.5f : 1f;
                    });
            ItemProperties.register(HexRanthought.DREIDEL_FIDGET.get(),
                    new ResourceLocation(HexRanthought.ID, "fidget_index"),
                    (stack, level, holder, seed) ->
                            (HexRanthought.DREIDEL_FIDGET.get().selected(stack) - 1) / 4f);
            ItemProperties.register(HexRanthought.LIBRARY_CARD.get(),
                    new ResourceLocation(HexRanthought.ID, "dimension"),
                    (stack, level, holder, seed) -> {
                        var dim = LibraryCardItem.getDimension(stack);
                        if (dim == null) return 0;
                        if (dim.equals(Level.OVERWORLD)) return 0.01f;
                        if (dim.equals(Level.NETHER)) return 0.02f;
                        if (dim.equals(Level.END)) return 0.03f;
                        return 1f;
                    });
        });
    }

    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 1 && HexRanthought.GLOOPIFACT.get().readIotaTag(stack) != null
                        ? IotaType.getColor(HexRanthought.GLOOPIFACT.get().readIotaTag(stack)) : 0xffffff,
                HexRanthought.GLOOPIFACT.get());
        event.register((stack, layer) -> layer == 0 ? HexRanthought.DREIDEL_FIDGET.get().getColor(stack) : 0xffffff,
                HexRanthought.DREIDEL_FIDGET.get());
    }
}
