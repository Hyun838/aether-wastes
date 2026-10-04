package com.aetherwastes.block;

import com.aetherwastes.magic.SpellData;
import com.aetherwastes.magic.SpellEffects;
import com.aetherwastes.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Руна-ловушка: хранит заклинание и хозяина, срабатывает на первого чужака. */
public class RuneTrapBlockEntity extends BlockEntity {
    private UUID owner;
    private SpellData spell;
    private float power = 1f;

    public RuneTrapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_TRAP.get(), pos, state);
    }

    public void arm(UUID owner, SpellData spell, float power) {
        this.owner = owner;
        this.spell = spell;
        this.power = power;
        setChanged();
    }

    public void trigger(ServerLevel level, LivingEntity victim) {
        if (owner != null && victim.getUUID().equals(owner)) return;
        if (spell == null || owner == null) {
            level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
            return;
        }
        if (!(level.getPlayerByUUID(owner) instanceof ServerPlayer caster)) return;
        if (SpellEffects.isAlly(caster, victim)) return;
        level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3,
                worldPosition.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        level.playSound(null, worldPosition, SoundEvents.EVOKER_CAST_SPELL, SoundSource.BLOCKS, 1f, 1.4f);
        SpellEffects.applyAll(SpellEffects.runeContext(caster, spell, power, victim));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("owner", owner);
        tag.putFloat("power", power);
        if (spell != null) {
            SpellData.CODEC.encodeStart(NbtOps.INSTANCE, spell).result().ifPresent(t -> tag.put("spell", t));
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        power = tag.getFloat("power");
        spell = tag.contains("spell") ? SpellData.CODEC.parse(NbtOps.INSTANCE, tag.get("spell")).result().orElse(null) : null;
    }
}
