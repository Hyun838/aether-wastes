package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.item.WastesWeapon;
import com.aetherwastes.item.WastesWeapon.Ability;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** Снаряжение 1.1: материалы брони, уровни инструментов, броня, инструменты, оружие. */
public final class ModGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AetherWastes.MODID);

    // ---------------- Уровни инструментов ----------------
    public static final Tier ETHER_STEEL_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 620, 7.0f, 2.5f, 18,
            () -> Ingredient.of(ModItems.ETHER_STEEL_INGOT.get()));
    public static final Tier STAR_IRON_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2200, 9.5f, 4.5f, 16,
            () -> Ingredient.of(ModItems.STAR_IRON_INGOT.get()));
    public static final Tier RELIC_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2600, 9.0f, 5.0f, 22,
            () -> Ingredient.of(ModItems.WANDERER_SHARD.get()));

    // ---------------- Материалы брони ----------------
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ASHEN = armor("ashen", new int[]{1, 3, 4, 2}, 15, 0f, 0f,
            () -> Ingredient.of(ModItems.ASH_PELT.get()), SoundEvents.ARMOR_EQUIP_LEATHER);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ETHER_STEEL = armor("ether_steel", new int[]{2, 5, 6, 2}, 16, 1f, 0f,
            () -> Ingredient.of(ModItems.ETHER_STEEL_INGOT.get()), SoundEvents.ARMOR_EQUIP_IRON);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PRISM = armor("prism", new int[]{2, 5, 6, 2}, 20, 0.5f, 0f,
            () -> Ingredient.of(ModItems.PRISM_SHARD.get()), SoundEvents.ARMOR_EQUIP_DIAMOND);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STAR_IRON = armor("star_iron", new int[]{3, 6, 8, 3}, 16, 3f, 0.1f,
            () -> Ingredient.of(ModItems.STAR_IRON_INGOT.get()), SoundEvents.ARMOR_EQUIP_NETHERITE);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARCHIVIST = armor("archivist", new int[]{2, 5, 6, 3}, 25, 1.5f, 0f,
            () -> Ingredient.of(ModItems.SOUL_ESSENCE.get()), SoundEvents.ARMOR_EQUIP_ELYTRA);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PHANTOM = armor("phantom", new int[]{3, 6, 8, 3}, 24, 2f, 0f,
            () -> Ingredient.of(ModItems.PHANTOM_ESSENCE.get()), SoundEvents.ARMOR_EQUIP_CHAIN);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> armor(String name, int[] def, int ench, float tough, float kb,
                                                                      java.util.function.Supplier<Ingredient> repair,
                                                                      net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.BOOTS, def[0]);
                    m.put(ArmorItem.Type.LEGGINGS, def[1]);
                    m.put(ArmorItem.Type.CHESTPLATE, def[2]);
                    m.put(ArmorItem.Type.HELMET, def[3]);
                    m.put(ArmorItem.Type.BODY, def[2]);
                }),
                ench, sound, repair, List.of(new ArmorMaterial.Layer(AetherWastes.id(name))), tough, kb));
    }

    // ---------------- Броня ----------------
    public static final DeferredItem<ArmorItem>[] ASHEN_SET = set("ashen", ASHEN, 12);
    public static final DeferredItem<ArmorItem>[] ETHER_STEEL_SET = set("ether_steel", ETHER_STEEL, 20);
    public static final DeferredItem<ArmorItem>[] PRISM_SET = set("prism", PRISM, 18);
    public static final DeferredItem<ArmorItem>[] STAR_IRON_SET = set("star_iron", STAR_IRON, 38);
    public static final DeferredItem<ArmorItem>[] ARCHIVIST_SET = set("archivist", ARCHIVIST, 30);
    public static final DeferredItem<ArmorItem>[] PHANTOM_SET = set("phantom", PHANTOM, 34);

    @SuppressWarnings("unchecked")
    private static DeferredItem<ArmorItem>[] set(String name, DeferredHolder<ArmorMaterial, ArmorMaterial> mat, int durability) {
        ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        String[] suffix = {"helmet", "chestplate", "leggings", "boots"};
        DeferredItem<ArmorItem>[] out = new DeferredItem[4];
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type type = types[i];
            Rarity rarity = name.equals("phantom") ? Rarity.EPIC : name.equals("star_iron") || name.equals("archivist") ? Rarity.RARE : Rarity.COMMON;
            out[i] = ModItems.ITEMS.register(name + "_" + suffix[i],
                    () -> new ArmorItem(mat, type, new Item.Properties().durability(type.getDurability(durability)).rarity(rarity)));
        }
        return out;
    }

    // ---------------- Инструменты ----------------
    public static final DeferredItem<Item> ETHER_STEEL_SWORD = ModItems.ITEMS.register("ether_steel_sword",
            () -> new SwordItem(ETHER_STEEL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(ETHER_STEEL_TIER, 3, -2.4f))));
    public static final DeferredItem<Item> ETHER_STEEL_PICKAXE = ModItems.ITEMS.register("ether_steel_pickaxe",
            () -> new PickaxeItem(ETHER_STEEL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(ETHER_STEEL_TIER, 1f, -2.8f))));
    public static final DeferredItem<Item> ETHER_STEEL_AXE = ModItems.ITEMS.register("ether_steel_axe",
            () -> new AxeItem(ETHER_STEEL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(ETHER_STEEL_TIER, 6f, -3.1f))));
    public static final DeferredItem<Item> ETHER_STEEL_SHOVEL = ModItems.ITEMS.register("ether_steel_shovel",
            () -> new ShovelItem(ETHER_STEEL_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(ETHER_STEEL_TIER, 1.5f, -3f))));
    public static final DeferredItem<Item> STAR_IRON_SWORD = ModItems.ITEMS.register("star_iron_sword",
            () -> new SwordItem(STAR_IRON_TIER, new Item.Properties().rarity(Rarity.RARE).attributes(SwordItem.createAttributes(STAR_IRON_TIER, 3, -2.4f))));
    public static final DeferredItem<Item> STAR_IRON_PICKAXE = ModItems.ITEMS.register("star_iron_pickaxe",
            () -> new PickaxeItem(STAR_IRON_TIER, new Item.Properties().rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(STAR_IRON_TIER, 1f, -2.8f))));
    public static final DeferredItem<Item> STAR_IRON_AXE = ModItems.ITEMS.register("star_iron_axe",
            () -> new AxeItem(STAR_IRON_TIER, new Item.Properties().rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(STAR_IRON_TIER, 5f, -3f))));
    public static final DeferredItem<Item> STAR_IRON_SHOVEL = ModItems.ITEMS.register("star_iron_shovel",
            () -> new ShovelItem(STAR_IRON_TIER, new Item.Properties().rarity(Rarity.RARE).attributes(DiggerItem.createAttributes(STAR_IRON_TIER, 1.5f, -3f))));

    // ---------------- Особое оружие ----------------
    public static final DeferredItem<Item> ETHER_BLADE = weapon("ether_blade", ETHER_STEEL_TIER, 4, -2.3f, Ability.ETHER_DRAIN, Rarity.UNCOMMON);
    public static final DeferredItem<Item> SALT_SCYTHE = weapon("salt_scythe", ETHER_STEEL_TIER, 5, -2.9f, Ability.SWEEP, Rarity.UNCOMMON);
    public static final DeferredItem<Item> EMBER_GREATAXE = weapon("ember_greataxe", ETHER_STEEL_TIER, 8, -3.3f, Ability.EMBER, Rarity.UNCOMMON);
    public static final DeferredItem<Item> PRISM_DAGGER = weapon("prism_dagger", ETHER_STEEL_TIER, 1, -1.4f, Ability.BACKSTAB, Rarity.UNCOMMON);
    public static final DeferredItem<Item> STAR_LONGSWORD = weapon("star_longsword", STAR_IRON_TIER, 5, -2.6f, Ability.NIGHTFALL, Rarity.RARE);
    public static final DeferredItem<Item> SPARK_SCEPTER = weapon("spark_scepter", RELIC_TIER, 3, -2.2f, Ability.FIREBOLT, Rarity.EPIC);
    public static final DeferredItem<Item> RESONANCE_MAUL = weapon("resonance_maul", RELIC_TIER, 9, -3.4f, Ability.SLAM, Rarity.EPIC);
    public static final DeferredItem<Item> CONVERGENCE_GLAIVE = weapon("convergence_glaive", RELIC_TIER, 6, -2.8f, Ability.BLINK, Rarity.EPIC);
    public static final DeferredItem<Item> HEART_BLADE = weapon("heart_blade", RELIC_TIER, 7, -2.4f, Ability.HEART, Rarity.EPIC);

    // Трофеи подземелий
    public static final DeferredItem<Item> ECHO_REAPER = weapon("echo_reaper", RELIC_TIER, 8, -2.7f, Ability.PHASE_DASH, Rarity.EPIC);
    public static final DeferredItem<Item> CHRONICLE_BLADE = weapon("chronicle_blade", RELIC_TIER, 6, -2.3f, Ability.TIMESTOP, Rarity.EPIC);
    public static final DeferredItem<Item> COLOSSUS_HAMMER = weapon("colossus_hammer", RELIC_TIER, 10, -3.3f, Ability.ERUPTION, Rarity.EPIC);

    public static final DeferredItem<Item> ETHER_BOW = ModItems.ITEMS.register("ether_bow",
            () -> new BowItem(new Item.Properties().durability(640).rarity(Rarity.UNCOMMON)));

    private static DeferredItem<Item> weapon(String name, Tier tier, int dmg, float speed, Ability ability, Rarity rarity) {
        return ModItems.ITEMS.register(name, () -> new WastesWeapon(tier, ability,
                new Item.Properties().rarity(rarity).attributes(SwordItem.createAttributes(tier, dmg, speed))));
    }

    /** Нужно, чтобы статические поля загрузились до регистрации предметов. */
    public static void init() {
        AetherWastes.LOGGER.debug("Aether Wastes gear: {} armor pieces", ASHEN_SET.length * 5);
    }

    private ModGear() {}
}
