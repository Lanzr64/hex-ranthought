package com.lanzr.hexranthought.blocks;

import at.petrak.hexcasting.common.lib.HexItems;
import com.lanzr.hexranthought.HexRanthought;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Three output-only slots, one for each possible product. */
public final class AmethystGeneratorBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] OUTPUT_SLOTS = {0, 1, 2};
    private static final int[] NO_SLOTS = {};
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private LazyOptional<IItemHandler> bottomHandler = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.DOWN));

    public AmethystGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(HexRanthought.AMETHYST_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean tryProduce(Item item) {
        int slot = item == HexItems.CHARGED_AMETHYST ? 0
                : item == HexItems.AMETHYST_DUST ? 1
                : item == Items.AMETHYST_SHARD ? 2 : -1;
        if (slot < 0) return false;

        ItemStack stored = items.get(slot);
        if (stored.isEmpty()) {
            items.set(slot, new ItemStack(item, 16));
        } else if (stored.is(item) && stored.getCount() + 16 <= Math.min(getMaxStackSize(), stored.getMaxStackSize())) {
            stored.grow(16);
        } else {
            return false;
        }
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.size(); slot++) items.set(slot, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction face) {
        return face == Direction.DOWN ? OUTPUT_SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction face) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER && face == Direction.DOWN) {
            return bottomHandler.cast();
        }
        return super.getCapability(capability, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        bottomHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        bottomHandler = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.DOWN));
    }
}
