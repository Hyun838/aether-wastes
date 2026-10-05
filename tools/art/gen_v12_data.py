"""Данные обновления 1.2: подземелья, Призрачная броня, хозяева подземелий, блоки, лут, перевод."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
ROOT = "/home/claude/aetherwastes/src/main/resources"
NS = "aetherwastes"
D = f"{ROOT}/data/{NS}"
L = f"{ROOT}/assets/{NS}/lang"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def n(x):
    return x if ":" in x else f"{NS}:{x}"


def ing(x):
    return x if isinstance(x, dict) else {"item": n(x)}


def shaped(name, pattern, key, result, count=1):
    write(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                                      "key": {k: ing(v) for k, v in key.items()}, "result": {"id": n(result), "count": count}})


def shapeless(name, items, result, count=1):
    write(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                      "ingredients": [ing(i) for i in items], "result": {"id": n(result), "count": count}})


def drop(name, lo, hi, chance=None, looting=True):
    e = {"type": "minecraft:item", "name": n(name), "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
    if looting:
        e["functions"].append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    pool = {"rolls": 1, "bonus_rolls": 0, "entries": [e]}
    if chance:
        pool["conditions"] = [{"condition": "minecraft:killed_by_player"}, {"condition": "minecraft:random_chance", "chance": chance}]
    return pool


def mob_loot(name, pools):
    write(f"{D}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{name}"})



A = f"{ROOT}/assets/{NS}"

# ---------------- Структуры ----------------
KINDS = {
    "lost_archive": ("archive", "underground_structures", "none", 40, 14, 81231711),
    "echo_crypt": ("crypt", "underground_structures", "none", 34, 12, 51472203),
    "ash_citadel": ("citadel", "surface_structures", "beard_thin", 38, 14, 73390117),
}
BIOMES = {
    "lost_archive": ["#minecraft:is_mountain", "#minecraft:is_taiga", "minecraft:snowy_plains", "minecraft:plains", "minecraft:desert",
                     "minecraft:dripstone_caves", "minecraft:deep_dark", "minecraft:lush_caves", "minecraft:windswept_hills",
                     "minecraft:meadow", "minecraft:cherry_grove"],
    "echo_crypt": ["#minecraft:is_forest", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:swamp", "minecraft:taiga",
                   "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:snowy_plains", "minecraft:meadow",
                   "minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:deep_dark"],
    "ash_citadel": ["minecraft:desert", "#minecraft:is_badlands", "#minecraft:is_savanna", "minecraft:plains", "minecraft:snowy_plains"],
}
for name, (kind, step, adapt, spacing, sep, salt) in KINDS.items():
    write(f"{D}/worldgen/structure/{name}.json", {"type": f"{NS}:dungeon", "kind": kind, "biomes": f"#{NS}:has_structure/{name}",
                                                  "step": step, "spawn_overrides": {}, "terrain_adaptation": adapt})
    write(f"{D}/worldgen/structure_set/{name}.json", {"structures": [{"structure": f"{NS}:{name}", "weight": 1}],
                                                      "placement": {"type": "minecraft:random_spread", "spacing": spacing,
                                                                    "separation": sep, "salt": salt}})
    write(f"{D}/tags/worldgen/biome/has_structure/{name}.json", {"replace": False, "values": BIOMES[name]})
write(f"{D}/tags/worldgen/structure/dungeons.json", {"replace": False, "values": [f"{NS}:{k}" for k in KINDS]})

# ---------------- Лут сундуков ----------------
def e(item, w=1, lo=1, hi=1, extra=None):
    fn = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}] if hi > 1 or lo > 1 else []
    if extra:
        fn += extra
    ent = {"type": "minecraft:item", "name": n(item), "weight": w}
    if fn:
        ent["functions"] = fn
    return ent


def book(w):
    return {"type": "minecraft:item", "name": "minecraft:book", "weight": w,
            "functions": [{"function": "minecraft:enchant_randomly"}]}


def potion(p, w):
    return {"type": "minecraft:item", "name": "minecraft:potion", "weight": w,
            "functions": [{"function": "minecraft:set_potion", "id": f"minecraft:{p}"}]}


def gear(item, w, lo=0.4, hi=0.9, ench=False):
    fn = [{"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    if ench:
        fn.append({"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 15, "max": 30}})
    return {"type": "minecraft:item", "name": n(item), "weight": w, "functions": fn}


def pool(rolls, entries, empty=0):
    if empty:
        entries = entries + [{"type": "minecraft:empty", "weight": empty}]
    r = rolls if isinstance(rolls, (int, float)) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}
    return {"rolls": r, "bonus_rolls": 0, "entries": entries}


def chest(name, pools):
    write(f"{D}/loot_table/chests/dungeon/{name}.json", {"type": "minecraft:chest", "pools": pools,
                                                         "random_sequence": f"{NS}:chests/dungeon/{name}"})


BASIC = [e("minecraft:bread", 10, 2, 5), e("minecraft:arrow", 8, 4, 12), e("minecraft:iron_ingot", 8, 1, 4), e("minecraft:gold_ingot", 5, 1, 3),
         e("ether_shard", 10, 2, 6), e("salt", 6, 2, 6), e("minecraft:bone", 8, 2, 6), e("glyph_blank", 4, 1, 2),
         potion("healing", 4), e("minecraft:torch", 6, 4, 10), e("bandage", 4, 1, 3), e("minecraft:experience_bottle", 4, 1, 3)]
PHANTOM = ["phantom_helmet", "phantom_chestplate", "phantom_leggings", "phantom_boots"]
KIND_RARE = {
    "archive": [e("soul_essence", 8, 1, 3), e("glyph_forge", 3), e("glyph_flow", 3), e("glyph_silence", 3), e("glyph_star", 3),
                gear("archivist_helmet", 2), gear("archivist_boots", 2), e("ether_steel_ingot", 6, 1, 3), book(8),
                e("minecraft:amethyst_shard", 6, 2, 6), e("dungeon_map", 3)],
    "crypt": [e("phantom_essence", 8, 1, 2), e("soul_essence", 6, 1, 3), gear("salt_scythe", 2), e("minecraft:golden_apple", 3),
              book(6), e("ether_steel_ingot", 5, 1, 3)] + [gear(p, 1, ench=False) for p in PHANTOM] + [e("dungeon_map", 3)],
    "citadel": [e("star_iron_ingot", 4, 1, 2), e("ash_pelt", 8, 2, 4), gear("ember_greataxe", 2), gear("ashen_chestplate", 2),
                e("minecraft:netherite_scrap", 2), e("minecraft:blaze_rod", 6, 1, 3), book(6), e("ether_steel_ingot", 6, 1, 3),
                e("dungeon_map", 3)],
}
TROPHY = {"archive": "chronicle_blade", "crypt": "echo_reaper", "citadel": "colossus_hammer"}
for kind, rare in KIND_RARE.items():
    chest(f"{kind}_entrance", [pool((3, 5), [e("minecraft:bread", 10, 2, 4), e("minecraft:torch", 10, 8, 16), potion("healing", 4),
                                             e("bandage", 6, 1, 3), e("minecraft:cooked_beef", 6, 2, 4), e("dungeon_map", 1)])])
    chest(f"{kind}_common", [pool((3, 6), BASIC), pool((0, 1), rare, empty=6)])
    chest(f"{kind}_supplies", [pool((3, 5), [potion("strong_healing", 6), potion("regeneration", 4), potion("strength", 3),
                                             e("minecraft:golden_apple", 4, 1, 2), e("minecraft:cooked_beef", 6, 3, 6),
                                             e("minecraft:arrow", 4, 8, 16), e("minecraft:totem_of_undying", 1)])])
    chest(f"{kind}_treasure", [pool((2, 4), rare), pool((2, 4), BASIC),
                               pool(1, [e("minecraft:diamond", 4, 1, 3), e("minecraft:emerald", 5, 2, 5), book(4)], empty=3)])
    chest(f"{kind}_shrine", [pool((2, 3), rare), pool(1, [book(1)])])
    boss = [pool((3, 5), rare), pool((2, 3), [e("minecraft:diamond", 5, 1, 3), e("minecraft:emerald", 4, 3, 7),
                                               e("minecraft:experience_bottle", 6, 3, 8), book(4),
                                               e("minecraft:enchanted_golden_apple", 1)]),
            pool(1, [e(TROPHY[kind], 1)], empty=3)]
    if kind == "crypt":
        boss.append(pool(1, [gear(p, 1, 0.0, 0.2) for p in PHANTOM]))
        boss.append(pool(1, [e("phantom_essence", 1, 2, 4)]))
    chest(f"{kind}_boss", boss)

# ---------------- Лут хозяев ----------------
def guaranteed(item, lo, hi):
    return {"rolls": 1, "bonus_rolls": 0, "entries": [e(item, 1, lo, hi)]}


mob_loot("echo_lord", [guaranteed("phantom_essence", 4, 7),
                       {"rolls": 1, "bonus_rolls": 0, "entries": [gear(p, 1, 0.0, 0.1) for p in PHANTOM]},
                       {"rolls": 1, "bonus_rolls": 0, "entries": [e("echo_reaper", 2), {"type": "minecraft:empty", "weight": 3}]},
                       guaranteed("soul_essence", 2, 4)])
mob_loot("archive_keeper", [guaranteed("soul_essence", 4, 8), guaranteed("phantom_essence", 1, 2),
                            {"rolls": 1, "bonus_rolls": 0, "entries": [e("chronicle_blade", 2), {"type": "minecraft:empty", "weight": 3}]},
                            {"rolls": 1, "bonus_rolls": 0, "entries": [gear(f"archivist_{p}", 1, 0.0, 0.2) for p in ("helmet", "chestplate", "leggings", "boots")]}])
mob_loot("ash_colossus", [guaranteed("star_iron_ingot", 3, 6), guaranteed("ash_pelt", 4, 8), guaranteed("phantom_essence", 1, 2),
                          {"rolls": 1, "bonus_rolls": 0, "entries": [e("colossus_hammer", 2), {"type": "minecraft:empty", "weight": 3}]}])

# ---------------- Рецепты ----------------
shaped("phantom_helmet", ["EIE", "E E"], {"E": "phantom_essence", "I": "ether_steel_ingot"}, "phantom_helmet")
shaped("phantom_chestplate", ["E E", "EIE", "EEE"], {"E": "phantom_essence", "I": "ether_steel_ingot"}, "phantom_chestplate")
shaped("phantom_leggings", ["EIE", "E E", "E E"], {"E": "phantom_essence", "I": "ether_steel_ingot"}, "phantom_leggings")
shaped("phantom_boots", ["E E", "I I"], {"E": "phantom_essence", "I": "ether_steel_ingot"}, "phantom_boots")
shapeless("dungeon_map", ["minecraft:map", "ether_shard", "minecraft:bone"], "dungeon_map")
shaped("rune_bricks", ["BB", "BS"], {"B": "minecraft:deepslate_bricks", "S": "ether_shard"}, "rune_bricks", 3)
shaped("rune_pillar", ["R", "R"], {"R": "rune_bricks"}, "rune_pillar", 2)
shaped("phantom_lantern", ["GSG", "SPS", "GSG"], {"G": "minecraft:glass", "S": "ether_shard", "P": "phantom_essence"}, "phantom_lantern", 4)
shaped("phantom_glass", ["GGG", "GPG", "GGG"], {"G": "minecraft:glass", "P": "phantom_essence"}, "phantom_glass", 8)
shaped("ash_bricks", ["BB", "BA"], {"B": "minecraft:polished_blackstone_bricks", "A": "ash_pelt"}, "ash_bricks", 3)
shaped("ember_bricks", ["BM", "MB"], {"B": "ash_bricks", "M": "minecraft:magma_block"}, "ember_bricks", 2)
shaped("archive_shelf", ["PPP", "BSB", "PPP"], {"P": "minecraft:dark_oak_planks", "B": "minecraft:book", "S": "soul_essence"}, "archive_shelf")
write(f"{D}/recipe/cracked_rune_bricks.json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": n("rune_bricks")},
                                                "result": {"id": n("cracked_rune_bricks")}, "experience": 0.1, "cookingtime": 200})

# ---------------- Лут блоков ----------------
for b in ("rune_bricks", "cracked_rune_bricks", "rune_pillar", "phantom_lantern", "ash_bricks", "ember_bricks", "archive_shelf"):
    write(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
          "entries": [{"type": "minecraft:item", "name": n(b)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          "random_sequence": f"{NS}:blocks/{b}"})
write(f"{D}/loot_table/blocks/phantom_glass.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
      "entries": [{"type": "minecraft:item", "name": n("phantom_glass")}], "conditions": [{"condition": "minecraft:match_tool",
      "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}],
      "random_sequence": f"{NS}:blocks/phantom_glass"})
# Блоки, которые добываются киркой
MINE = f"{ROOT}/data/minecraft/tags/block/mineable/pickaxe.json"
mine = load(MINE) if os.path.exists(MINE) else {"replace": False, "values": []}
for b in ("rune_bricks", "cracked_rune_bricks", "rune_pillar", "ash_bricks", "ember_bricks"):
    if n(b) not in mine["values"]:
        mine["values"].append(n(b))
write(MINE, mine)
AXE = f"{ROOT}/data/minecraft/tags/block/mineable/axe.json"
axe = load(AXE) if os.path.exists(AXE) else {"replace": False, "values": []}
if n("archive_shelf") not in axe["values"]:
    axe["values"].append(n("archive_shelf"))
write(AXE, axe)

# ---------------- Теги предметов ----------------
IT = f"{ROOT}/data/minecraft/tags/item"
for tagname, piece in (("head_armor", "helmet"), ("chest_armor", "chestplate"), ("leg_armor", "leggings"), ("foot_armor", "boots")):
    t = load(f"{IT}/{tagname}.json")
    if n(f"phantom_{piece}") not in t["values"]:
        t["values"].append(n(f"phantom_{piece}"))
    write(f"{IT}/{tagname}.json", t)
t = load(f"{IT}/swords.json")
for w in TROPHY.values():
    if n(w) not in t["values"]:
        t["values"].append(n(w))
write(f"{IT}/swords.json", t)

# ---------------- Модели блоков и предметов ----------------
def bs(name, variants):
    write(f"{A}/blockstates/{name}.json", {"variants": variants})


def bm(name, obj):
    write(f"{A}/models/block/{name}.json", obj)


def im(name, obj):
    write(f"{A}/models/item/{name}.json", obj)


for b in ("rune_bricks", "cracked_rune_bricks", "phantom_lantern", "ash_bricks", "ember_bricks"):
    bs(b, {"": {"model": f"{NS}:block/{b}"}})
    bm(b, {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{b}"}})
    im(b, {"parent": f"{NS}:block/{b}"})
bs("phantom_glass", {"": {"model": f"{NS}:block/phantom_glass"}})
bm("phantom_glass", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": f"{NS}:block/phantom_glass"}})
im("phantom_glass", {"parent": f"{NS}:block/phantom_glass"})
bs("rune_pillar", {"axis=y": {"model": f"{NS}:block/rune_pillar"},
                   "axis=x": {"model": f"{NS}:block/rune_pillar", "x": 90, "y": 90},
                   "axis=z": {"model": f"{NS}:block/rune_pillar", "x": 90}})
bm("rune_pillar", {"parent": "minecraft:block/cube_column", "textures": {"side": f"{NS}:block/rune_pillar", "end": f"{NS}:block/rune_pillar_top"}})
im("rune_pillar", {"parent": f"{NS}:block/rune_pillar"})
bs("archive_shelf", {"": {"model": f"{NS}:block/archive_shelf"}})
bm("archive_shelf", {"parent": "minecraft:block/cube_column", "textures": {"side": f"{NS}:block/archive_shelf", "end": "minecraft:block/dark_oak_planks"}})
im("archive_shelf", {"parent": f"{NS}:block/archive_shelf"})
# Ловушка: тонкая руна на полу
trap_variants = {}
for k, kn in enumerate(("archive", "crypt", "citadel")):
    for armed in ("true", "false"):
        mname = f"dungeon_trap_{kn}" + ("" if armed == "true" else "_off")
        trap_variants[f"armed={armed},kind={k}"] = {"model": f"{NS}:block/{mname}"}
        bm(mname, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
                   "textures": {"particle": f"{NS}:block/{mname}", "rune": f"{NS}:block/{mname}"},
                   "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16],
                                 "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#rune"},
                                           "down": {"uv": [0, 16, 16, 0], "texture": "#rune"}}}]})
bs("dungeon_trap", trap_variants)
im("dungeon_trap", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/dungeon_trap_archive"}})
# Печать Стража: плита-основание + парящий кристалл
seal_variants = {}
for k, kn in enumerate(("archive", "crypt", "citadel")):
    for active in ("true", "false"):
        mname = f"guardian_seal_{kn}" + ("" if active == "true" else "_dormant")
        seal_variants[f"active={active},kind={k}"] = {"model": f"{NS}:block/{mname}"}
        top = f"{NS}:block/guardian_seal_top_{kn}" if active == "true" else f"{NS}:block/guardian_seal_top_dormant"
        crystal = f"{NS}:block/seal_crystal_{kn}" if active == "true" else f"{NS}:block/seal_crystal_dormant"
        bm(mname, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                   "textures": {"particle": f"{NS}:block/guardian_seal_side", "side": f"{NS}:block/guardian_seal_side",
                                "top": top, "crystal": crystal},
                   "elements": [
                       {"from": [0, 0, 0], "to": [16, 10, 16], "faces": {
                           "down": {"texture": "#side", "cullface": "down"}, "up": {"texture": "#top"},
                           "north": {"uv": [0, 6, 16, 16], "texture": "#side"}, "south": {"uv": [0, 6, 16, 16], "texture": "#side"},
                           "west": {"uv": [0, 6, 16, 16], "texture": "#side"}, "east": {"uv": [0, 6, 16, 16], "texture": "#side"}}},
                       {"from": [5, 12, 5], "to": [11, 20, 11], "rotation": {"angle": 45, "axis": "y", "origin": [8, 16, 8]},
                        "faces": {f: {"uv": [0, 0, 6, 8], "texture": "#crystal"} for f in ("north", "south", "east", "west")}
                        | {"up": {"uv": [5, 5, 11, 11], "texture": "#crystal"}, "down": {"uv": [5, 5, 11, 11], "texture": "#crystal"}}},
                   ]})
bs("guardian_seal", seal_variants)
im("guardian_seal", {"parent": f"{NS}:block/guardian_seal_crypt"})
# Предметы
for it in ["phantom_essence", "dungeon_map"] + PHANTOM:
    im(it, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{it}"}})
for w in TROPHY.values():
    im(w, {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{w}"}})

# ---------------- Частицы ----------------
PART = {"ether_spark": 4, "phantom_wisp": 4, "rune": 6, "ash_ember": 4}
for p, frames in PART.items():
    write(f"{A}/particles/{p}.json", {"textures": [f"{NS}:{p}_{i}" for i in range(frames)]})

# ---------------- Звуки ----------------
SJ = f"{A}/sounds.json"
sj = load(SJ)
for s, cnt in (("phantom_phase", 2), ("phantom_return", 2), ("dungeon_ambient", 2), ("boss_slam", 2), ("seal_break", 1)):
    sj[s] = {"sounds": [f"{NS}:{s}{i + 1}" for i in range(cnt)], "subtitle": f"subtitles.{NS}.{s}"}
write(SJ, sj)

# ---------------- Достижения ----------------
ADV = f"{D}/advancement/main"


def adv(name, parent, icon, criteria, frame="task", xp=0, reqs=None):
    obj = {"parent": f"{NS}:main/{parent}",
           "display": {"icon": {"id": n(icon)}, "title": {"translate": f"advancements.{NS}.{name}.title"},
                       "description": {"translate": f"advancements.{NS}.{name}.desc"}, "frame": frame,
                       "show_toast": True, "announce_to_chat": True, "hidden": False}, "criteria": criteria}
    if xp:
        obj["rewards"] = {"experience": xp}
    if reqs:
        obj["requirements"] = reqs
    write(f"{ADV}/{name}.json", obj)


def kill(mob):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": n(mob)}}]}}


def visit(struct):
    return {"trigger": "minecraft:location", "conditions": {"player": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                        "predicate": {"location": {"structures": n(struct)}}}]}}


adv("dungeon", "root", "rune_bricks", {k: visit(k) for k in KINDS}, reqs=[list(KINDS)])
adv("boss_archive", "dungeon", "chronicle_blade", {"k": kill("archive_keeper")}, frame="goal", xp=300)
adv("boss_crypt", "dungeon", "echo_reaper", {"k": kill("echo_lord")}, frame="goal", xp=300)
adv("boss_citadel", "dungeon", "colossus_hammer", {"k": kill("ash_colossus")}, frame="goal", xp=300)
adv("phantom_set", "boss_crypt", "phantom_chestplate",
    {p: {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": n(p)}]}} for p in PHANTOM}, frame="challenge", xp=500)
adv("all_bosses", "boss_crypt", "guardian_seal", {"a": kill("archive_keeper"), "c": kill("echo_lord"), "z": kill("ash_colossus")},
    frame="challenge", xp=800)

# ---------------- Перевод ----------------
ru = load(f"{L}/ru_ru.json")
en = load(f"{L}/en_us.json")


def t(k, r, e_):
    ru[k] = r
    en[k] = e_


for k, (r, e_) in {
    "phantom_helmet": ("Призрачный шлем", "Phantom Helm"), "phantom_chestplate": ("Призрачный доспех", "Phantom Cuirass"),
    "phantom_leggings": ("Призрачные поножи", "Phantom Greaves"), "phantom_boots": ("Призрачные сапоги", "Phantom Boots"),
    "phantom_essence": ("Призрачная эссенция", "Phantom Essence"), "dungeon_map": ("Карта Картографа Пустошей", "Wastes Cartographer Map"),
    "echo_reaper": ("Коса Эха", "Echo Reaper"), "chronicle_blade": ("Клинок Летописца", "Chronicle Blade"),
    "colossus_hammer": ("Молот Колосса", "Colossus Hammer"),
}.items():
    t(f"item.{NS}.{k}", r, e_)
for k, (r, e_) in {
    "rune_bricks": ("Рунический кирпич", "Rune Bricks"), "cracked_rune_bricks": ("Потрескавшийся рунический кирпич", "Cracked Rune Bricks"),
    "rune_pillar": ("Рунная колонна", "Rune Pillar"), "phantom_lantern": ("Призрачный светильник", "Phantom Lantern"),
    "phantom_glass": ("Призрачное стекло", "Phantom Glass"), "ash_bricks": ("Пепельный кирпич", "Ash Bricks"),
    "ember_bricks": ("Тлеющий кирпич", "Ember Bricks"), "archive_shelf": ("Стеллаж Архива", "Archive Shelf"),
    "dungeon_trap": ("Руна-ловушка подземелья", "Dungeon Rune Trap"), "guardian_seal": ("Печать Стража", "Guardian Seal"),
}.items():
    t(f"block.{NS}.{k}", r, e_)
for k, (r, e_) in {"echo_lord": ("Лорд Эха", "Echo Lord"), "archive_keeper": ("Хранитель Архива", "Keeper of the Archive"),
                   "ash_colossus": ("Пепельный Колосс", "Ash Colossus")}.items():
    t(f"entity.{NS}.{k}", r, e_)
for k, (r, e_) in {"lost_archive": ("Затерянный Архив", "Lost Archive"), "echo_crypt": ("Склеп Эха", "Crypt of Echoes"),
                   "ash_citadel": ("Пепельная Цитадель", "Ash Citadel")}.items():
    t(f"structure.{NS}.{k}", r, e_)
t(f"item.{NS}.dungeon_map.desc", "ПКМ: отмечает ближайшее подземелье Пустошей", "Use: marks the nearest Wastes dungeon")
t(f"item.{NS}.dungeon_map.of", "Карта: %s", "Map: %s")
t(f"item.{NS}.dungeon_map.none", "Поблизости нет подземелий", "No dungeons nearby")
t(f"item.{NS}.dungeon_map.found", "%s — %s блоков отсюда", "%s — %s blocks away")
for k, (r, e_) in {
    "phase_dash": ("ПКМ: призрачный рывок на 10 блоков — сквозь врагов и стены", "Use: a phantom dash 10 blocks through foes and walls"),
    "timestop": ("ПКМ: время вокруг замирает — враги в радиусе 9 почти не двигаются 6 с", "Use: time stands still — foes within 9 blocks barely move for 6 s"),
    "eruption": ("ПКМ: огненное извержение вокруг вас — урон, поджог, отброс", "Use: a fiery eruption around you — damage, fire, knockback"),
}.items():
    t(f"weapon.{NS}.{k}", r, e_)
t(f"key.{NS}.phase", "Призрачный шаг (Призрачная броня)", "Phantom Step (Phantom armor)")
t(f"key.categories.{NS}", "Эфирные Пустоши", "Aether Wastes")
t(f"msg.{NS}.phase.need_set", "Нужен полный комплект Призрачной брони", "Requires the full Phantom armor set")
t(f"msg.{NS}.phase.cooldown", "Призрачный шаг восстановится через %s с", "Phantom Step ready in %s s")
t(f"msg.{NS}.phase.ready", "Призрачный шаг готов", "Phantom Step is ready")
t(f"msg.{NS}.phase.ending", "Возвращение в плоть через %s…", "Returning to flesh in %s…")
t(f"tooltip.{NS}.phantom_set", "Комплект: Призрачный шаг [%s] — 10 с полёта сквозь стены, перезарядка 60 с",
  "Set: Phantom Step [%s] — fly through walls for 10 s, 60 s cooldown")
t(f"boss.{NS}.enraged", "%s впадает в ярость!", "%s becomes enraged!")
t(f"boss.{NS}.defeated", "%s повержен. Сокровищница открыта.", "%s has fallen. The treasury is yours.")
for s, (r, e_) in {"phantom_phase": ("Призрачный шаг", "Phantom Step"), "phantom_return": ("Возвращение в плоть", "Return to flesh"),
                   "dungeon_ambient": ("Подземелье шепчет", "Dungeon whispers"), "boss_slam": ("Сокрушительный удар", "Crushing slam"),
                   "seal_break": ("Печать сломана", "Seal broken")}.items():
    t(f"subtitles.{NS}.{s}", r, e_)
ADVT = {
    "dungeon": ("Во тьму", "Into the Dark", "Найдите подземелье Пустошей", "Find a Wastes dungeon"),
    "boss_archive": ("Последняя страница", "The Last Page", "Победите Хранителя Архива", "Defeat the Keeper of the Archive"),
    "boss_crypt": ("Тишина в склепе", "Silence in the Crypt", "Победите Лорда Эха", "Defeat the Echo Lord"),
    "boss_citadel": ("Пепел к пеплу", "Ashes to Ashes", "Победите Пепельного Колосса", "Defeat the Ash Colossus"),
    "phantom_set": ("Сквозь стены", "Through Walls", "Соберите полный комплект Призрачной брони", "Collect the full Phantom armor set"),
    "all_bosses": ("Покоритель подземелий", "Dungeon Conqueror", "Победите всех трёх хозяев подземелий", "Defeat all three dungeon masters"),
}
for k, (rt, et, rd, ed) in ADVT.items():
    t(f"advancements.{NS}.{k}.title", rt, et)
    t(f"advancements.{NS}.{k}.desc", rd, ed)
assert set(ru) == set(en)
write(f"{L}/ru_ru.json", ru)
write(f"{L}/en_us.json", en)
print("v1.2 data ok, lang:", len(ru))
