"""Графика снаряжения: иконки брони/инструментов (16×16), уникальное оружие и лук (32×32),
текстуры брони на модели (64×32), модели предметов и превью брони на манекене."""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *
from render3d import Scene, add_entity_part
from PIL import Image, ImageDraw

RES = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes"
PREV = "/tmp/claude-0/-home-claude/6e7c1073-f517-50dd-916e-c96eb0f32376/scratchpad/art"
OUTLINE = (24, 16, 30)

# ---------------- Палитры материалов ----------------
MATS = {
    "ashen": {"r": ramp("#1e1c1a", "#33302c", "#4a4540", "#615b54", "#7a736a"), "a": ramp("#a83a10", "#ff7a2a")},
    "ether_steel": {"r": ramp("#2a2f3c", "#454c5e", "#66708a", "#8e98b0", "#c4cce0"), "a": ramp("#7d45c9", "#d6b8ff")},
    "prism": {"r": ramp("#3a6f80", "#5fa0b4", "#8fd0e0", "#c4f0f8", "#f4ffff"), "a": ramp("#7d45c9", "#ffffff")},
    "star_iron": {"r": ramp("#141a36", "#222c58", "#34437e", "#4d60a6", "#7a8ed0"), "a": ramp("#bf8a24", "#fff0a8")},
    "archivist": {"r": ramp("#1a0a2a", "#2c1446", "#42205e", "#5a2e7a", "#744096"), "a": ramp("#bf8a24", "#ffe080")},
    "phantom": {"r": ramp("#081014", "#122228", "#1f3a44", "#3a6672", "#9adce6"), "a": ramp("#22c8f0", "#e6ffff")},
}
HANDLE = {"h": hexc("#3b2516"), "H": hexc("#6e4a2c")}


def mask_icon(rows, r, a):
    pal = {str(i + 1): r[i] for i in range(5)}
    pal.update({"a": a[0], "A": a[1]})
    pal.update(HANDLE)
    return outline_sprite(sprite(rows, pal), OUTLINE)


ARMOR_MASKS = {
    "helmet": [
        "................",
        "................",
        "................",
        "....22222222....",
        "...2455555542...",
        "..245444444542..",
        "..244444444442..",
        "..243aAAAAa342..",
        "..241111111142..",
        "..243......342..",
        "..232......232..",
        "..22........22..",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..222......222..",
        ".24552....25542.",
        ".244542222454421",
        ".2443455554344.1",
        "..1243444434421.",
        "...12344a4321...",
        "...1234aA4321...",
        "...123444a321...",
        "...12344443 21..",
        "...12333333321..",
        "...12222222221..",
        "...1111111111...",
        "................",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "................",
        "...2222222222...",
        "...2455aA5542...",
        "...2444aa4442...",
        "...244421 442...",
        "...24432..4432..",
        "...24432..4432..",
        "...24432..4432..",
        "...23421..2432..",
        "...23421..2432..",
        "...22221..2222..",
        "...11111..1111..",
        "................",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...2222..2222...",
        "...2442..2442...",
        "...2442..2442...",
        "...2aA2..2Aa2...",
        "..24442..24442..",
        "..244431.244431.",
        "..2333321233332.",
        "..1111111111111.",
        "................",
        "................",
    ],
}
ARMOR_MASKS = {k: [row.replace(" ", ".") for row in v] for k, v in ARMOR_MASKS.items()}

TOOL_MASKS = {
    "sword": [
        "..............22",
        ".............254",
        "............2541",
        "...........2541.",
        "..........2541..",
        ".........2541...",
        "........2541....",
        ".......2541.....",
        "..a...2541......",
        "...a.2541.......",
        "....aA41........",
        "....HaA.........",
        "...HH.a.........",
        "..hH...a........",
        ".ah.............",
        "................",
    ],
    "pickaxe": [
        "................",
        "....222222......",
        "...25555542.....",
        "..2544444442....",
        ".2541....H4442..",
        ".241....H.14542.",
        ".21....H....1442",
        ".......H.....142",
        "......H......12.",
        ".....H..........",
        "....H...........",
        "...H............",
        "..H.............",
        ".h..............",
        "................",
        "................",
    ],
    "axe": [
        "................",
        ".......222......",
        "......25542.....",
        ".....2544442....",
        "....2544H442....",
        "....2441H32.....",
        ".....21H.2......",
        "......H.........",
        ".....H..........",
        "....H...........",
        "...H............",
        "..H.............",
        ".h..............",
        "................",
        "................",
        "................",
    ],
    "shovel": [
        "................",
        "..........222...",
        ".........25542..",
        "........254442..",
        "........24442...",
        ".........2a2....",
        "........H.......",
        ".......H........",
        "......H.........",
        ".....H..........",
        "....H...........",
        "...H............",
        "..H.............",
        ".h..............",
        "................",
        "................",
    ],
}


