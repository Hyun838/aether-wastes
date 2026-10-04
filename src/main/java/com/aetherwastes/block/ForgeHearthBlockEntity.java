package com.aetherwastes.block;

import com.aetherwastes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Горн: топливо, жар и заготовка внутри. */
public class ForgeHearthBlockEntity extends BlockEntity {
    public float heat = 0f;
    public int fuel = 0;
    public ItemStack work = ItemStack.EMPTY;

    public ForgeHearthBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGE_HEARTH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeHearthBlockEntity be) {
        boolean changed = false;
        if (be.fuel > 0) {
            be.fuel--;
            if (be.heat < 55f) be.heat += 0.12f;
            changed = true;
        }
        if (be.heat > 0f) {
            // Выше 55 жар без мехов спадает.
            be.heat -= be.heat > 55f ? 0.08f : (be.fuel > 0 ? 0f : 0.15f);
            if (be.heat < 0f) be.heat = 0f;
            changed = true;
        }
        boolean lit = be.fuel > 0 || be.heat > 20f;
        if (state.getValue(ForgeHearthBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(ForgeHearthBlock.LIT, lit), 3);
        }
        if (changed && level.getGameTime() % 20 == 0) be.setChanged();
        if (lit && level instanceof ServerLevel server && level.random.nextInt(8) == 0) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, 1, 0.2, 0, 0.2, 0);
        }
    }

    public void pump() {
        heat = Math.min(100f, heat + (fuel > 0 ? 8f : 3f));
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("heat", heat);
        tag.putInt("fuel", fuel);
        if (!work.isEmpty()) tag.put("work", work.save(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getFloat("heat");
        fuel = tag.getInt("fuel");
        work = tag.contains("work") ? ItemStack.parseOptional(registries, tag.getCompound("work")) : ItemStack.EMPTY;
    }
}
