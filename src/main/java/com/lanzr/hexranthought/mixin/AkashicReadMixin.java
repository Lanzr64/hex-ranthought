package com.lanzr.hexranthought.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.common.casting.actions.akashic.OpAkashicRead;
import com.lanzr.hexranthought.items.LibraryCardItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = OpAkashicRead.class, remap = false)
public abstract class AkashicReadMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE",
            target = "Lat/petrak/hexcasting/api/casting/eval/CastingEnvironment;getWorld()Lnet/minecraft/server/level/ServerLevel;"),
            remap = false)
    private ServerLevel hex_ranthought$cardDimension(CastingEnvironment env) {
        ServerLevel current = env.getWorld();
        if (env.getCaster() == null) return current;
        // Preserve the original inventory-order priority when several cards are present.
        var inventory = env.getCaster().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof LibraryCardItem) {
                ResourceKey<Level> dimension = LibraryCardItem.getDimension(stack);
                if (dimension != null) {
                    ServerLevel other = current.getServer().getLevel(dimension);
                    if (other != null) return other;
                }
            }
        }
        return current;
    }
}