# ---------------- Оружие 32×32 из многоугольников ----------------
def shaded_parts(parts, size=32):
    """parts: [(polygon_points, ramp, highlight_line|None)]. Возвращает изображение с объёмом и контуром."""
    img = new(size, size)
    for poly, r, hl in parts:
        m = Image.new("L", (size, size), 0)
        ImageDraw.Draw(m).polygon(poly, fill=255)
        px = m.load()
        for x in range(size):
            for y in range(size):
                if px[x, y] == 0:
                    continue
                ul = px[x - 1, y - 1] if x > 0 and y > 0 else 0
                lr = px[x + 1, y + 1] if x < size - 1 and y < size - 1 else 0
                c = r[2]
                if ul == 0:
                    c = r[3]
                elif lr == 0:
                    c = r[1]
                if (x * 7 + y * 3) % 11 == 0 and c == r[2]:
                    c = r[1] if len(r) > 1 else c
                put(img, x, y, c)
        if hl:
            d = ImageDraw.Draw(img)
            d.line(hl, fill=tuple(r[-1]) + (255,), width=1)
    return outline_sprite(img, OUTLINE)


def blade(x0, y0, x1, y1, w):
    """Клинок от (x0,y0) острием к (x1,y1), полуширина w."""
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    nx, ny = -dy / L * w, dx / L * w
    tipx, tipy = x1, y1
    bx, by = x1 - dx / L * w * 2.2, y1 - dy / L * w * 2.2
    return [(x0 + nx, y0 + ny), (bx + nx, by + ny), (tipx, tipy), (bx - nx, by - ny), (x0 - nx, y0 - ny)]


def bar(x0, y0, x1, y1, w):
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    nx, ny = -dy / L * w, dx / L * w
    return [(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)]


def circle(cx, cy, r, n=10):
    return [(cx + math.cos(a) * r, cy + math.sin(a) * r) for a in [i * 2 * math.pi / n for i in range(n)]]


WOOD = ramp("#2a1a10", "#4e321e", "#6e4a2c", "#8f6640", "#a8804e")
LEATHER = ramp("#2a160c", "#4a2a16", "#6a3e22", "#8a5430", "#a06a40")
GOLDR = ramp("#5c3d0a", "#8a5e14", "#bf8a24", "#e8b84a", "#fff0a8")
STEEL = MATS["ether_steel"]["r"]
STAR = MATS["star_iron"]["r"]
PURP = ramp("#3d1c6b", "#5a2d9a", "#7d45c9", "#a06ae6", "#efe0ff")
SALT = ramp("#8a8478", "#b8b2a4", "#ddd9cf", "#f4f2ec", "#ffffff")
EMBER = ramp("#3a0e04", "#8a2a08", "#e05a10", "#ff9a30", "#fff0a0")
DARKIRON = ramp("#1a1a1e", "#2c2c32", "#40404a", "#585862", "#787884")
PRISMR = MATS["prism"]["r"]
FIRE = ramp("#8a2a00", "#ff6a00", "#ffb020", "#fff0a0", "#ffffff")
STONE = ramp("#2e2e34", "#45454e", "#5e5e68", "#7a7a84", "#a0a0aa")
GREEN = ramp("#1a5a3a", "#2ea060", "#6ae0a0", "#d0ffe8", "#ffffff")
VOID = ramp("#120a24", "#2c1a50", "#4c2e80", "#8a45ff", "#f4e8ff")
HEART = ramp("#4a0a36", "#7a1458", "#b0207e", "#e04aa8", "#ffd0f0")


