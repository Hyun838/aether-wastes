"""Текстуры и фигурные модели блоков «Эфирных Пустошей» + лист превью."""
import json, os, sys, math, random
sys.path.insert(0, os.path.dirname(__file__))
from texlib import *
from render3d import Scene, add_block_model
from PIL import Image, ImageDraw

ROOT = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes"
NS = "aetherwastes"
TEX = {}
ANIM = {}


def save_tex(name, img, frametime=None):
    TEX[name] = img
    p = f"{ROOT}/textures/block/{name}.png"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    meta = p + ".mcmeta"
    if frametime:
        ANIM[name] = frametime
        with open(meta, "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": True}}, f)
    elif os.path.exists(meta):
        os.remove(meta)


def first_frame(img):
    w = img.width
    return img.crop((0, 0, w, w)) if img.height > w else img


# ======================= Палитры =======================
STONE = ramp("#4a4a4f", "#5d5d63", "#707077", "#83838a", "#96969c")
DEEP = ramp("#26262e", "#30303a", "#3b3b46", "#474752", "#555561")
SLATE = ramp("#2c2836", "#373242", "#433d50", "#504960", "#5f5770")
PURP = ramp("#3d1c6b", "#5a2d9a", "#7d45c9", "#a06ae6", "#c9a2ff", "#efe0ff")
CYAN = ramp("#0d4a5c", "#167a8f", "#2bb3c9", "#6ee3f0", "#d0fbff")
GOLD = ramp("#5c3d0a", "#8a5e14", "#bf8a24", "#e8b84a", "#fff0a8")
COPPER = ramp("#5a2e1a", "#7d4026", "#a5582f", "#c8733c", "#e09a5c")
BRICK = ramp("#4a2016", "#682c1c", "#8a3d25", "#a44d2e", "#bd6440")
MORTAR = ramp("#3a3533", "#57504c")
DWOOD = ramp("#2a1a10", "#3b2516", "#4e321e", "#634028", "#7a5034")
QUARTZ = ramp("#a9a39b", "#c4bfb7", "#d9d5ce", "#ebe8e2", "#faf8f4")
TEAL = ramp("#0c5c4e", "#169079", "#2cc9a6", "#84f0d6")
MAG = ramp("#4a0a36", "#7a1458", "#b0207e", "#e04aa8", "#ff9ad8", "#fff0fa")
ASH = ramp("#3e3c3a", "#4d4a48", "#5e5b58", "#706c69", "#827e7a")
SALT = ramp("#b8b2a4", "#cdc8bc", "#ddd9cf", "#ebe8e0", "#f7f5f0")
BLOOD_BARK = ramp("#2a0e0c", "#3d1512", "#541e19", "#6c2a22", "#80362c")
RESIN = ramp("#5c0a0a", "#8f1414", "#c42a1a", "#ff5a2a")


# ======================= Текстуры =======================
def stone_tex(r, seed):
    return speckle(material(r, seed), r, seed + 1, 7, 2)


