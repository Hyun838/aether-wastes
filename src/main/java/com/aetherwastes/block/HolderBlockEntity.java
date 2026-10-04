package com.aetherwastes.block;

import com.aetherwastes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Блок с одним предметом на нём (Фокус и Постамент ритуала). */
public class HolderBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;

    public HolderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static HolderBlockEntity focus(BlockPos pos, BlockState state) {
        return new HolderBlockEntity(ModBlockEntities.RITUAL_FOCUS.get(), pos, state);
    }

    public static HolderBlockEntity pedestal(BlockPos pos, BlockState state) {
        return new HolderBlockEntity(ModBlockEntities.RITUAL_PEDESTAL.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack stack) {
        item = stack;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) tag.put("item", item.save(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains("item") ? ItemStack.parseOptional(registries, tag.getCompound("item")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