def weapons():
    W = {}
    W["ether_blade"] = shaded_parts([
        (bar(5, 27, 9, 23, 1.6), LEATHER, None),
        (circle(4.5, 27.5, 1.8, 8), GOLDR, None),
        (bar(6, 20, 12, 26, 1.4), GOLDR, None),
        (blade(9, 23, 28, 4, 2.6), STEEL, [(11, 21), (26, 6)]),
        (circle(9, 23, 1.4, 6), PURP, None),
    ])
    W["salt_scythe"] = shaded_parts([
        (bar(4, 29, 22, 5, 1.2), WOOD, None),
        ([(20, 6), (24, 4), (29, 8), (30, 14), (27, 20), (26, 13), (22, 9)], SALT, [(24, 5), (29, 11)]),
        (circle(21.5, 6, 1.6, 8), STEEL, None),
    ])
    W["ember_greataxe"] = shaded_parts([
        (bar(4, 29, 23, 6, 1.4), WOOD, None),
        ([(17, 5), (24, 2), (30, 6), (30, 15), (25, 18), (21, 11)], DARKIRON, [(24, 3), (30, 8)]),
        ([(22, 10), (27, 12), (25, 17)], EMBER, None),
        (bar(19, 8, 23, 11, 1.2), EMBER, None),
    ])
    W["prism_dagger"] = shaded_parts([
        (bar(6, 26, 10, 22, 1.5), LEATHER, None),
        (bar(8, 19, 13, 24, 1.1), STEEL, None),
        (blade(10, 22, 24, 8, 2.2), PRISMR, [(12, 20), (22, 10)]),
        (circle(5.5, 26.5, 1.6, 6), PURP, None),
    ])
    W["star_longsword"] = shaded_parts([
        (bar(3, 29, 8, 24, 1.5), LEATHER, None),
        (circle(3, 29, 1.8, 8), GOLDR, None),
        (bar(4, 20, 12, 28, 1.5), GOLDR, None),
        (blade(8, 24, 30, 2, 2.4), STAR, [(10, 22), (28, 4)]),
        (circle(17, 15, 1.0, 6), GOLDR, None),
        (circle(23, 9, 0.9, 6), GOLDR, None),
    ])
    W["spark_scepter"] = shaded_parts([
        (bar(4, 29, 20, 13, 1.3), DARKIRON, None),
        (bar(16, 13, 23, 20, 1.2), GOLDR, None),
        (circle(23.5, 9.5, 4.2, 10), FIRE, None),
        ([(23, 2), (25, 6), (21, 6)], FIRE, None),
        ([(29, 7), (27, 11), (26, 6)], FIRE, None),
    ])
    W["resonance_maul"] = shaded_parts([
        (bar(4, 29, 19, 14, 1.4), WOOD, None),
        ([(14, 6), (22, 2), (30, 10), (22, 18)], STONE, [(15, 7), (22, 3)]),
        ([(19, 8), (23, 6), (25, 10), (21, 12)], GREEN, None),
        (bar(13, 12, 16, 15, 1.6), GOLDR, None),
    ])
    W["convergence_glaive"] = shaded_parts([
        (bar(3, 30, 22, 11, 1.1), DARKIRON, None),
        (blade(20, 13, 30, 2, 2.8), VOID, [(22, 11), (28, 4)]),
        ([(19, 8), (17, 3), (22, 9)], VOID, None),
        (circle(20.5, 12.5, 1.8, 8), ramp("#8a45ff", "#c9a2ff", "#efe0ff", "#ffffff"), None),
        (circle(3, 30, 1.4, 6), VOID, None),
    ])
    W["heart_blade"] = shaded_parts([
        (bar(4, 28, 9, 23, 1.6), LEATHER, None),
        (circle(3.5, 28.5, 2.0, 8), HEART, None),
        (bar(4, 20, 13, 29, 1.6), GOLDR, None),
        (blade(9, 23, 29, 3, 3.0), HEART, [(11, 21), (27, 5)]),
        (circle(9, 23, 1.6, 8), ramp("#ff3aa8", "#ff9ad8", "#fff0fa", "#ffffff"), None),
    ])
    GHOST = MATS["phantom"]["r"]
    CYAN = ramp("#0a6a8a", "#30c8f0", "#9af0ff", "#f0ffff", "#ffffff")
    ARCANE = ramp("#2a1c5a", "#4a2e9a", "#8a5aff", "#d0b0ff", "#ffffff")
    W["echo_reaper"] = shaded_parts([
        (bar(4, 30, 21, 6, 1.1), DARKIRON, None),
        ([(18, 7), (22, 2), (29, 3), (31, 9), (29, 16), (26, 10), (21, 9)], GHOST, [(23, 3), (30, 7)]),
        ([(24, 6), (28, 6), (27, 11)], CYAN, None),
        (circle(20, 7.5, 1.7, 8), CYAN, None),
        (circle(4, 30, 1.4, 6), CYAN, None),
        (bar(9, 22, 12, 19, 1.5), GHOST, None),
    ])
    W["chronicle_blade"] = shaded_parts([
        (bar(4, 28, 9, 23, 1.6), LEATHER, None),
        (circle(3.5, 28.5, 1.9, 8), GOLDR, None),
        ([(5, 19), (13, 27), (14, 24), (8, 18)], GOLDR, None),
        (blade(9, 23, 29, 3, 2.7), ARCANE, [(11, 21), (27, 5)]),
        (circle(15, 17, 1.1, 6), GOLDR, None),
        (circle(21, 11, 1.1, 6), GOLDR, None),
        (circle(9.5, 22.5, 1.6, 8), ramp("#8a45ff", "#c9a2ff", "#efe0ff", "#ffffff"), None),
    ])
    W["colossus_hammer"] = shaded_parts([
        (bar(3, 30, 18, 15, 1.5), DARKIRON, None),
        ([(11, 9), (19, 1), (31, 13), (23, 21)], ramp("#141212", "#221e1c", "#332d2a", "#463e3a", "#5a504a"), [(12, 9), (19, 2)]),
        ([(16, 9), (20, 6), (26, 12), (22, 15)], EMBER, None),
        ([(13, 13), (15, 12), (17, 17), (15, 18)], EMBER, None),
        (bar(14, 15, 17, 18, 1.7), GOLDR, None),
        (circle(3, 30, 1.5, 6), EMBER, None),
    ])
    return W


