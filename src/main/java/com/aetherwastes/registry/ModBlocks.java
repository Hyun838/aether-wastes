package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.base.Devices;
import com.aetherwastes.block.AetherScarBlock;
import com.aetherwastes.block.AnchorBlock;
import com.aetherwastes.block.DeviceBlock;
import com.aetherwastes.block.ForgeHearthBlock;
import com.aetherwastes.block.JournalArchiveBlock;
import com.aetherwastes.block.PurifyingObeliskBlock;
import com.aetherwastes.block.RitualBlock;
import com.aetherwastes.block.RuneTrapBlock;
import com.aetherwastes.block.SlateBlock;
import com.aetherwastes.block.WardPylonBlock;
import com.aetherwastes.block.WorldBlocks;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AetherWastes.MODID);

    // --- Руды и материалы ---
    public static final DeferredBlock<Block> ETHER_CRYSTAL_ORE = BLOCKS.register("ether_crystal_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).lightLevel(s -> 5)));
    public static final DeferredBlock<Block> DEEPSLATE_ETHER_CRYSTAL_ORE = BLOCKS.register("deepslate_ether_crystal_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE).lightLevel(s -> 5)));
    public static final DeferredBlock<Block> STAR_IRON_ORE = BLOCKS.register("star_iron_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).lightLevel(s -> 7)));
    public static final DeferredBlock<Block> SALT_CRUST = BLOCKS.register("salt_crust",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).strength(0.8f).mapColor(MapColor.SNOW)));
    public static final DeferredBlock<Block> ASH_BLOCK = BLOCKS.register("ash_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COARSE_DIRT).sound(SoundType.SAND).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> ETHER_GLASS = BLOCKS.register("ether_glass",
            () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(s -> 4)));
    public static final DeferredBlock<Block> GLOWCAP_BLOCK = BLOCKS.register("glowcap_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).lightLevel(s -> 13)));
    public static final DeferredBlock<Block> CHARGED_CRYSTAL = BLOCKS.register("charged_crystal",
            () -> new WorldBlocks.ChargedCrystal(BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel(s -> 10)));
    public static final DeferredBlock<Block> BLEEDING_LOG = BLOCKS.register("bleeding_log",
            () -> new WorldBlocks.BleedingLog(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.CRIMSON_STEM)));
    public static final DeferredBlock<Block> LIVING_WALL = BLOCKS.register("living_wall",
            () -> new WorldBlocks.LivingWall(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).strength(1.5f).randomTicks()));
    public static final DeferredBlock<Block> AETHER_SCAR = BLOCKS.register("aether_scar",
            () -> new AetherScarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MYCELIUM).randomTicks().lightLevel(s -> 3)
                    .mapColor(MapColor.COLOR_PURPLE).noLootTable()));

    // --- База ---
    public static final DeferredBlock<AnchorBlock> ANCHOR = BLOCKS.register("anchor",
            () -> new AnchorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE).strength(3.5f, 1200f).noOcclusion()
                    .lightLevel(s -> 6 + s.getValue(AnchorBlock.TIER) * 2).sound(SoundType.LODESTONE)));
    public static final DeferredBlock<SlateBlock> SLATE = BLOCKS.register("slate",
            () -> new SlateBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LECTERN).strength(2.5f).lightLevel(s -> 3).noOcclusion()));
    public static final DeferredBlock<ForgeHearthBlock> FORGE_HEARTH = BLOCKS.register("forge_hearth",
            () -> new ForgeHearthBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLAST_FURNACE).noOcclusion()
                    .lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 13 : 0)));
    public static final DeferredBlock<RitualBlock> RITUAL_FOCUS = BLOCKS.register("ritual_focus",
            () -> new RitualBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(s -> 7).noOcclusion(), true));
    public static final DeferredBlock<RitualBlock> RITUAL_PEDESTAL = BLOCKS.register("ritual_pedestal",
            () -> new RitualBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).noOcclusion(), false));
    public static final DeferredBlock<WardPylonBlock> WARD_PYLON = BLOCKS.register("ward_pylon",
            () -> new WardPylonBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(s -> 6).noOcclusion()));
    public static final DeferredBlock<PurifyingObeliskBlock> PURIFYING_OBELISK = BLOCKS.register("purifying_obelisk",
            () -> new PurifyingObeliskBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).lightLevel(s -> 9).noOcclusion()));
    public static final DeferredBlock<DeviceBlock> OBSERVATORY = BLOCKS.register("observatory",
            () -> new DeviceBlock(Devices.OBSERVATORY, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()));
    public static final DeferredBlock<JournalArchiveBlock> JOURNAL_ARCHIVE = BLOCKS.register("journal_archive",
            () -> new JournalArchiveBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF)));

    // --- Порталы и особые ---
    public static final DeferredBlock<Block> RIFT_PORTAL = BLOCKS.register("rift_portal",
            () -> new WorldBlocks.RiftPortal(BlockBehaviour.Properties.of().noCollission().strength(-1f, 3600000f)
                    .lightLevel(s -> 12).noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<Block> UNDERSIDE_PORTAL = BLOCKS.register("underside_portal",
            () -> new WorldBlocks.UndersidePortal(BlockBehaviour.Properties.ofFullCopy(Blocks.CRYING_OBSIDIAN).lightLevel(s -> 11)));
    public static final DeferredBlock<Block> HEART_OF_RIFT = BLOCKS.register("heart_of_rift",
            () -> new WorldBlocks.HeartOfRift(BlockBehaviour.Properties.of().strength(-1f, 3600000f).lightLevel(s -> 15).noOcclusion()
                    .noLootTable().mapColor(MapColor.COLOR_MAGENTA)));
    public static final DeferredBlock<RuneTrapBlock> RUNE_TRAP = BLOCKS.register("rune_trap",
            () -> new RuneTrapBlock(BlockBehaviour.Properties.of().noCollission().instabreak().lightLevel(s -> 5)
                    .noLootTable().noOcclusion().mapColor(MapColor.COLOR_PURPLE)));

    // --- Подземелья ---
    public static final DeferredBlock<Block> RUNE_BRICKS = BLOCKS.register("rune_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).lightLevel(s -> 3).mapColor(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<Block> CRACKED_RUNE_BRICKS = BLOCKS.register("cracked_rune_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CRACKED_DEEPSLATE_BRICKS).lightLevel(s -> 2).mapColor(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<net.minecraft.world.level.block.RotatedPillarBlock> RUNE_PILLAR = BLOCKS.register("rune_pillar",
            () -> new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).lightLevel(s -> 4)));
    public static final DeferredBlock<Block> PHANTOM_LANTERN = BLOCKS.register("phantom_lantern",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).lightLevel(s -> 14).mapColor(MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<Block> PHANTOM_GLASS = BLOCKS.register("phantom_glass",
            () -> new TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(s -> 6)));
    public static final DeferredBlock<Block> ASH_BRICKS = BLOCKS.register("ash_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BRICKS).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<Block> EMBER_BRICKS = BLOCKS.register("ember_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE_BRICKS).lightLevel(s -> 8).mapColor(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<Block> ARCHIVE_SHELF = BLOCKS.register("archive_shelf",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).lightLevel(s -> 3).mapColor(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<com.aetherwastes.block.DungeonTrapBlock> DUNGEON_TRAP = BLOCKS.register("dungeon_trap",
            () -> new com.aetherwastes.block.DungeonTrapBlock(BlockBehaviour.Properties.of().noCollission().strength(50f, 1200f)
                    .lightLevel(s -> s.getValue(com.aetherwastes.block.DungeonTrapBlock.ARMED) ? 3 : 0).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK).mapColor(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<com.aetherwastes.block.GuardianSealBlock> GUARDIAN_SEAL = BLOCKS.register("guardian_seal",
            () -> new com.aetherwastes.block.GuardianSealBlock(BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable()
                    .lightLevel(s -> s.getValue(com.aetherwastes.block.GuardianSealBlock.ACTIVE) ? 13 : 4)
                    .pushReaction(PushReaction.BLOCK).mapColor(MapColor.COLOR_PURPLE).sound(SoundType.LODESTONE)));

    private ModBlocks() {}
}
