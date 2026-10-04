package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.item.ElixirItem;
import com.aetherwastes.item.EtherCompassItem;
import com.aetherwastes.item.GlyphItem;
import com.aetherwastes.item.JournalItem;
import com.aetherwastes.item.SpecialItem;
import com.aetherwastes.item.StaffItem;
import com.aetherwastes.item.TreatmentItem;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.survival.Traumas;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AetherWastes.MODID);

    // --- Материалы ---
    public static final DeferredItem<Item> ETHER_SHARD = simple("ether_shard");
    public static final DeferredItem<Item> GLYPH_BLANK = simple("glyph_blank");
    public static final DeferredItem<Item> ANCHOR_CORE = ITEMS.register("anchor_core", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SALT = simple("salt");
    public static final DeferredItem<Item> RESIN = simple("resin");
    public static final DeferredItem<Item> CHARGED_SHARD = simple("charged_shard");
    public static final DeferredItem<Item> STAR_IRON_INGOT = ITEMS.register("star_iron_ingot", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SOUL_ESSENCE = ITEMS.register("soul_essence", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> WANDERER_SHARD = ITEMS.register("wanderer_shard",
            () -> new SpecialItem(SpecialItem.Kind.PLAIN, "tooltip.aetherwastes.wanderer_shard", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> HUNTER_TROPHY = ITEMS.register("hunter_trophy",
            () -> new SpecialItem(SpecialItem.Kind.PLAIN, "tooltip.aetherwastes.hunter_trophy", new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> NEMESIS_TROPHY = ITEMS.register("nemesis_trophy",
            () -> new SpecialItem(SpecialItem.Kind.PLAIN, "tooltip.aetherwastes.nemesis_trophy", new Item.Properties().rarity(Rarity.EPIC)));

    // --- Инструменты и снаряжение ---
    public static final DeferredItem<EtherCompassItem> ETHER_COMPASS = ITEMS.register("ether_compass",
            () -> new EtherCompassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<StaffItem> ETHER_STAFF = ITEMS.register("ether_staff",
            () -> new StaffItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<JournalItem> JOURNAL = ITEMS.register("journal",
            () -> new JournalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SPLINT = ITEMS.register("splint", () -> new TreatmentItem(Traumas.FRACTURE, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BANDAGE = ITEMS.register("bandage", () -> new TreatmentItem(Traumas.BLEEDING, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> BURN_SALVE = ITEMS.register("burn_salve", () -> new TreatmentItem(Traumas.BURN, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ElixirItem> ELIXIR = ITEMS.register("elixir", () -> new ElixirItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> HEARTY_STEW = ITEMS.register("hearty_stew", () -> new Item(new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f).usingConvertsTo(Items.BOWL).build())));
    public static final DeferredItem<Item> UNDERSIDE_KEY = ITEMS.register("underside_key",
            () -> new SpecialItem(SpecialItem.Kind.UNDERSIDE_KEY, "tooltip.aetherwastes.underside_key", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SECOND_AGE_SEAL = ITEMS.register("second_age_seal",
            () -> new SpecialItem(SpecialItem.Kind.SECOND_AGE_SEAL, "tooltip.aetherwastes.second_age_seal", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> NEMESIS_ECHO = ITEMS.register("nemesis_echo",
            () -> new SpecialItem(SpecialItem.Kind.NEMESIS_ECHO, "tooltip.aetherwastes.nemesis_echo", new Item.Properties().rarity(Rarity.RARE)));

    // --- Глифы ---
    public static final Map<Glyph, DeferredItem<GlyphItem>> GLYPHS = new EnumMap<>(Glyph.class);

    static {
        for (Glyph glyph : Glyph.values()) {
            GLYPHS.put(glyph, ITEMS.register("glyph_" + glyph.id(), () -> new GlyphItem(glyph, new Item.Properties().stacksTo(16))));
        }
    }

    // --- Яйца призыва ---
    public static final DeferredItem<Item> SALT_WRAITH_EGG = ITEMS.register("salt_wraith_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SALT_WRAITH, 0xE8E4D8, 0x9FB4C7, new Item.Properties()));
    public static final DeferredItem<Item> RESIN_WALKER_EGG = ITEMS.register("resin_walker_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RESIN_WALKER, 0x6B1E1E, 0xC2702A, new Item.Properties()));
    public static final DeferredItem<Item> GLASSMAN_EGG = ITEMS.register("glassman_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GLASSMAN, 0xBFE8F0, 0x9B5DE5, new Item.Properties()));

    // --- Блоки ---
    public static final DeferredItem<BlockItem> ETHER_CRYSTAL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.ETHER_CRYSTAL_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_ETHER_CRYSTAL_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_ETHER_CRYSTAL_ORE);
    public static final DeferredItem<BlockItem> STAR_IRON_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.STAR_IRON_ORE);
    public static final DeferredItem<BlockItem> SALT_CRUST = ITEMS.registerSimpleBlockItem(ModBlocks.SALT_CRUST);
    public static final DeferredItem<BlockItem> ASH_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.ASH_BLOCK);
    public static final DeferredItem<BlockItem> ETHER_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.ETHER_GLASS);
    public static final DeferredItem<BlockItem> GLOWCAP_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GLOWCAP_BLOCK);
    public static final DeferredItem<BlockItem> CHARGED_CRYSTAL = ITEMS.registerSimpleBlockItem(ModBlocks.CHARGED_CRYSTAL);
    public static final DeferredItem<BlockItem> BLEEDING_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.BLEEDING_LOG);
    public static final DeferredItem<BlockItem> LIVING_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.LIVING_WALL);
    public static final DeferredItem<BlockItem> AETHER_SCAR = ITEMS.registerSimpleBlockItem(ModBlocks.AETHER_SCAR);
    public static final DeferredItem<BlockItem> ANCHOR = ITEMS.registerSimpleBlockItem(ModBlocks.ANCHOR);
    public static final DeferredItem<BlockItem> SLATE = ITEMS.registerSimpleBlockItem(ModBlocks.SLATE);
    public static final DeferredItem<BlockItem> FORGE_HEARTH = ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_HEARTH);
    public static final DeferredItem<BlockItem> RITUAL_FOCUS = ITEMS.registerSimpleBlockItem(ModBlocks.RITUAL_FOCUS);
    public static final DeferredItem<BlockItem> RITUAL_PEDESTAL = ITEMS.registerSimpleBlockItem(ModBlocks.RITUAL_PEDESTAL);
    public static final DeferredItem<BlockItem> WARD_PYLON = ITEMS.registerSimpleBlockItem(ModBlocks.WARD_PYLON);
    public static final DeferredItem<BlockItem> PURIFYING_OBELISK = ITEMS.registerSimpleBlockItem(ModBlocks.PURIFYING_OBELISK);
    public static final DeferredItem<BlockItem> OBSERVATORY = ITEMS.registerSimpleBlockItem(ModBlocks.OBSERVATORY);
    public static final DeferredItem<BlockItem> JOURNAL_ARCHIVE = ITEMS.registerSimpleBlockItem(ModBlocks.JOURNAL_ARCHIVE);
    public static final DeferredItem<BlockItem> UNDERSIDE_PORTAL = ITEMS.registerSimpleBlockItem(ModBlocks.UNDERSIDE_PORTAL);

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public static Item glyph(Glyph glyph) {
        return GLYPHS.get(glyph).get();
    }

    private ModItems() {}
}