def bow_frames():
    frames = []
    for k, pull in enumerate((0.0, 0.35, 0.65, 0.95)):
        img = new(32, 32)
        d = ImageDraw.Draw(img)
        # дуга лука (снизу-слева вверх-вправо), тетива натягивается к низу-слева
        bend = 4 + pull * 3
        pts = []
        for i in range(21):
            t = i / 20
            x = 6 + 20 * t
            y = 26 - 20 * t
            off = math.sin(t * math.pi) * bend
            pts.append((x + off * 0.7, y + off * 0.7))
        for i in range(len(pts) - 1):
            d.line([pts[i], pts[i + 1]], fill=tuple(WOOD[2]) + (255,), width=3)
            d.line([pts[i], pts[i + 1]], fill=tuple(WOOD[3]) + (255,), width=1)
        for (x, y) in (pts[0], pts[-1]):
            d.ellipse([x - 1.6, y - 1.6, x + 1.6, y + 1.6], fill=tuple(PURP[3]) + (255,))
        mid = pts[10]
        d.ellipse([mid[0] - 1.8, mid[1] - 1.8, mid[0] + 1.8, mid[1] + 1.8], fill=tuple(PURP[2]) + (255,))
        sx = 6 + (pts[10][0] - 16) * 0 - pull * 6
        sy = 26 - 10 + pull * 6
        nock = (16 - pull * 7, 16 + pull * 7)
        d.line([pts[0], nock, pts[-1]], fill=(230, 230, 240, 255), width=1)
        if pull > 0:
            tip = (nock[0] + 13, nock[1] - 13)
            d.line([nock, tip], fill=tuple(WOOD[3]) + (255,), width=1)
            d.polygon([(tip[0] + 2, tip[1] - 2), (tip[0] - 2, tip[1]), (tip[0], tip[1] + 2)], fill=tuple(PURP[4]) + (255,))
        frames.append(outline_sprite(img, OUTLINE))
    return frames


