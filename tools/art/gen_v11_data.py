"""Данные обновления 1.1: рецепты, лут, спавн, теги, достижения, перевод."""
import json, os

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


# ---------------- Рецепты ----------------
shapeless("ether_steel_ingot", ["minecraft:iron_ingot", "ether_shard", "ether_shard", {"tag": "minecraft:coals"}], "ether_steel_ingot")
STICK = "minecraft:stick"
ARMOR = {"helmet": ["XXX", "X X"], "chestplate": ["X X", "XXX", "XXX"], "leggings": ["XXX", "X X", "X X"], "boots": ["X X", "X X"]}
for setn, mat in (("ashen", "ash_pelt"), ("ether_steel", "ether_steel_ingot"), ("prism", "prism_shard"), ("star_iron", "star_iron_ingot")):
    for piece, pat in ARMOR.items():
        shaped(f"{setn}_{piece}", pat, {"X": mat}, f"{setn}_{piece}")
# Облачение Архивариуса: эссенции душ + шерсть, в нагрудник — Осколок Скитальца
shaped("archivist_helmet", ["SWS", "W W"], {"S": "soul_essence", "W": "minecraft:purple_wool"}, "archivist_helmet")
shaped("archivist_chestplate", ["W W", "SKS", "WSW"], {"S": "soul_essence", "W": "minecraft:purple_wool", "K": "wanderer_shard"}, "archivist_chestplate")
shaped("archivist_leggings", ["SWS", "W W", "W W"], {"S": "soul_essence", "W": "minecraft:purple_wool"}, "archivist_leggings")
shaped("archivist_boots", ["S S", "W W"], {"S": "soul_essence", "W": "minecraft:purple_wool"}, "archivist_boots")
TOOLS = {"sword": [" X ", " X ", " # "], "pickaxe": ["XXX", " # ", " # "], "axe": ["XX ", "X# ", " # "], "shovel": [" X ", " # ", " # "]}
for mat, item in (("ether_steel", "ether_steel_ingot"), ("star_iron", "star_iron_ingot")):
    for tool, pat in TOOLS.items():
        shaped(f"{mat}_{tool}", pat, {"X": item, "#": STICK}, f"{mat}_{tool}")
shaped("ether_blade", ["  E", "SE ", "HS "], {"E": "ether_steel_ingot", "S": "ether_shard", "H": STICK}, "ether_blade")
shaped("salt_scythe", ["EES", "  H", " H "], {"E": "ether_steel_ingot", "S": "salt", "H": STICK}, "salt_scythe")
shaped("ember_greataxe", ["EM ", "EH ", " H "], {"E": "ether_steel_ingot", "M": "minecraft:magma_block", "H": STICK}, "ember_greataxe")
shaped("prism_dagger", [" P", "H "], {"P": "prism_shard", "H": "ether_steel_ingot"}, "prism_dagger")
shaped("star_longsword", [" T ", " T ", "GHG"], {"T": "star_iron_ingot", "G": "minecraft:gold_ingot", "H": STICK}, "star_longsword")
shaped("ether_bow", [" HX", "E X", " HX"], {"H": "heartwood", "X": "minecraft:string", "E": "ether_shard"}, "ether_bow")

# ---------------- Лут ----------------
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


mob_loot("ash_hound", [drop("ash_pelt", 0, 2), drop("minecraft:bone", 0, 1)])
mob_loot("ether_wisp", [drop("ether_shard", 1, 2), drop("soul_essence", 1, 1, 0.15)])
mob_loot("scar_crawler", [drop("minecraft:string", 0, 2), drop("minecraft:spider_eye", 0, 1), drop("soul_essence", 1, 1, 0.25)])
mob_loot("glassman", [drop("prism_shard", 1, 2), drop("minecraft:bone", 0, 2)])
mob_loot("resin_walker", [drop("resin", 1, 2), drop("heartwood", 0, 1)])
mob_loot("salt_wraith", [drop("salt", 1, 3), drop("soul_essence", 1, 1, 0.1)])
RELIC = {"spark": "spark_scepter", "resonance": "resonance_maul", "convergence": "convergence_glaive", "heart": "heart_blade"}
for w, relic in RELIC.items():
    mob_loot(f"wanderer_{w}", [drop(relic, 1, 1, looting=False), drop("star_iron_ingot", 2, 4), drop("ether_shard", 6, 12)])

