package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.block.ForgeHearthBlockEntity;
import com.aetherwastes.block.HolderBlockEntity;
import com.aetherwastes.block.JournalArchiveBlock;
import com.aetherwastes.block.PurifyingObeliskBlock;
import com.aetherwastes.block.RuneTrapBlockEntity;
import com.aetherwastes.block.SlateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("DataFlowIssue")
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AetherWastes.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlateBlockEntity>> SLATE =
            BLOCK_ENTITIES.register("slate", () -> BlockEntityType.Builder.of(SlateBlockEntity::new, ModBlocks.SLATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeHearthBlockEntity>> FORGE_HEARTH =
            BLOCK_ENTITIES.register("forge_hearth", () -> BlockEntityType.Builder.of(ForgeHearthBlockEntity::new, ModBlocks.FORGE_HEARTH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HolderBlockEntity>> RITUAL_FOCUS =
            BLOCK_ENTITIES.register("ritual_focus", () -> BlockEntityType.Builder.of(HolderBlockEntity::focus, ModBlocks.RITUAL_FOCUS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HolderBlockEntity>> RITUAL_PEDESTAL =
            BLOCK_ENTITIES.register("ritual_pedestal", () -> BlockEntityType.Builder.of(HolderBlockEntity::pedestal, ModBlocks.RITUAL_PEDESTAL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PurifyingObeliskBlock.Entity>> PURIFYING_OBELISK =
            BLOCK_ENTITIES.register("purifying_obelisk", () -> BlockEntityType.Builder.of(PurifyingObeliskBlock.Entity::new, ModBlocks.PURIFYING_OBELISK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JournalArchiveBlock.Entity>> JOURNAL_ARCHIVE =
            BLOCK_ENTITIES.register("journal_archive", () -> BlockEntityType.Builder.of(JournalArchiveBlock.Entity::new, ModBlocks.JOURNAL_ARCHIVE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RuneTrapBlockEntity>> RUNE_TRAP =
            BLOCK_ENTITIES.register("rune_trap", () -> BlockEntityType.Builder.of(RuneTrapBlockEntity::new, ModBlocks.RUNE_TRAP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.aetherwastes.block.GuardianSealBlockEntity>> GUARDIAN_SEAL =
            BLOCK_ENTITIES.register("guardian_seal", () -> BlockEntityType.Builder.of(com.aetherwastes.block.GuardianSealBlockEntity::new, ModBlocks.GUARDIAN_SEAL.get()).build(null));

    private ModBlockEntities() {}
}