def materials():
    M = {}
    M["ether_steel_ingot"] = outline_sprite(sprite([
        "................", "................", "................", "................",
        "......4444444...", ".....45444443...", "....4444444432..", "...44444a444322..",
        "..3333333332221.", "..333a333322211.", "..33333333221...", "..2222222221....",
        "...111111111....", "................", "................", "................",
    ], {"1": STEEL[0], "2": STEEL[1], "3": STEEL[2], "4": STEEL[3], "5": STEEL[4], "a": PURP[3]}), OUTLINE)
    M["prism_shard"] = outline_sprite(sprite([
        "................", "........5.......", ".......545......", "......54435.....",
        "......544435....", ".....5444335....", ".....54a4335....", "....5444a3335...",
        "....544443335...", "....44443333....", ".....4333332....", "......33322.....",
        ".......222......", "................", "................", "................",
    ], {"2": PRISMR[0], "3": PRISMR[1], "4": PRISMR[2], "5": PRISMR[4], "a": (255, 255, 255)}), OUTLINE)
    FUR = MATS["ashen"]["r"]
    M["ash_pelt"] = outline_sprite(sprite([
        "................", "................", "...33......33...", "...343....343...",
        "....34444443....", "...3444a444443..", "..34444444a443..", "..3444a4444443..",
        "..34444444444...", "...344a444443...", "....3444444a3...", "...33444444433..",
        "..33..3333..33..", "................", "................", "................",
    ], {"3": FUR[1], "4": FUR[3], "a": EMBER[3]}), OUTLINE)
    M["heartwood"] = outline_sprite(sprite([
        "................", "................", "....22222222....", "...2333333332...",
        "..233r444r3332..", "..23r44rr44r32..", "..234r4rr4r432..", "..2344r44r4432..",
        "..234r4rr4r432..", "..23r44rr44r32..", "..233r444r3332..", "...2333333332...",
        "....22222222....", "................", "................", "................",
    ], {"2": hexc("#2a0e0c"), "3": hexc("#541e19"), "4": hexc("#94583e"), "r": hexc("#c42a1a")}), OUTLINE)
    G = MATS["phantom"]["r"]
    M["phantom_essence"] = outline_sprite(sprite([
        "................", ".......44.......", "......4554......", ".....455554.....",
        "....45566554....", "....45677654....", "...4567cc7654...", "...456cWWc654...",
        "...4567cc7654...", "....45677654....", ".....456654.....", "......4554......",
        ".....4.44.4.....", "....4..4...4....", "........4.......", "................",
    ], {"4": G[1], "5": G[2], "6": G[3], "7": G[4], "c": hexc("#30c8f0"), "W": hexc("#ffffff")}), OUTLINE)
    PAPER = ramp("#6a5a3a", "#a08a60", "#d0bc8c", "#efe2bc")
    M["dungeon_map"] = outline_sprite(sprite([
        "................", "..222222222222..", ".23333333333332.", ".23443333344332.",
        ".2343xx3333x332.", ".23433x33334x32.", ".234333xx3343x2.", ".2333334333xx32.",
        ".23rr33x433x332.", ".233rr3343xx332.", ".23rr3r33443332.", ".23333rr3333332.",
        ".23344333333432.", ".23333333333332.", "..222222222222..", "................",
    ], {"2": PAPER[0], "3": PAPER[2], "4": PAPER[1], "x": hexc("#5a3a20"), "r": hexc("#c41e1e")}), OUTLINE)
    return M