def build_textures():
    # --- руды и природные блоки ---
    save_tex("ether_crystal_ore", ore(stone_tex(STONE, 1), [PURP[1], PURP[2], PURP[3], PURP[4]], 2, 4, (40, 24, 60)))
    save_tex("deepslate_ether_crystal_ore", ore(stone_tex(DEEP, 3), [PURP[1], PURP[2], PURP[3], PURP[5]], 4, 4, (20, 10, 34)))
    star = ramp("#4a5a9c", "#7a8fd6", "#c5d3ff", "#fffbe0")
    save_tex("star_iron_ore", ore(stone_tex(ramp("#232540", "#2c2f50", "#363a60", "#414670"), 5), star, 6, 5, (14, 14, 30)))
    salt = material(SALT, 7, contrast=0.8)
    for i in range(0, 16, 4):  # кристаллические трещины соли
        for k in range(16):
            if (k + i * 3) % 7 == 0:
                put(salt, (i + k // 3) % 16, k, SALT[0])
    save_tex("salt_crust", speckle(salt, SALT, 8, 6, 1))
    save_tex("ash_block", speckle(material(ASH, 9, contrast=1.2), ASH, 10, 14, 1))

    g = new()
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            put(g, x, y, PURP[4] if edge else PURP[3], 255 if edge else 70)
    for k in range(4):
        put(g, 2 + k, 2 + k, (255, 255, 255), 200)
        put(g, 10 + k // 2, 3 + k, (255, 255, 255), 140)
    for x in range(1, 15):
        put(g, x, 14, PURP[2], 255)
        put(g, 14, x, PURP[2], 255)
    save_tex("ether_glass", g)

    cap = material(ramp("#1e6b62", "#2a8f80", "#3db3a0", "#5fd6c0"), 11, contrast=0.9)
    rnd = random.Random(12)
    for _ in range(9):
        x, y = rnd.randrange(16), rnd.randrange(16)
        put(cap, x, y, (210, 255, 240))
        put(cap, (x + 1) % 16, y, (140, 240, 220))
    save_tex("glowcap_block", bevel(cap, 1.15, 0.75))

    frames = []
    for f in range(8):
        img = material(ramp("#1c2a5c", "#26387a", "#32499a", "#4560b8"), 13, contrast=0.8)
        rnd = random.Random(14)
        for b in range(3):
            x = rnd.randrange(16)
            for y in range(16):
                x = (x + rnd.choice((-1, 0, 1))) % 16
                bright = (y + f * 2 + b * 5) % 16 < 4
                put(img, x, y, (255, 255, 200) if bright else (150, 190, 255))
        frames.append(img)
    save_tex("charged_crystal", anim_strip(frames), 2)

    bark = new()
    nz = Noise(15)
    for x in range(16):
        for y in range(16):
            v = nz.smooth(x * 4, y, 4, 64)
            c = BLOOD_BARK[1 + int(v * 3)]
            if x % 5 == 0:
                c = BLOOD_BARK[0]
            put(bark, x, y, c)
    tapped = bark.copy()
    for x, y0, n in ((3, 2, 6), (8, 6, 7), (12, 1, 5)):
        for k in range(n):
            put(bark, x, y0 + k, RESIN[2] if k < n - 1 else RESIN[3])
            put(bark, x + 1, y0 + k, RESIN[1])
    save_tex("bleeding_log", bark)
    save_tex("bleeding_log_tapped", tapped)
    top = material(ramp("#5c3020", "#7a4430", "#94583e"), 16, contrast=0.6)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7:
                put(top, x, y, BLOOD_BARK[1])
            elif abs(d - 2.5) < 0.6 or abs(d - 5) < 0.6:
                put(top, x, y, ramp("#4a2416")[0])
    put(top, 7, 7, RESIN[2]); put(top, 8, 8, RESIN[2])
    save_tex("bleeding_log_top", top)

    leaves = new()
    nz = Noise(17)
    LEAF = ramp("#1f4a1c", "#2c6626", "#3d8a32", "#56a843")
    for x in range(16):
        for y in range(16):
            v = nz.fbm(x, y)
            if v < 0.32:
                continue
            put(leaves, x, y, LEAF[min(3, int(v * 4))])
    for x, y in ((3, 4), (11, 2), (7, 11), (13, 12)):
        put(leaves, x, y, (220, 130, 230)); put(leaves, x + 1, y, (180, 90, 200))
    save_tex("living_wall", leaves)

    frames = []
    for f in range(6):
        img = material(ramp("#2a0f3a", "#3d1752", "#521f6b", "#6b2a88"), 18 + f // 3, contrast=1.1)
        rnd = random.Random(19)
        for _ in range(6):
            x, y = rnd.randrange(16), rnd.randrange(16)
            k = (f + x) % 6
            put(img, x, y, PURP[3 + k % 3])
        for x in range(16):
            if (x + f) % 6 == 0:
                put(img, x, (x * 7 + f) % 16, MAG[3])
        frames.append(img)
    save_tex("aether_scar", anim_strip(frames), 6)

    # --- Якорь ---
    a = bevel(stone_tex(SLATE, 20), 1.2, 0.7)
    save_tex("anchor_stone", a)
    at = stone_tex(SLATE, 21)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 6) < 0.55:
                put(at, x, y, PURP[3])
            elif abs(d - 6) < 1.1:
                put(at, x, y, PURP[1])
    for i, (x, y) in enumerate(((7, 1), (1, 7), (13, 7), (7, 12))):
        rune(at, x - 1 if x > 7 else x, y, i, PURP[4])
    save_tex("anchor_top", bevel(at, 1.15, 0.75))
    frames = []
    for f in range(8):
        img = bevel(stone_tex(SLATE, 22), 1.1, 0.8)
        glow = PURP[3 + (1 if f in (2, 3, 4, 5) else 0) + (1 if f in (3, 4) else 0)]
        for k, y in enumerate((2, 7, 12)):
            rune(img, 6, y, k + 2, glow, PURP[1])
        for y in range(16):
            put(img, 3, y, PURP[1]); put(img, 12, y, PURP[1])
        frames.append(img)
    save_tex("anchor_rune", anim_strip(frames), 3)
    frames = []
    for f in range(10):
        img = new()
        for x in range(16):
            for y in range(16):
                band = (y + x // 2 - f) % 10
                c = PURP[2 + (2 if band < 2 else 1 if band < 4 else 0)]
                if x in (0, 15):
                    c = PURP[1]
                if x == 7 or x == 8:
                    c = PURP[min(5, PURP.index(c) + 1)]
                put(img, x, y, c)
        put(img, 3, 2 + f % 10, (255, 255, 255))
        frames.append(img)
    save_tex("anchor_crystal", anim_strip(frames), 2)
    gold = metal(16, 16, GOLD, 23, rivets=False, plates=16)
    save_tex("gold_trim", bevel(gold, 1.2, 0.7))

    # --- Скрижаль ---
    save_tex("dark_wood", planks(16, 16, DWOOD, 24))
    st = material(ramp("#3a3f4a", "#464c58", "#525966", "#5f6674"), 25, contrast=0.6)
    frame(st, (30, 32, 40), (110, 118, 132))
    for k in (5, 10):
        for t in range(2, 14):
            put(st, k, t, (40, 44, 54)); put(st, t, k, (40, 44, 54))
    for i, (x, y) in enumerate(((2, 2), (7, 2), (11, 2), (2, 7), (7, 7), (11, 7), (2, 11), (7, 11), (11, 11))):
        if i in (0, 4, 5, 8):
            rune(st, x, y, i, PURP[4])
    save_tex("slate_top", st)
    side = material(ramp("#3a3f4a", "#464c58", "#525966"), 26, contrast=0.6)
    frame(side, (30, 32, 40), (110, 118, 132))
    save_tex("slate_stone", side)
    ink = new(16, 16, (20, 20, 30, 255))
    frame(ink, (10, 10, 16), (60, 60, 90))
    for x in range(5, 11):
        for y in range(5, 11):
            put(ink, x, y, (40, 20, 80))
    save_tex("inkwell", ink)
    book = new(16, 16, (100, 30, 40, 255))
    for x in range(16):
        put(book, x, 0, (230, 220, 190)); put(book, x, 15, (60, 15, 20))
    for y in range(16):
        put(book, 0, y, (150, 120, 60))
    rune(book, 6, 6, 3, GOLD[3])
    save_tex("tome", book)

    # --- Горн ---
    save_tex("forge_bricks", bricks(16, 16, BRICK, MORTAR, 27))
    save_tex("chimney_bricks", bricks(16, 16, ramp("#2a2626", "#363131", "#433d3d", "#504949"), MORTAR, 28))
    for lit in (False, True):
        fr = bricks(16, 16, BRICK, MORTAR, 27)
        for x in range(3, 13):
            for y in range(6, 16):
                arch = y >= 8 or (x - 7.5) ** 2 / 25 + (y - 8) ** 2 / 4 <= 1
                if not arch:
                    continue
                edge = x in (3, 12) or y == 6 or (y == 7 and x in (4, 11))
                if edge:
                    put(fr, x, y, (40, 20, 14))
                elif lit:
                    t = (y - 8) / 8
                    put(fr, x, y, mix((255, 200, 60), (230, 70, 20), t + 0.15 * ((x * 3 + y) % 3)))
                else:
                    put(fr, x, y, (18, 12, 10) if (x + y) % 4 else (36, 26, 22))
        for x in range(2, 14):
            put(fr, x, 5, COPPER[3]); put(fr, x, 4, COPPER[1])
        save_tex("forge_front_on" if lit else "forge_front", fr)
    frames = []
    for f in range(6):
        coal = new()
        rnd = random.Random(29)
        for x in range(16):
            for y in range(16):
                v = rnd.random()
                hot = ((x * 7 + y * 3 + f * 5) % 11) < 3
                put(coal, x, y, (255, 150 + int(v * 80), 40) if hot and v > 0.4 else (30 + int(v * 20), 24, 22))
        frames.append(coal)
    save_tex("coals_lit", anim_strip(frames), 4)
    coal = new()
    rnd = random.Random(30)
    for x in range(16):
        for y in range(16):
            v = rnd.random()
            put(coal, x, y, (24 + int(v * 30), 22 + int(v * 24), 22 + int(v * 24)))
    save_tex("coals", coal)
    leather = material(ramp("#3d2412", "#55331a", "#6b4224", "#80522e"), 31, contrast=0.8)
    for x in range(16):
        if x % 4 == 0:
            for y in range(16):
                put(leather, x, y, (40, 24, 12))
    save_tex("leather", leather)

    # --- Ритуальные блоки ---
    pd = bevel(stone_tex(DEEP, 32), 1.25, 0.7)
    save_tex("polished_dark", pd)
    rt = stone_tex(DEEP, 33)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 7) < 0.6 or abs(d - 4.2) < 0.5:
                put(rt, x, y, GOLD[3])
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        put(rt, int(7.5 + math.cos(a) * 5.6), int(7.5 + math.sin(a) * 5.6), PURP[4])
    save_tex("ritual_circle", rt)
    bowl = stone_tex(DEEP, 34)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 4.5:
                put(bowl, x, y, (14, 12, 20))
            elif d < 5.6:
                put(bowl, x, y, GOLD[2])
    save_tex("ritual_bowl", bowl)

    # --- Рунный столб ---
    for dmg in (False, True):
        frames = []
        for f in range(1 if dmg else 6):
            img = bevel(stone_tex(DEEP, 35), 1.2, 0.75)
            for y in range(16):
                for x in (7, 8):
                    if dmg:
                        put(img, x, y, (60, 50, 50) if y % 3 else (90, 40, 40))
                    else:
                        b = (y + f * 3) % 16
                        put(img, x, y, CYAN[4] if b < 2 else CYAN[3] if b < 5 else CYAN[2])
            if dmg:
                for x, y in ((3, 2), (4, 3), (4, 4), (5, 5), (11, 9), (10, 10), (10, 11), (9, 12), (12, 3), (13, 4)):
                    put(img, x, y, (12, 10, 14))
            frames.append(img)
        save_tex("pylon_damaged" if dmg else "pylon", anim_strip(frames) if len(frames) > 1 else frames[0], None if dmg else 3)
    cr = new()
    for x in range(16):
        for y in range(16):
            put(cr, x, y, CYAN[3] if (x + y) % 5 else CYAN[4])
    frame(cr, CYAN[1], CYAN[4])
    save_tex("ward_crystal", cr)

    # --- Обелиск ---
    q = bevel(material(QUARTZ, 36, contrast=0.6), 1.05, 0.85)
    for y in range(16):
        put(q, 7, y, TEAL[2]); put(q, 8, y, TEAL[3])
    save_tex("obelisk", q)
    save_tex("quartz_base", bevel(material(QUARTZ, 37, contrast=0.5), 1.05, 0.8))
    tc = new()
    for x in range(16):
        for y in range(16):
            put(tc, x, y, TEAL[3] if (x * 2 + y) % 7 else (230, 255, 250))
    frame(tc, TEAL[1], TEAL[3])
    save_tex("teal_crystal", tc)

    # --- Обсерватория ---
    save_tex("copper_plate", bevel(metal(16, 16, COPPER, 38), 1.1, 0.8))
    brass = metal(16, 16, GOLD, 39, rivets=False, plates=4)
    save_tex("brass_tube", brass)
    lens = new(16, 16, GOLD[2] + (255,))
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6:
                put(lens, x, y, mix((40, 70, 140), (170, 220, 255), max(0, 1 - d / 6)))
    put(lens, 5, 5, (255, 255, 255))
    save_tex("lens", lens)

    # --- Сердце Раскола, порталы, руна ---
    frames = []
    for f in range(12):
        img = new()
        for x in range(16):
            for y in range(16):
                v = (math.sin((x + f) * 0.7) + math.cos((y - f) * 0.6) + math.sin((x + y + f * 2) * 0.4)) / 3
                put(img, x, y, MAG[max(0, min(5, int((v + 1) * 3)))])
        frame(img, MAG[0], MAG[4])
        frames.append(img)
    save_tex("heart_core", anim_strip(frames), 2)
    frames = []
    for f in range(16):
        img = new()
        for x in range(16):
            for y in range(16):
                ang = math.atan2(y - 7.5, x - 7.5)
                d = math.hypot(x - 7.5, y - 7.5)
                v = math.sin(ang * 3 + d * 0.9 - f * 0.4)
                a = max(0, min(255, int(255 - d * 22)))
                if a < 40:
                    continue
                c = mix(PURP[1], MAG[4], (v + 1) / 2)
                put(img, x, y, c, a if d > 3 else 255)
        frames.append(img)
    save_tex("rift", anim_strip(frames), 2)
    frames = []
    for f in range(8):
        img = bevel(stone_tex(ramp("#1a0f26", "#24163a", "#2e1d4a", "#3a2560"), 40), 1.2, 0.7)
        for x in range(2, 14):
            for y in range(2, 14):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 5:
                    v = math.sin(d * 1.3 - f * 0.8)
                    put(img, x, y, mix(PURP[1], PURP[4], (v + 1) / 2))
        frames.append(img)
    save_tex("underside_portal", anim_strip(frames), 2)
    frames = []
    for f in range(8):
        img = new()
        k = 0.6 + 0.4 * math.sin(f / 8 * 2 * math.pi)
        for x in range(1, 15):
            for y in range(1, 15):
                d = math.hypot(x - 7.5, y - 7.5)
                if abs(d - 6) < 0.6 or abs(d - 3) < 0.5 or (abs(x - y) < 1 and d < 6) or (abs(x + y - 15) < 1 and d < 6):
                    put(img, x, y, mix(PURP[2], PURP[5], k), 230)
        frames.append(img)
    save_tex("rune_trap", anim_strip(frames), 2)


# ======================= Модели =======================
MODELS = {}
FACES6 = ["up", "down", "north", "south", "east", "west"]


def el(f, t, tex, rot=None, faces=None, per_face=None, shade=True):
    faces = faces or FACES6
    d = {"from": list(f), "to": list(t), "faces": {}}
    for fc in faces:
        tx = (per_face or {}).get(fc, tex)
        d["faces"][fc] = {"texture": "#" + tx}
        if fc == "down" and f[1] == 0:
            d["faces"][fc]["cullface"] = "down"
    if rot:
        d["rotation"] = {"origin": list(rot[0]), "axis": rot[1], "angle": rot[2]}
    if not shade:
        d["shade"] = False
    return d


def model(name, elements, textures, particle, render=None):
    m = {"parent": "minecraft:block/block", "textures": {k: f"{NS}:block/{v}" for k, v in textures.items()}, "elements": elements}
    m["textures"]["particle"] = f"{NS}:block/{particle}"
    if render:
        m["render_type"] = render
    MODELS[name] = m


def build_models():
    # --- Якорь, 5 уровней ---
    for tier in range(1, 6):
        e = [
            el((0, 0, 0), (16, 3, 16), "s", per_face={"up": "top"}),
            el((2, 3, 2), (14, 5, 14), "s"),
            el((5, 5, 5), (11, 12, 11), "r", per_face={"up": "s", "down": "s"}),
            el((4, 12, 4), (12, 13, 12), "g"),
        ]
        for (x, z) in ((4, 4), (11, 4), (4, 11), (11, 11)):
            e.append(el((x, 13, z), (x + 1, 15, z + 1), "g"))
        h = 3 + tier
        e.append(el((6.5, 14, 6.5), (9.5, 14 + h, 9.5), "c", rot=((8, 14, 8), "y", 45), shade=False))
        if tier >= 2:
            e.append(el((7, 14 + h, 7), (9, 15 + h, 9), "c", rot=((8, 14, 8), "y", 45), shade=False))
        if tier >= 3:
            for (x, z) in ((1, 7.5), (14, 7.5)):
                e.append(el((x, 15, z), (x + 1, 18, z + 1), "c", rot=((x + 0.5, 15, 8), "z", 22.5 if x < 8 else -22.5), shade=False))
        if tier >= 4:
            for (x, z) in ((7.5, 1), (7.5, 14)):
                e.append(el((x, 15, z), (x + 1, 18, z + 1), "c", rot=((8, 15, z + 0.5), "x", 22.5 if z > 8 else -22.5), shade=False))
        if tier >= 5:
            for (f, t) in (((3, 19, 3), (13, 20, 4)), ((3, 19, 12), (13, 20, 13)), ((3, 19, 4), (4, 20, 12)), ((12, 19, 4), (13, 20, 12))):
                e.append(el(f, t, "g"))
        model(f"anchor_t{tier}", e, {"s": "anchor_stone", "top": "anchor_top", "r": "anchor_rune", "g": "gold_trim", "c": "anchor_crystal"},
              "anchor_stone", "minecraft:cutout")

    # --- Скрижаль ---
    e = [el((0, 12, 0), (16, 15, 16), "w", per_face={"up": "top", "down": "w"})]
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        e.append(el((x, 0, z), (x + 2, 12, z + 2), "w"))
    e.append(el((2, 4, 2), (14, 5, 14), "w"))
    e.append(el((11, 15, 2), (13, 17, 4), "ink"))
    e.append(el((2, 15, 10), (7, 16, 14), "book"))
    model("slate", e, {"w": "dark_wood", "top": "slate_top", "ink": "inkwell", "book": "tome"}, "slate_top")

    # --- Горн (перед на север) ---
    for lit in (False, True):
        e = [
            el((0, 0, 0), (16, 11, 16), "b", per_face={"north": "front", "up": "b"}),
            el((1, 11, 1), (15, 12, 9), "coal", faces=["up", "north", "east", "west"]),
            el((10, 11, 10), (15, 16, 15), "ch"),
            el((9, 15, 9), (16, 16, 16), "ch"),
            el((1, 11, 10), (8, 13, 15), "lea"),
        ]
        model("forge_hearth_on" if lit else "forge_hearth", e,
              {"b": "forge_bricks", "front": "forge_front_on" if lit else "forge_front", "coal": "coals_lit" if lit else "coals",
               "ch": "chimney_bricks", "lea": "leather"}, "forge_bricks")

    # --- Ритуальный постамент и фокус ---
    model("ritual_pedestal", [
        el((2, 0, 2), (14, 2, 14), "d"),
        el((5, 2, 5), (11, 10, 11), "d"),
        el((4, 9, 4), (12, 10, 12), "g"),
        el((3, 10, 3), (13, 13, 13), "d", per_face={"up": "bowl"}),
    ], {"d": "polished_dark", "g": "gold_trim", "bowl": "ritual_bowl"}, "polished_dark")
    e = [
        el((0, 0, 0), (16, 2, 16), "d", per_face={"up": "circle"}),
        el((3, 2, 3), (13, 3, 13), "g"),
        el((5, 3, 5), (11, 9, 11), "d"),
        el((4, 9, 4), (12, 10, 12), "g", per_face={"up": "bowl"}),
    ]
    for (x, z) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        e.append(el((x, 2, z), (x + 1, 6, z + 1), "c", rot=((x + 0.5, 2, z + 0.5), "y", 45), shade=False))
    model("ritual_focus", e, {"d": "polished_dark", "g": "gold_trim", "circle": "ritual_circle", "bowl": "ritual_bowl", "c": "anchor_crystal"},
          "polished_dark", "minecraft:cutout")

    # --- Рунный столб ---
    model("ward_pylon", [
        el((3, 0, 3), (13, 2, 13), "d"),
        el((5, 2, 5), (11, 13, 11), "p", per_face={"up": "d", "down": "d"}),
        el((6, 13, 6), (10, 16, 10), "d"),
        el((7, 16, 7), (9, 21, 9), "c", rot=((8, 16, 8), "y", 45), shade=False),
    ], {"d": "polished_dark", "p": "pylon", "c": "ward_crystal"}, "pylon", "minecraft:cutout")
    model("ward_pylon_damaged", [
        el((3, 0, 3), (13, 2, 13), "d"),
        el((5, 2, 5), (11, 11, 11), "p", per_face={"up": "d", "down": "d"}),
        el((6, 11, 6), (10, 14, 10), "d", rot=((8, 11, 8), "z", 22.5)),
        el((10, 2, 2), (12, 4, 4), "c", rot=((11, 2, 3), "y", 22.5)),
    ], {"d": "polished_dark", "p": "pylon_damaged", "c": "ward_crystal"}, "pylon_damaged", "minecraft:cutout")

    # --- Обелиск ---
    model("purifying_obelisk", [
        el((2, 0, 2), (14, 3, 14), "q"),
        el((4, 3, 4), (12, 7, 12), "q"),
        el((5, 7, 5), (11, 22, 11), "o", per_face={"up": "q", "down": "q"}),
        el((6, 22, 6), (10, 26, 10), "q", rot=((8, 22, 8), "y", 45)),
        el((7, 27, 7), (9, 31, 9), "c", rot=((8, 27, 8), "y", 45), shade=False),
    ], {"q": "quartz_base", "o": "obelisk", "c": "teal_crystal"}, "obelisk", "minecraft:cutout")

    # --- Обсерватория ---
    model("observatory", [
        el((1, 0, 1), (15, 6, 15), "cu"),
        el((0, 6, 0), (16, 7, 16), "cu"),
        el((6, 7, 6), (10, 11, 10), "br"),
        el((6.5, 10, 1), (9.5, 13, 15), "br", rot=((8, 11.5, 8), "x", -22.5), per_face={"north": "lens"}),
        el((6, 9.5, 0), (10, 13.5, 2), "br", rot=((8, 11.5, 8), "x", -22.5), per_face={"north": "lens"}),
    ], {"cu": "copper_plate", "br": "brass_tube", "lens": "lens"}, "copper_plate")

    # --- Сердце Раскола ---
    e = [
        el((1, 0, 1), (15, 2, 15), "d", per_face={"up": "circle"}),
        el((4, 5, 4), (12, 13, 12), "h", rot=((8, 9, 8), "y", 45), shade=False),
        el((5, 4, 5), (11, 14, 11), "h", rot=((8, 9, 8), "x", 45), shade=False),
    ]
    for (x, z) in ((1, 1), (13, 1), (1, 13), (13, 13)):
        e.append(el((x, 2, z), (x + 2, 12, z + 2), "d"))
        e.append(el((x, 12, z), (x + 2, 13, z + 2), "g"))
    model("heart_of_rift", e, {"d": "polished_dark", "h": "heart_core", "g": "gold_trim", "circle": "ritual_circle"},
          "heart_core", "minecraft:cutout")


SIMPLE_CUBES = {
    "ether_crystal_ore": "ether_crystal_ore", "deepslate_ether_crystal_ore": "deepslate_ether_crystal_ore",
    "star_iron_ore": "star_iron_ore", "salt_crust": "salt_crust", "ash_block": "ash_block",
    "glowcap_block": "glowcap_block", "charged_crystal": "charged_crystal", "aether_scar": "aether_scar",
    "underside_portal": "underside_portal", "journal_archive": None,
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def write_all():
    # Сначала удалить старые модели блоков, которые больше не нужны.
    mdir = f"{ROOT}/models/block"
    for f in os.listdir(mdir):
        os.remove(os.path.join(mdir, f))
    for name, m in MODELS.items():
        write_json(f"{mdir}/{name}.json", m)
    for b, t in SIMPLE_CUBES.items():
        if t:
            write_json(f"{mdir}/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{t}"}})
    write_json(f"{mdir}/ether_glass.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                            "textures": {"all": f"{NS}:block/ether_glass"}})
    write_json(f"{mdir}/living_wall.json", {"parent": "minecraft:block/leaves", "render_type": "minecraft:cutout_mipped",
                                            "textures": {"all": f"{NS}:block/living_wall"}})
    for nm in ("bleeding_log", "bleeding_log_tapped"):
        write_json(f"{mdir}/{nm}.json", {"parent": "minecraft:block/cube_column",
                                         "textures": {"end": f"{NS}:block/bleeding_log_top", "side": f"{NS}:block/{nm}"}})
        write_json(f"{mdir}/{nm}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                    "textures": {"end": f"{NS}:block/bleeding_log_top", "side": f"{NS}:block/{nm}"}})
    write_json(f"{mdir}/journal_archive.json", {"parent": "minecraft:block/cube_column",
                                                "textures": {"end": f"{NS}:block/dark_wood", "side": f"{NS}:block/journal_archive"}})
    write_json(f"{mdir}/rift_portal.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:translucent",
                                            "textures": {"cross": f"{NS}:block/rift", "particle": f"{NS}:block/rift"}})
    write_json(f"{mdir}/rune_trap.json", {"parent": "minecraft:block/carpet", "render_type": "minecraft:translucent",
                                          "textures": {"wool": f"{NS}:block/rune_trap", "particle": f"{NS}:block/rune_trap"}})

    bs = f"{ROOT}/blockstates"
    write_json(f"{bs}/anchor.json", {"variants": {f"tier={t}": {"model": f"{NS}:block/anchor_t{t}"} for t in range(1, 6)}})
    v = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ("false", "true"):
            m = {"model": f"{NS}:block/forge_hearth{'_on' if lit == 'true' else ''}"}
            if y:
                m["y"] = y
            v[f"facing={facing},lit={lit}"] = m
    write_json(f"{bs}/forge_hearth.json", {"variants": v})
    write_json(f"{bs}/ward_pylon.json", {"variants": {"damaged=false": {"model": f"{NS}:block/ward_pylon"},
                                                      "damaged=true": {"model": f"{NS}:block/ward_pylon_damaged"}}})

    im = f"{ROOT}/models/item"
    for item, mdl in (("anchor", "anchor_t3"), ("forge_hearth", "forge_hearth_on"), ("ward_pylon", "ward_pylon")):
        write_json(f"{im}/{item}.json", {"parent": f"{NS}:block/{mdl}"})


def journal_tex():
    img = planks(16, 16, DWOOD, 41)
    rnd = random.Random(42)
    cols = [(120, 30, 40), (40, 70, 120), (40, 100, 50), (140, 110, 40), (80, 40, 110)]
    for shelf in (1, 9):
        x = 1
        while x < 15:
            w = rnd.choice((1, 2))
            c = rnd.choice(cols)
            hgt = rnd.randint(5, 6)
            for xx in range(x, min(15, x + w)):
                for y in range(shelf + 6 - hgt, shelf + 6):
                    put(img, xx, y, shade(c, 1.15 if xx == x else 0.9))
                put(img, xx, shelf + 6 - hgt + 1, GOLD[3])
            x += w
    for y in (0, 7, 8, 15):
        for x in range(16):
            put(img, x, y, DWOOD[0])
    save_tex("journal_archive", img)


def preview():
    textures = {k: first_frame(v) for k, v in TEX.items()}
    names = ["anchor_t1", "anchor_t3", "anchor_t5", "slate", "forge_hearth", "forge_hearth_on", "ritual_focus",
             "ritual_pedestal", "ward_pylon", "ward_pylon_damaged", "purifying_obelisk", "observatory", "heart_of_rift"]
    cubes = ["ether_crystal_ore", "deepslate_ether_crystal_ore", "star_iron_ore", "salt_crust", "ash_block",
             "glowcap_block", "charged_crystal", "aether_scar", "underside_portal", "ether_glass", "living_wall"]
    cell = 200
    cols = 6
    allm = names + cubes + ["journal_archive", "bleeding_log"]
    rows = (len(allm) + cols - 1) // cols
    sheet = Image.new("RGBA", (cell * cols, cell * rows), (52, 56, 64, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(allm):
        if n in MODELS:
            m = MODELS[n]
        elif n == "journal_archive" or n == "bleeding_log":
            side = "journal_archive" if n == "journal_archive" else "bleeding_log"
            end = "dark_wood" if n == "journal_archive" else "bleeding_log_top"
            m = {"textures": {"s": side, "e": end}, "elements": [el((0, 0, 0), (16, 16, 16), "s", per_face={"up": "e", "down": "e"})]}
        else:
            m = {"textures": {"a": n}, "elements": [el((0, 0, 0), (16, 16, 16), "a")]}
        tm = {}
        for k, v in m["textures"].items():
            tm[k] = v.split("/")[-1] if "/" in v else v
        mm = dict(m)
        mm["textures"] = tm
        s = Scene()
        add_block_model(s, mm, textures)
        img = s.render(cell - 20, yaw=-45, pitch=28)
        sheet.alpha_composite(img, ((i % cols) * cell + 10, (i // cols) * cell + 6))
        d.text(((i % cols) * cell + 8, (i // cols) * cell + cell - 16), n, fill=(230, 230, 230, 255))
    sheet.save(os.path.join(os.path.dirname(__file__), "blocks_preview.png"))


if __name__ == "__main__":
    build_textures()
    journal_tex()
    build_models()
    # удалить старые текстуры блоков, которых больше нет
    tdir = f"{ROOT}/textures/block"
    for f in os.listdir(tdir):
        base = f.split(".")[0]
        if base not in TEX:
            os.remove(os.path.join(tdir, f))
    write_all()
    preview()
    print("blocks ok", len(TEX), "textures", len(MODELS), "models")
