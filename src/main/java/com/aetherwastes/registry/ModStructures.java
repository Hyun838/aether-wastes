package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.world.dungeon.DungeonPiece;
import com.aetherwastes.world.dungeon.DungeonStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Подземелья: тип структуры и тип её частей. */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, AetherWastes.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, AetherWastes.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<DungeonStructure>> DUNGEON =
            STRUCTURE_TYPES.register("dungeon", () -> () -> DungeonStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> DUNGEON_ROOM =
            PIECE_TYPES.register("dungeon_room", () -> (StructurePieceType.ContextlessType) DungeonPiece::new);

    public static final TagKey<Structure> DUNGEONS = TagKey.create(Registries.STRUCTURE, AetherWastes.id("dungeons"));
    public static final ResourceKey<Structure> LOST_ARCHIVE = ResourceKey.create(Registries.STRUCTURE, AetherWastes.id("lost_archive"));
    public static final ResourceKey<Structure> ECHO_CRYPT = ResourceKey.create(Registries.STRUCTURE, AetherWastes.id("echo_crypt"));
    public static final ResourceKey<Structure> ASH_CITADEL = ResourceKey.create(Registries.STRUCTURE, AetherWastes.id("ash_citadel"));

    private ModStructures() {}
}