def chest_add(name, entries):
    p = f"{D}/loot_table/chests/{name}.json"
    lt = load(p)
    lt["pools"].append({"rolls": {"type": "minecraft:uniform", "min": 0, "max": 1}, "bonus_rolls": 0,
                        "entries": [{"type": "minecraft:item", "name": n(e), "weight": w} for e, w in entries]})
    write(p, lt)


chest_add("archive_ruins", [("ether_steel_ingot", 6), ("ether_blade", 1), ("ether_steel_helmet", 2), ("prism_dagger", 1), ("ether_bow", 1)])
chest_add("echo_tomb", [("archivist_helmet", 2), ("archivist_boots", 2), ("soul_essence", 6)])
chest_add("sunken_observatory", [("star_iron_helmet", 1), ("star_longsword", 1), ("star_iron_ingot", 4)])
chest_add("rift_reward", [("star_iron_chestplate", 1), ("salt_scythe", 2), ("ember_greataxe", 2), ("ether_bow", 2), ("star_iron_pickaxe", 2)])
chest_add("caravan_camp", [("ether_steel_ingot", 4), ("ashen_boots", 2), ("ash_pelt", 4)])
chest_add("ash_ruin", [("ashen_helmet", 2), ("ether_steel_ingot", 3), ("ash_pelt", 4)])

# ---------------- Спавн ----------------
BM = f"{D}/neoforge/biome_modifier"
def spawns(name, biomes, mob, weight, lo, hi):
    write(f"{BM}/{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
                                "spawners": [{"type": n(mob), "weight": weight, "minCount": lo, "maxCount": hi}]})


spawns("ash_hound", ["minecraft:plains", "minecraft:savanna", "minecraft:taiga", "minecraft:meadow", "minecraft:snowy_plains"], "ash_hound", 35, 2, 4)
spawns("ether_wisp", "#minecraft:is_overworld", "ether_wisp", 6, 1, 2)
spawns("scar_crawler", ["minecraft:dark_forest", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:deep_dark"], "scar_crawler", 18, 1, 2)
for b, extra in (("ashen_wastes", [("ash_hound", 60, 2, 4)]), ("whisper_saltflats", [("ether_wisp", 30, 1, 2)]),
                 ("bleeding_grove", [("scar_crawler", 40, 1, 2)]), ("glass_dunes", [("ether_wisp", 20, 1, 2)]),
                 ("fungal_hollows", [("scar_crawler", 50, 1, 2)]), ("frozen_storm_fields", [("ash_hound", 30, 2, 3)]),
                 ("star_shoals", [("ether_wisp", 60, 1, 3)])):
    p = f"{D}/worldgen/biome/{b}.json"
    bio = load(p)
    for mob, w, lo, hi in extra:
        if not any(s["type"] == n(mob) for s in bio["spawners"]["monster"]):
            bio["spawners"]["monster"].append({"type": n(mob), "weight": w, "minCount": lo, "maxCount": hi})
    write(p, bio)

# ---------------- Теги предметов ----------------
IT = f"{ROOT}/data/minecraft/tags/item"
SETS = ["ashen", "ether_steel", "prism", "star_iron", "archivist"]
def tag(name, vals):
    write(f"{IT}/{name}.json", {"replace": False, "values": [n(v) for v in vals]})


tag("head_armor", [f"{s}_helmet" for s in SETS])
tag("chest_armor", [f"{s}_chestplate" for s in SETS])
tag("leg_armor", [f"{s}_leggings" for s in SETS])
tag("foot_armor", [f"{s}_boots" for s in SETS])
WEAPONS = ["ether_blade", "salt_scythe", "ember_greataxe", "prism_dagger", "star_longsword", "spark_scepter",
           "resonance_maul", "convergence_glaive", "heart_blade"]
tag("swords", ["ether_steel_sword", "star_iron_sword"] + WEAPONS)
tag("pickaxes", ["ether_steel_pickaxe", "star_iron_pickaxe"])
tag("axes", ["ether_steel_axe", "star_iron_axe"])
tag("shovels", ["ether_steel_shovel", "star_iron_shovel"])
tag("enchantable/bow", ["ether_bow"])
tag("enchantable/durability", ["ether_bow"])

# ---------------- Достижения ----------------
A = f"{D}/advancement/main"
ADV = []


def adv(name, parent, icon, criteria, frame="task", reqs=None, xp=0, hidden=False):
    obj = {"display": {"icon": {"id": n(icon)}, "title": {"translate": f"advancements.{NS}.{name}.title"},
                       "description": {"translate": f"advancements.{NS}.{name}.desc"}, "frame": frame,
                       "show_toast": True, "announce_to_chat": True, "hidden": hidden},
           "criteria": criteria}
    if parent:
        obj["parent"] = f"{NS}:main/{parent}"
    else:
        obj["display"]["background"] = "minecraft:textures/gui/advancements/backgrounds/stone.png"
    if reqs:
        obj["requirements"] = reqs
    if xp:
        obj["rewards"] = {"experience": xp}
    write(f"{A}/{name}.json", obj)
    ADV.append(name)


def has(*items):
    return {f"has_{i}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": n(it)}]}}
            for i, it in enumerate(items)}