# ---------------- Текстуры брони на модели ----------------
def armor_pixel(setn, face, i, j, w, h, part):
    r = MATS[setn]["r"]
    a = MATS[setn]["a"]
    c = r[2]
    if setn in ("ashen", "archivist"):
        c = r[2 + (1 if i % 4 == 1 else -1 if i % 4 == 3 else 0)]
    if i == 0 or j == 0:
        c = r[3]
    if i == w - 1 or j == h - 1:
        c = r[1]
    if setn in ("ether_steel", "star_iron", "prism") and j % 4 == 3 and part != "head":
        c = r[1]
    if setn == "prism" and (i + j) % 5 == 0:
        c = r[4]
    if setn == "star_iron" and (i * 5 + j * 3) % 13 == 0:
        c = a[1]
    if part == "head" and face == "front":
        if setn == "archivist":
            if 1 <= i <= w - 2 and 2 <= j:
                return None  # капюшон открыт спереди
        elif setn == "ashen":
            if 1 <= i <= w - 2 and 3 <= j <= h - 2:
                return None
        else:
            if j in (3, 4) and 1 <= i <= w - 2:
                c = (14, 10, 18)
            if j in (3, 4) and setn == "ether_steel" and i in (2, w - 3):
                c = a[1]
    if part == "body" and face == "front":
        if setn in ("ether_steel", "archivist", "star_iron") and i in (w // 2 - 1, w // 2) and 2 <= j <= h - 3:
            c = a[1] if j % 3 else a[0]
        if setn == "ashen" and (i + j * 2) % 7 == 0:
            c = a[1]
        if setn == "prism" and 3 <= i <= 4 and 3 <= j <= 5:
            c = a[0]
    if part in ("arm",) and j < 4 and setn in ("star_iron", "ether_steel", "archivist"):
        c = a[0] if j == 3 else r[3]
    if setn == "phantom":
        # Бледные пластины с призрачными швами и мерцающими прожилками
        c = r[2] if (i + j) % 6 else r[3]
        if j % 5 == 4 and part != "head":
            c = a[0]
        if (i * 3 + j * 7) % 17 == 0:
            c = a[1]
        if i == 0 or j == 0:
            c = r[4]
        if i == w - 1 or j == h - 1:
            c = r[1]
        if part == "head" and face == "front":
            c = r[1] if 2 <= j <= 5 else c
            if j == 3 and i in (1, 2, w - 3, w - 2):
                c = a[1]
            if j == 4 and i in (2, w - 3):
                c = a[0]
        if part == "body" and face == "front" and abs(i - (w - 1) / 2) <= 1.5 and 3 <= j <= 6:
            c = a[1] if (i + j) % 2 else a[0]
    if face == "up":
        c = r[3]
    return c


def paint_box(img, u, v, dx, dy, dz, fn):
    regions = [("up", u + dz, v, dx, dz), ("down", u + dz + dx, v, dx, dz), ("right", u, v + dz, dz, dy),
               ("front", u + dz, v + dz, dx, dy), ("left", u + dz + dx, v + dz, dz, dy), ("back", u + 2 * dz + dx, v + dz, dx, dy)]
    for face, x0, y0, w, h in regions:
        for i in range(w):
            for j in range(h):
                c = fn(face, i, j, w, h)
                if c is not None:
                    put(img, x0 + i, y0 + j, c)


def armor_layers(setn):
    l1, l2 = new(64, 32), new(64, 32)
    paint_box(l1, 0, 0, 8, 8, 8, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "head"))
    paint_box(l1, 16, 16, 8, 12, 4, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "body"))
    paint_box(l1, 40, 16, 4, 12, 4, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "arm"))
    paint_box(l1, 0, 16, 4, 12, 4, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "boot") if (f in ("up", "down") or j >= 8) else None)
    paint_box(l2, 16, 16, 8, 12, 4, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "waist") if (f not in ("up",) and j >= 7) else None)
    paint_box(l2, 0, 16, 4, 12, 4, lambda f, i, j, w, h: armor_pixel(setn, f, i, j, w, h, "leg"))
    return l1, l2


def mannequin_preview(sets):
    skin = new(64, 64)
    paint_box(skin, 0, 0, 8, 8, 8, lambda f, i, j, w, h: (200, 160, 130))
    paint_box(skin, 16, 16, 8, 12, 4, lambda f, i, j, w, h: (60, 90, 140))
    paint_box(skin, 40, 16, 4, 12, 4, lambda f, i, j, w, h: (200, 160, 130))
    paint_box(skin, 0, 16, 4, 12, 4, lambda f, i, j, w, h: (50, 50, 90))
    cells = []
    for setn in sets:
        l1, l2 = armor_layers(setn)
        s = Scene()

        def part(p, cubes, tex):
            add_entity_part(s, {"pose": p, "cubes": cubes, "children": []}, tex)
        for tex, infl in ((l2, 0), (l1, 0)):
            def box(uv, o, sz):
                return {"uv": uv, "origin": (o[0] - infl, o[1] - infl, o[2] - infl), "size": (sz[0] + 2 * infl, sz[1] + 2 * infl, sz[2] + 2 * infl)}
            # Инфляция меняет размер, но не развёртку — для превью растягиваем текстуру через size.
            if tex is l2:
                part((0, 0, 0, 0, 0, 0), [box((16, 16), (-4, 0, -2), (8, 12, 4))], tex)
                part((-1.9, 12, 0, 0, 0, 0), [box((0, 16), (-2, 0, -2), (4, 12, 4))], tex)
                part((1.9, 12, 0, 0, 0, 0), [box((0, 16), (-2, 0, -2), (4, 12, 4))], tex)
                continue
            part((0, 0, 0, 0, 0, 0), [box((0, 0), (-4, -8, -4), (8, 8, 8))], tex)
            part((0, 0, 0, 0, 0, 0), [box((16, 16), (-4, 0, -2), (8, 12, 4))], tex)
            part((-5, 2, 0, 0, 0, 0.1), [box((40, 16), (-3, -2, -2), (4, 12, 4))], tex)
            part((5, 2, 0, 0, 0, -0.1), [box((40, 16), (-1, -2, -2), (4, 12, 4))], tex)
            part((-1.9, 12, 0, 0, 0, 0), [box((0, 16), (-2, 0, -2), (4, 12, 4))], tex)
            part((1.9, 12, 0, 0, 0, 0), [box((0, 16), (-2, 0, -2), (4, 12, 4))], tex)
        cells.append(s.render(240, yaw=-25, pitch=10))
    return cells


def main():
    tex_item = f"{RES}/textures/item"
    os.makedirs(tex_item, exist_ok=True)
    icons = {}
    for setn, m in MATS.items():
        for piece, rows in ARMOR_MASKS.items():
            icons[f"{setn}_{piece}"] = mask_icon(rows, m["r"], m["a"])
    for mat in ("ether_steel", "star_iron"):
        for tool, rows in TOOL_MASKS.items():
            icons[f"{mat}_{tool}"] = mask_icon(rows, MATS[mat]["r"], MATS[mat]["a"])
    icons.update(materials())
    big = weapons()
    bows = bow_frames()
    for n, img in {**icons, **big}.items():
        img.save(f"{tex_item}/{n}.png")
    names_b = ["ether_bow", "ether_bow_pulling_0", "ether_bow_pulling_1", "ether_bow_pulling_2"]
    for n, img in zip(names_b, bows):
        img.save(f"{tex_item}/{n}.png")

    armor_dir = f"{RES}/textures/models/armor"
    os.makedirs(armor_dir, exist_ok=True)
    for setn in MATS:
        l1, l2 = armor_layers(setn)
        l1.save(f"{armor_dir}/{setn}_layer_1.png")
        l2.save(f"{armor_dir}/{setn}_layer_2.png")

    # модели предметов
    md = f"{RES}/models/item"
    def wj(name, obj):
        with open(f"{md}/{name}.json", "w") as f:
            json.dump(obj, f, indent=2)
    for n in icons:
        handheld = any(n.endswith(t) for t in TOOL_MASKS)
        wj(n, {"parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated", "textures": {"layer0": f"aetherwastes:item/{n}"}})
    for n in big:
        wj(n, {"parent": "minecraft:item/handheld", "textures": {"layer0": f"aetherwastes:item/{n}"}})
    bow_display = {"thirdperson_righthand": {"rotation": [-80, 260, -40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
                   "thirdperson_lefthand": {"rotation": [-80, -280, 40], "translation": [-1, -2, 2.5], "scale": [0.9, 0.9, 0.9]},
                   "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
                   "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]}}
    wj("ether_bow", {"parent": "minecraft:item/generated", "textures": {"layer0": "aetherwastes:item/ether_bow"}, "display": bow_display,
                     "overrides": [{"predicate": {"pulling": 1}, "model": "aetherwastes:item/ether_bow_pulling_0"},
                                   {"predicate": {"pulling": 1, "pull": 0.65}, "model": "aetherwastes:item/ether_bow_pulling_1"},
                                   {"predicate": {"pulling": 1, "pull": 0.9}, "model": "aetherwastes:item/ether_bow_pulling_2"}]})
    for i in range(3):
        wj(f"ether_bow_pulling_{i}", {"parent": "minecraft:item/generated", "display": bow_display,
                                      "textures": {"layer0": f"aetherwastes:item/ether_bow_pulling_{i}"}})
    for egg in ("ash_hound_spawn_egg", "ether_wisp_spawn_egg", "scar_crawler_spawn_egg"):
        wj(egg, {"parent": "minecraft:item/template_spawn_egg"})

    # превью
    allicons = list(icons.items())
    sheet_w = 12 * 72
    rows = (len(allicons) + 11) // 12
    sheet = Image.new("RGBA", (sheet_w, rows * 72 + 150 + 250), (139, 139, 139, 255))
    for k, (n, img) in enumerate(allicons):
        sheet.alpha_composite(img.resize((64, 64), Image.NEAREST), ((k % 12) * 72 + 4, (k // 12) * 72 + 4))
    y0 = rows * 72 + 4
    for k, img in enumerate(list(big.values()) + bows):
        sheet.alpha_composite(img.resize((64, 64), Image.NEAREST), ((k % 13) * 66 + 4, y0))
    for k, cell in enumerate(mannequin_preview(list(MATS))):
        sheet.alpha_composite(cell.resize((170, 170)), (k * 172, y0 + 75))
    sheet.save(f"{PREV}/gear_preview.png")
    print(len(icons), "icons,", len(big), "weapons")


if __name__ == "__main__":
    main()