def any_of(crit):
    return [list(crit.keys())]


def kill(mob):
    return {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": [
        {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": n(mob)}}]}}


adv("root", None, "ether_shard", has("ether_shard"))
adv("compass", "root", "ether_compass", has("ether_compass"))
adv("anchor", "compass", "anchor", {"placed": {"trigger": "minecraft:placed_block", "conditions": {"location": [
    {"condition": "minecraft:block_state_property", "block": n("anchor")}]}}})
ess = has(*[f"glyph_{e}" for e in ("forge", "flow", "root", "silence", "stone", "ash", "star")])
adv("glyph", "anchor", "glyph_forge", ess, reqs=any_of(ess))
adv("staff", "glyph", "ether_staff", has("ether_staff"))
adv("forge", "anchor", "forge_hearth", {"placed": {"trigger": "minecraft:placed_block", "conditions": {"location": [
    {"condition": "minecraft:block_state_property", "block": n("forge_hearth")}]}}})
adv("ether_steel", "forge", "ether_steel_ingot", has("ether_steel_ingot"))
adv("ether_armor", "ether_steel", "ether_steel_chestplate", has("ether_steel_chestplate"))
adv("special_weapon", "ether_steel", "ether_blade", has("ether_blade", "salt_scythe", "ember_greataxe", "prism_dagger", "ether_bow"),
    reqs=[["has_0", "has_1", "has_2", "has_3", "has_4"]])
adv("star_iron", "ether_armor", "star_iron_ingot", has("star_iron_ingot"))
adv("star_armor", "star_iron", "star_iron_chestplate", has("star_iron_chestplate", "star_iron_helmet", "star_iron_leggings", "star_iron_boots"),
    frame="goal", xp=100)
adv("archivist", "staff", "archivist_chestplate", has("archivist_chestplate"), frame="goal", xp=100)
hunt = {m: kill(m) for m in ("ash_hound", "salt_wraith", "resin_walker", "glassman", "ether_wisp", "scar_crawler")}
adv("hunter", "root", "ash_pelt", hunt, frame="challenge", xp=150)
adv("wanderer_spark", "staff", "spark_scepter", {"k": kill("wanderer_spark")}, frame="goal", xp=200)
adv("wanderer_resonance", "wanderer_spark", "resonance_maul", {"k": kill("wanderer_resonance")}, frame="goal", xp=300)
adv("underside", "wanderer_resonance", "underside_portal", {"d": {"trigger": "minecraft:changed_dimension", "conditions": {"to": n("underside")}}})
adv("wanderer_convergence", "underside", "convergence_glaive", {"k": kill("wanderer_convergence")}, frame="goal", xp=400)
adv("wanderer_heart", "wanderer_convergence", "heart_blade", {"k": kill("wanderer_heart")}, frame="challenge", xp=1000)
relics = has("spark_scepter", "resonance_maul", "convergence_glaive", "heart_blade")
adv("all_relics", "wanderer_heart", "heart_blade", relics, frame="challenge", xp=500)
print("data ok:", len(ADV), "advancements")

# ---------------- Перевод ----------------
ru = load(f"{L}/ru_ru.json")
en = load(f"{L}/en_us.json")


def t(k, r, e):
    ru[k] = r
    en[k] = e


SETNAMES = {"ashen": ("Пепельного странника", "Ashen Wanderer"), "ether_steel": ("из эфирной стали", "Ether Steel"),
            "prism": ("призматический", "Prismatic"), "star_iron": ("из звёздного железа", "Star Iron"),
            "archivist": ("Архивариуса", "Archivist")}
PIECES = {"helmet": ("Шлем", "Helmet"), "chestplate": ("Нагрудник", "Chestplate"), "leggings": ("Поножи", "Leggings"), "boots": ("Сапоги", "Boots")}
for s, (rs, es) in SETNAMES.items():
    for p, (rp, ep) in PIECES.items():
        if s == "prism":
            rr = {"helmet": "Призматический шлем", "chestplate": "Призматический нагрудник", "leggings": "Призматические поножи", "boots": "Призматические сапоги"}[p]
        elif s == "archivist":
            rr = {"helmet": "Капюшон Архивариуса", "chestplate": "Мантия Архивариуса", "leggings": "Полы Архивариуса", "boots": "Сапоги Архивариуса"}[p]
        else:
            rr = f"{rp} {rs}"
        t(f"item.{NS}.{s}_{p}", rr, f"{es} {ep}" if s != "archivist" else {"helmet": "Archivist Hood", "chestplate": "Archivist Mantle", "leggings": "Archivist Robes", "boots": "Archivist Boots"}[p])
TOOLN = {"sword": ("Меч", "Sword"), "pickaxe": ("Кирка", "Pickaxe"), "axe": ("Топор", "Axe"), "shovel": ("Лопата", "Shovel")}
for m, (rm, em) in (("ether_steel", ("из эфирной стали", "Ether Steel")), ("star_iron", ("из звёздного железа", "Star Iron"))):
    for tl, (rt, et) in TOOLN.items():
        t(f"item.{NS}.{m}_{tl}", f"{rt} {rm}", f"{em} {et}")
ITEMS = {
    "ether_steel_ingot": ("Слиток эфирной стали", "Ether Steel Ingot"), "prism_shard": ("Призматический осколок", "Prism Shard"),
    "ash_pelt": ("Пепельная шкура", "Ash Pelt"), "heartwood": ("Сердцевина", "Heartwood"),
    "ether_blade": ("Эфирный клинок", "Ether Blade"), "salt_scythe": ("Солевая коса", "Salt Scythe"),
    "ember_greataxe": ("Тлеющий секач", "Ember Greataxe"), "prism_dagger": ("Призматический кинжал", "Prism Dagger"),
    "star_longsword": ("Звёздный длинный меч", "Star Longsword"), "spark_scepter": ("Скипетр Искры", "Scepter of Spark"),
    "resonance_maul": ("Молот Резонанса", "Maul of Resonance"), "convergence_glaive": ("Глефа Схождения", "Glaive of Convergence"),
    "heart_blade": ("Клинок Сердца", "Blade of the Heart"), "ether_bow": ("Эфирный лук", "Ether Bow"),
    "ash_hound_spawn_egg": ("Яйцо призыва пепельного пса", "Ash Hound Spawn Egg"),
    "ether_wisp_spawn_egg": ("Яйцо призыва эфирного огонька", "Ether Wisp Spawn Egg"),
    "scar_crawler_spawn_egg": ("Яйцо призыва ползуна Шрама", "Scar Crawler Spawn Egg"),
}
for k, (r, e) in ITEMS.items():
    t(f"item.{NS}.{k}", r, e)
for k, (r, e) in {"ash_hound": ("Пепельный пёс", "Ash Hound"), "ether_wisp": ("Эфирный огонёк", "Ether Wisp"),
                  "scar_crawler": ("Ползун Шрама", "Scar Crawler"), "heart_guardian": ("Страж Сердца", "Heart Guardian")}.items():
    t(f"entity.{NS}.{k}", r, e)
W = {
    "ether_drain": ("Удар наполняет Сосуд на 4 Эфира", "Hits restore 4 Aether to your Vessel"),
    "sweep": ("Широкий взмах: ранит и замедляет всех врагов рядом", "Wide sweep: hurts and slows all nearby foes"),
    "ember": ("Поджигает цель на 5 секунд", "Sets the target ablaze for 5 seconds"),
    "backstab": ("Двойной урон при ударе в спину", "Double damage when striking from behind"),
    "nightfall": ("Ночью урон +50%", "+50% damage at night"),
    "firebolt": ("ПКМ: веер из трёх огненных снарядов", "Use: a fan of three fire bolts"),
    "slam": ("ПКМ: удар о землю — урон и подброс врагов в радиусе 5", "Use: ground slam that hurls foes within 5 blocks"),
    "blink": ("ПКМ: рывок сквозь врагов на 9 блоков", "Use: dash 9 blocks through your foes"),
    "heart": ("Вампиризм; ПКМ: призыв двух Стражей Сердца", "Lifesteal; Use: summon two Heart Guardians"),
}
for k, (r, e) in W.items():
    t(f"weapon.{NS}.{k}", r, e)
t(f"weapon.{NS}.active", "Стоит %s Эфира, перезарядка %s с", "Costs %s Aether, cooldown %s s")
OBJ = {
    "compass": ("найдите фиолетовую Эфирную жилу (под землёй), добудьте Осколки и сделайте Эфирный компас",
                "find a purple Aether Vein underground, mine Shards and craft an Aether Compass"),
    "anchor": ("поставьте Якорь — сердце вашей базы (кость, Осколки, костёр, булыжник)",
               "place an Anchor, the heart of your base (bones, Shards, campfire, cobblestone)"),
    "school": ("сделайте пластину глифа и глиф-Суть любой школы, затем ПКМ им в воздухе",
               "craft a glyph plate and an Essence glyph of any school, then use it in the air"),
    "inscribe": ("сделайте Скрижаль Глифики и Эфирный жезл; положите Форму и Суть и ПКМ жезлом по Скрижали",
                 "craft a Glyph Slate and an Aether Staff; place a Form and an Essence, then use the staff on the Slate"),
    "cast": ("произнесите заклинание: ПКМ жезлом", "cast a spell: use the staff"),
    "forge": ("постройте Горн, раздуйте его (ПКМ) и выкуйте вещь (Shift + ПКМ на жаре 65–75)",
              "build a Forge Hearth, pump it and forge an item (sneak + use at heat 65–75)"),
    "armor": ("соберите полный комплект брони Пустошей — у каждого есть бонус",
              "assemble a full Wastes armor set — every set has a bonus"),
    "gate1": ("переживите первый Прилив Эфира (5-я ночь недели)", "survive the first Aether Tide (5th night of the week)"),
    "gate2_spark": ("будьте готовы: Скиталец Искры придёт ночью — победите его", "be ready: the Wanderer of Spark comes at night — defeat it"),
    "gate2_insights": ("соберите 15 Озарений (%s/15): изучайте руды компасом, убивайте новых существ, путешествуйте",
                       "gather 15 Insights (%s/15): study ores with the compass, slay new creatures, travel"),
    "gate3_resonance": ("победите Скитальца Резонанса", "defeat the Wanderer of Resonance"),
    "gate3_underside": ("проведите ритуал Портального камня и войдите в Изнанку", "perform the Portal Stone ritual and enter the Underside"),
    "gate4_convergence": ("победите Скитальца Схождения", "defeat the Wanderer of Convergence"),
    "gate4_circle": ("достигните 5-го круга мастерства в любой школе (сейчас %s)", "reach mastery circle 5 in any school (now %s)"),
    "heart": ("отправляйтесь к Сердцу Раскола в центре Изнанки (0, 131, 0)", "travel to the Heart of the Rift at the centre of the Underside (0, 131, 0)"),
    "free": ("мир ваш. Исследуйте Разломы, соберите все реликвии", "the world is yours. Explore Rifts and collect every relic"),
}
for k, (r, e) in OBJ.items():
    t(f"objective.{NS}.{k}", r, e)
t(f"hud.{NS}.goal", "Цель: ", "Goal: ")
ADVT = {
    "root": ("Эфирные Пустоши", "Aether Wastes", "Добудьте Осколок Эфира", "Obtain an Aether Shard"),
    "compass": ("Чувство Эфира", "Aether Sense", "Сделайте Эфирный компас", "Craft an Aether Compass"),
    "anchor": ("Тихая гавань", "Safe Harbour", "Поставьте Якорь", "Place an Anchor"),
    "glyph": ("Первый знак", "First Sign", "Сделайте глиф-Суть", "Craft an Essence glyph"),
    "staff": ("Голос Эфира", "Voice of Aether", "Сделайте Эфирный жезл", "Craft an Aether Staff"),
    "forge": ("Жар и металл", "Heat and Metal", "Поставьте Горн", "Place a Forge Hearth"),
    "ether_steel": ("Сталь, что помнит", "Steel That Remembers", "Выплавьте эфирную сталь", "Make Ether Steel"),
    "ether_armor": ("Закованный", "Clad in Steel", "Сделайте нагрудник из эфирной стали", "Craft an Ether Steel Chestplate"),
    "special_weapon": ("Оружие Пустошей", "Weapon of the Wastes", "Получите особое оружие", "Obtain a special weapon"),
    "star_iron": ("Упавшее небо", "Fallen Sky", "Добудьте звёздное железо", "Obtain Star Iron"),
    "star_armor": ("Звёздный доспех", "Starforged", "Сделайте часть брони из звёздного железа", "Craft Star Iron armor"),
    "archivist": ("Наследник Ордена", "Heir of the Order", "Наденьте мантию Архивариуса", "Obtain the Archivist Mantle"),
    "hunter": ("Охотник Пустошей", "Hunter of the Wastes", "Убейте всех новых существ Пустошей", "Slay every new creature of the Wastes"),
    "wanderer_spark": ("Погасшая Искра", "Spark Extinguished", "Победите Скитальца Искры", "Defeat the Wanderer of Spark"),
    "wanderer_resonance": ("Тишина после бури", "Silence After the Storm", "Победите Скитальца Резонанса", "Defeat the Wanderer of Resonance"),
    "underside": ("По ту сторону", "The Other Side", "Войдите в Изнанку", "Enter the Underside"),
    "wanderer_convergence": ("Разомкнутый круг", "The Circle Broken", "Победите Скитальца Схождения", "Defeat the Wanderer of Convergence"),
    "wanderer_heart": ("Сердце Раскола", "Heart of the Rift", "Победите Последнего Скитальца", "Defeat the Last Wanderer"),
    "all_relics": ("Хранитель реликвий", "Keeper of Relics", "Соберите все четыре реликвии Скитальцев", "Collect all four Wanderer relics"),
}
for k, (rt, et, rd, ed) in ADVT.items():
    t(f"advancements.{NS}.{k}.title", rt, et)
    t(f"advancements.{NS}.{k}.desc", rd, ed)
t(f"message.{NS}.welcome", "Вы — Пробуждённый. Мир расколот, Эфир сочится из трещин. Цель — в левом верхнем углу; Журнал (ПКМ) покажет всё остальное.",
  "You are Awakened. The world is broken and Aether seeps through the cracks. Your goal is shown top-left; the Journal shows the rest.")
assert set(ru) == set(en)
write(f"{L}/ru_ru.json", ru)
write(f"{L}/en_us.json", en)
print("lang:", len(ru))
