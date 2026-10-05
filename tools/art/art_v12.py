"""Графика 1.2: блоки подземелий, частицы, интерфейс (спрайты HUD, рамка босса), виньетка."""
import json, math, os, sys, random
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from texlib import *
from PIL import Image, ImageDraw, ImageFilter

RES = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes/textures"
PREV = "/tmp/claude-0/-home-claude/6e7c1073-f517-50dd-916e-c96eb0f32376/scratchpad/art"
os.makedirs(f"{RES}/block", exist_ok=True)
os.makedirs(f"{RES}/particle", exist_ok=True)
os.makedirs(f"{RES}/gui/sprites/hud", exist_ok=True)

DEEP = ramp("#1c1c22", "#26262e", "#30303a", "#3c3c48", "#4a4a58")
CYAN = ramp("#0a4a66", "#1a8ab0", "#38d0f0", "#a8f4ff", "#ffffff")
ASH = ramp("#16141a", "#201e24", "#2c2930", "#38343c", "#46414a")
EMB = ramp("#5a1400", "#a83200", "#ff6a10", "#ffb040", "#fff0a0")
PURP = ramp("#2a1050", "#4a2a8a", "#7d45c9", "#c9a2ff", "#f4e8ff")
WOODD = ramp("#2a1a10", "#3c2616", "#4e321e", "#6e4a2c")


def save(img, path, frames=None, frametime=4, interpolate=True):
    img.save(path)
    if frames:
        with open(path + ".mcmeta", "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, f)


def glow_line(img, pts, col, core):
    d = ImageDraw.Draw(img)
    d.line(pts, fill=tuple(col) + (255,), width=1)
    return img


# ---------------- Блоки ----------------
def rune_bricks(seed=3, cracked=False, glow=1.0):
    img = bricks(16, 16, DEEP, ramp("#121216", "#18181e"), seed)
    rnd = random.Random(seed)
    # руны на кирпичах
    for k, (x, y) in enumerate(((1, 0), (9, 4), (2, 8), (10, 12))):
        if rnd.random() < 0.75:
            col = mix(CYAN[1], CYAN[2], glow)
            rune(img, x + 1, y, rnd.randrange(8), col, mix(DEEP[2], CYAN[0], glow))
    if cracked:
        d = ImageDraw.Draw(img)
        d.line([(3, 0), (5, 4), (4, 7), (7, 10), (6, 15)], fill=(10, 10, 14, 255))
        d.line([(12, 2), (11, 6), (14, 9)], fill=(10, 10, 14, 255))
    return img


def rune_pillar():
    side = new(16, 16)
    for y in range(16):
        for x in range(16):
            c = DEEP[2] if 2 <= x <= 13 else DEEP[1]
            if x in (2, 13):
                c = DEEP[3]
            if x in (0, 15):
                c = DEEP[0]
            if y in (0, 15):
                c = DEEP[4] if 1 <= x <= 14 else DEEP[1]
            if (x * 3 + y * 7) % 23 == 0:
                c = DEEP[1]
            put(side, x, y, c)
    for y in range(3, 13):
        put(side, 7, y, CYAN[2] if y % 3 else CYAN[3])
        put(side, 8, y, CYAN[1] if y % 3 else CYAN[2])
    for y in (4, 8, 12):
        put(side, 6, y, CYAN[1])
        put(side, 9, y, CYAN[1])
    top = new(16, 16)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            c = DEEP[3] if r < 6.5 else DEEP[1]
            if 4.3 < r < 5.3:
                c = CYAN[1]
            if r < 1.6:
                c = CYAN[3]
            if x in (0, 15) or y in (0, 15):
                c = DEEP[0]
            put(top, x, y, c)
    return side, top


def phantom_lantern_frames():
    frames = []
    for f in range(4):
        img = new(16, 16)
        ph = f / 4 * math.pi * 2
        for y in range(16):
            for x in range(16):
                edge = x in (0, 15) or y in (0, 15)
                inner = x in (1, 14) or y in (1, 14)
                if edge:
                    c = DEEP[1]
                elif inner:
                    c = DEEP[3] if (x + y) % 2 else DEEP[2]
                else:
                    r = math.hypot(x - 7.5, y - 7.5)
                    w = 0.5 + 0.5 * math.sin(ph + r * 0.9)
                    c = mix(CYAN[2], CYAN[4], max(0, 1 - r / 7) * (0.6 + 0.4 * w))
                    if (x in (5, 10) or y in (5, 10)) and 2 <= x <= 13 and 2 <= y <= 13:
                        c = mix(c, CYAN[1], 0.55)
                put(img, x, y, c)
        frames.append(img)
    return anim_strip(frames)


def phantom_glass():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                put(img, x, y, CYAN[1], 230)
            elif (x + y) % 9 == 0 and 2 < x < 13:
                put(img, x, y, CYAN[3], 140)
            else:
                put(img, x, y, mix(CYAN[0], CYAN[1], (y / 16)), 70)
    for (x, y) in ((3, 3), (4, 4), (11, 9), (12, 10)):
        put(img, x, y, CYAN[4], 200)
    return img


def ash_bricks():
    img = bricks(16, 16, ASH, ramp("#0c0b0e", "#121014"), 11, bw=8, bh=4)
    nz = Noise(5)
    for y in range(16):
        for x in range(16):
            if nz.h(x, y) > 0.93:
                put(img, x, y, ramp("#5a5560")[0])
    return img


def ember_frames():
    base = bricks(16, 16, ASH, ramp("#0c0b0e", "#121014"), 13, bw=8, bh=4)
    frames = []
    for f in range(4):
        img = base.copy()
        t = 0.5 + 0.5 * math.sin(f / 4 * math.pi * 2)
        # тлеющие швы
        for y in range(16):
            for x in range(16):
                row = y // 4
                off = 4 * (row % 2)
                if y % 4 == 3 or (x + off) % 8 == 7:
                    k = 0.5 + 0.5 * math.sin(x * 0.7 + y * 0.4 + f * 1.6)
                    put(img, x, y, mix(EMB[1], EMB[3], k * (0.5 + 0.5 * t)))
        for (x, y) in ((2, 1), (11, 6), (5, 9), (13, 13)):
            put(img, x, y, EMB[2 + (f % 2)])
        frames.append(img)
    return anim_strip(frames)


def archive_shelf():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            c = WOODD[2]
            if y in (0, 7, 8, 15):
                c = WOODD[3] if y in (0, 8) else WOODD[1]
            if x in (0, 15):
                c = WOODD[1]
            put(img, x, y, c)
    cols = [PURP[1], PURP[2], hexc("#2a4a8a"), hexc("#1a6a6a"), PURP[3], hexc("#6a2a4a"), hexc("#3a3a6a")]
    rnd = random.Random(7)
    for shelf in (1, 9):
        x = 1
        while x < 15:
            w = 1 + (rnd.random() < 0.4)
            h = 5 + (rnd.random() < 0.5)
            c = rnd.choice(cols)
            for i in range(w):
                for j in range(h):
                    yy = shelf + (6 - h) + j
                    put(img, x + i, yy, c if j != 1 else mix(c, (255, 220, 120), 0.6))
            x += w
            if rnd.random() < 0.15:
                x += 1
    # светящиеся корешки
    put(img, 6, 3, CYAN[3])
    put(img, 11, 11, CYAN[3])
    return img


def trap_tex(kind, armed=True):
    col = {"archive": CYAN, "crypt": ramp("#1a3a5a", "#3a7ab0", "#8ad0ff", "#e0f4ff", "#ffffff"), "citadel": EMB}[kind]
    img = new(16, 16)
    a = 255 if armed else 120
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 6.0 < r < 7.2:
                put(img, x, y, col[2] if armed else col[0], a)
            elif 3.0 < r < 3.8:
                put(img, x, y, col[1], a)
    for k in range(6):
        ang = k * math.pi / 3
        x, y = int(7.5 + math.cos(ang) * 5), int(7.5 + math.sin(ang) * 5)
        put(img, x, y, col[3] if armed else col[1], a)
    rune(img, 6, 6, {"archive": 0, "crypt": 4, "citadel": 2}[kind], col[3] if armed else col[1])
    return img


def seal_side():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            c = DEEP[2]
            if y in (6, 15):
                c = DEEP[0]
            if y == 7:
                c = DEEP[4]
            if (x + y) % 5 == 0 and y > 7:
                c = DEEP[3]
            put(img, x, y, c)
    for x in (3, 8, 13):
        for y in range(9, 14):
            put(img, x, y, PURP[2] if y % 2 else PURP[3])
    return img


def seal_top_frames(kind):
    col = {"archive": PURP, "crypt": CYAN, "citadel": EMB}[kind]
    frames = []
    for f in range(8):
        img = new(16, 16)
        ang0 = f / 8 * math.pi * 2
        for y in range(16):
            for x in range(16):
                r = math.hypot(x - 7.5, y - 7.5)
                a = math.atan2(y - 7.5, x - 7.5)
                c = DEEP[2] if (x + y) % 2 else DEEP[1]
                if 5.6 < r < 7.0:
                    c = col[1] if math.sin(a * 6 + ang0 * 2) > 0 else col[2]
                if 2.5 < r < 3.4:
                    c = col[2] if math.sin(a * 4 - ang0 * 3) > 0 else col[3]
                if r < 1.6:
                    c = col[4]
                if x in (0, 15) or y in (0, 15):
                    c = DEEP[0]
                put(img, x, y, c)
        frames.append(img)
    return anim_strip(frames)


def seal_top_dormant():
    img = new(16, 16)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            c = DEEP[2] if (x + y) % 2 else DEEP[1]
            if 5.6 < r < 7.0 or 2.5 < r < 3.4:
                c = DEEP[3]
            if x in (0, 15) or y in (0, 15):
                c = DEEP[0]
            put(img, x, y, c)
    d = ImageDraw.Draw(img)
    d.line([(3, 2), (7, 7), (6, 11), (10, 14)], fill=(8, 8, 10, 255))
    return img


def crystal_frames(kind):
    col = {"archive": PURP, "crypt": CYAN, "citadel": EMB, "dormant": DEEP}[kind]
    frames = []
    for f in range(4 if kind != "dormant" else 1):
        img = new(16, 16)
        for y in range(16):
            for x in range(16):
                k = (x + y + f * 3) % 8
                c = col[2] if k < 3 else col[3] if k < 5 else col[1]
                if x in (0, 5) or y in (0, 7):
                    c = col[1]
                if (x - y + f) % 7 == 0:
                    c = col[4]
                put(img, x, y, c)
        frames.append(img)
    return anim_strip(frames) if len(frames) > 1 else frames[0]


def blocks():
    B = f"{RES}/block"
    save(rune_bricks(), f"{B}/rune_bricks.png")
    save(rune_bricks(9, cracked=True, glow=0.6), f"{B}/cracked_rune_bricks.png")
    side, top = rune_pillar()
    save(side, f"{B}/rune_pillar.png")
    save(top, f"{B}/rune_pillar_top.png")
    save(phantom_lantern_frames(), f"{B}/phantom_lantern.png", frames=True, frametime=5)
    save(phantom_glass(), f"{B}/phantom_glass.png")
    save(ash_bricks(), f"{B}/ash_bricks.png")
    save(ember_frames(), f"{B}/ember_bricks.png", frames=True, frametime=6)
    save(archive_shelf(), f"{B}/archive_shelf.png")
    for k in ("archive", "crypt", "citadel"):
        save(trap_tex(k), f"{B}/dungeon_trap_{k}.png")
        save(trap_tex(k, False), f"{B}/dungeon_trap_{k}_off.png")
        save(seal_top_frames(k), f"{B}/guardian_seal_top_{k}.png", frames=True, frametime=3)
        save(crystal_frames(k), f"{B}/seal_crystal_{k}.png", frames=True, frametime=4)
    save(seal_side(), f"{B}/guardian_seal_side.png")
    save(seal_top_dormant(), f"{B}/guardian_seal_top_dormant.png")
    save(crystal_frames("dormant"), f"{B}/seal_crystal_dormant.png")


# ---------------- Частицы ----------------
def soft(size, col, core, r_out, r_in=0.0, alpha=1.0):
    img = new(size, size)
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c)
            if r > r_out:
                continue
            t = r / r_out
            colr = mix(core, col, min(1, max(0, (r - r_in) / max(0.01, r_out - r_in))))
            put(img, x, y, colr, int(255 * alpha * (1 - t * t)))
    return img


def particles():
    P = f"{RES}/particle"
    for i in range(4):
        # искра: крестик со свечением
        img = new(8, 8)
        s = [3, 2, 1, 1][i]
        for k in range(-s, s + 1):
            put(img, 3 + k, 3, CYAN[3] if abs(k) < s else CYAN[2], 255 - abs(k) * 40)
            put(img, 3, 3 + k, CYAN[3] if abs(k) < s else CYAN[2], 255 - abs(k) * 40)
        put(img, 3, 3, (255, 255, 255))
        img.save(f"{P}/ether_spark_{i}.png")
        # призрачный огонёк
        w = soft(8, CYAN[1], CYAN[4], [3.6, 3.4, 3.0, 2.4][i], alpha=[1, 0.9, 0.75, 0.55][i])
        w.save(f"{P}/phantom_wisp_{i}.png")
        # уголь
        e = soft(8, EMB[1], EMB[4], [2.6, 2.3, 1.9, 1.4][i])
        e.save(f"{P}/ash_ember_{i}.png")
    for i in range(6):
        img = new(8, 8)
        rune(img, 2, 2, i, CYAN[3], CYAN[1])
        img.save(f"{P}/rune_{i}.png")


# ---------------- Интерфейс ----------------
def nine(path, img, border, w=None, h=None):
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        json.dump({"gui": {"scaling": {"type": "nine_slice", "width": img.width, "height": img.height, "border": border}}}, f)


def gui():
    G = f"{RES}/gui/sprites/hud"
    # панель: тёмное стекло с золотыми уголками
    p = new(32, 32)
    for y in range(32):
        for x in range(32):
            edge = x in (0, 31) or y in (0, 31)
            inner = x in (1, 30) or y in (1, 30)
            if edge:
                put(p, x, y, (10, 8, 18), 230)
            elif inner:
                put(p, x, y, (70, 52, 110), 230)
            else:
                put(p, x, y, mix((18, 14, 32), (10, 8, 20), y / 32), 175)
    for cx, cy in ((1, 1), (28, 1), (1, 28), (28, 28)):
        for i in range(3):
            put(p, cx + i, cy, (230, 180, 70))
            put(p, cx, cy + i, (230, 180, 70))
    put(p, 2, 2, (255, 230, 150))
    put(p, 29, 2, (255, 230, 150))
    put(p, 2, 29, (255, 230, 150))
    put(p, 29, 29, (255, 230, 150))
    for cx, cy in ((28, 1), (1, 28), (28, 28)):
        pass
    # отражаем правильные уголки
    pr = p.copy()
    nine(f"{G}/panel.png", pr, 4)
    # рамка полосы
    f = new(16, 7)
    for y in range(7):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 6)
            put(f, x, y, (8, 6, 14) if edge else (24, 20, 36), 240 if edge else 200)
    put(f, 0, 0, (0, 0, 0), 0)
    put(f, 15, 0, (0, 0, 0), 0)
    put(f, 0, 6, (0, 0, 0), 0)
    put(f, 15, 6, (0, 0, 0), 0)
    nine(f"{G}/bar_frame.png", f, 2)

    def fill(name, c0, c1, c2):
        img = new(90, 5)
        for x in range(90):
            t = x / 89
            base = mix(c0, c1, t)
            for y in range(5):
                c = base
                if y == 0:
                    c = mix(base, c2, 0.55)
                elif y == 4:
                    c = shade(base, 0.6)
                if x % 9 == 8:
                    c = shade(c, 0.82)
                put(img, x, y, c)
        img.save(f"{G}/{name}.png")

    fill("bar_vessel", hexc("#4a1a8a"), hexc("#a868ff"), hexc("#f0e0ff"))
    fill("bar_overflow", hexc("#a87010"), hexc("#ffd060"), hexc("#fff6c0"))
    fill("bar_clarity", hexc("#106a8a"), hexc("#5ad8ff"), hexc("#e0faff"))
    fill("bar_clarity_low", hexc("#6a0a14"), hexc("#ff4a5a"), hexc("#ffd0d4"))
    fill("bar_warm", hexc("#1a6a2a"), hexc("#9ae070"), hexc("#effff0"))
    fill("bar_cold", hexc("#1a3a8a"), hexc("#6ac4ff"), hexc("#e8f6ff"))
    fill("bar_hot", hexc("#8a2a00"), hexc("#ffa040"), hexc("#fff0c0"))
    sh = new(12, 5)
    for x in range(12):
        a = int(150 * math.sin(math.pi * x / 11))
        for y in range(5):
            put(sh, x, y, (255, 255, 255), a if y < 3 else a // 2)
    sh.save(f"{G}/bar_sheen.png")

    def icon(name, rows, pal):
        outline_sprite(sprite(rows, pal), (8, 6, 14)).save(f"{G}/{name}.png")

    icon("icon_vessel", [".........", "....a....", "...aba...", "..abcba..", "..abcba..", "..abcba..", "...aba...", "....a....", "........."],
         {"a": hexc("#5a2d9a"), "b": hexc("#a06ae6"), "c": hexc("#f0e0ff")})
    icon("icon_clarity", [".........", ".........", "..aaaaa..", ".abbcbba.", "abbcdcbba", ".abbcbba.", "..aaaaa..", ".........", "........."],
         {"a": hexc("#1a6a8a"), "b": hexc("#bfefff"), "c": hexc("#3ab0e0"), "d": hexc("#0a1a2a")})
    icon("icon_temp", ["....a....", "...aba...", "...aba...", "...aca...", "...aca...", "..acdca..", "..adddа..".replace("а", "a"), "..acdca..", "...aaa..."],
         {"a": hexc("#c0c0d0"), "b": hexc("#ffffff"), "c": hexc("#ff7a30"), "d": hexc("#ffb060")})
    icon("icon_pressure", [".........", "..aaaa...", ".a....a..", "a..bb..a.", "a.b..b.a.", "a..b.ba..", ".a...a...", "..aaa....", "........."],
         {"a": hexc("#c9a2ff"), "b": hexc("#7d45c9")})
    icon("icon_noise", [".........", "...a.....", "..aa..b..", ".aaa.b.b.", ".aaa.b.b.", ".aaa.b.b.", "..aa..b..", "...a.....", "........."],
         {"a": hexc("#d0d0d0"), "b": hexc("#ff9a60")})
    # ячейка способности
    for name, rim, glow in (("ability_slot", (60, 70, 90), (30, 40, 56)), ("ability_slot_ready", (120, 230, 255), (40, 120, 160))):
        s = new(24, 24)
        for y in range(24):
            for x in range(24):
                edge = x in (0, 23) or y in (0, 23)
                inner = x in (1, 22) or y in (1, 22)
                if edge:
                    put(s, x, y, (6, 6, 12), 240)
                elif inner:
                    put(s, x, y, rim)
                elif x in (2, 21) or y in (2, 21):
                    put(s, x, y, glow)
                else:
                    put(s, x, y, (12, 16, 26), 210)
        for c in ((0, 0), (23, 0), (0, 23), (23, 23)):
            put(s, c[0], c[1], (0, 0, 0), 0)
        s.save(f"{G}/{name}.png")
    # иконка призрака 18×18
    ghost = sprite([
        "..................", "......aaaaaa......", ".....abbbbbba.....", "....abbccccbba....", "...abccccccccba...",
        "...abcddccddcba...", "...abcdeccdecba...", "...abccccccccba...", "...abcccffcccba...", "...abccccccccba...",
        "...abccccccccba...", "...abccccccccba...", "...abcccccccccba..", "...abccbccbccbba..", "...abcb.bcb.bcba..",
        "...aba..aba..aba..", "....a....a....a...", "..................",
    ], {"a": hexc("#0a3a4a"), "b": hexc("#3ab0d0"), "c": hexc("#bff4ff"), "d": hexc("#0a1a24"), "e": hexc("#30e0ff"),
        "f": hexc("#7ad0e0")})
    ghost.save(f"{G}/phantom_icon.png")
    pb = new(4, 18)
    for y in range(18):
        for x in range(4):
            put(pb, x, y, mix(hexc("#e0ffff"), hexc("#20a0d0"), y / 17) if x in (1, 2) else hexc("#0a3a4a"))
    pb.save(f"{G}/phase_bar.png")
    # рамка босса 204×17 (9-slice по 12 px)
    bf = new(48, 17)
    for y in range(17):
        for x in range(48):
            put(bf, x, y, (0, 0, 0), 0)
    d = ImageDraw.Draw(bf)
    d.rectangle([10, 4, 37, 12], outline=(20, 14, 26, 255))
    d.line([(11, 4), (36, 4)], fill=(220, 170, 70, 255))
    d.line([(11, 12), (36, 12)], fill=(140, 100, 40, 255))
    # уголки-шипы
    for side in (0, 1):
        ox = 0 if side == 0 else 37
        pts = [(ox + 10 if side == 0 else ox, 8), (ox + 5 if side == 0 else ox + 5, 2), (ox + 1 if side == 0 else ox + 9, 8),
               (ox + 5 if side == 0 else ox + 5, 14)]
        d.polygon(pts, fill=(60, 40, 20, 255), outline=(230, 180, 70, 255))
        d.point([(ox + 5, 8)], fill=(255, 240, 180, 255))
    nine(f"{G}/boss_frame.png", bf, {"left": 12, "top": 5, "right": 12, "bottom": 5})
    bb = new(16, 7)
    for y in range(7):
        for x in range(16):
            put(bb, x, y, (10, 6, 12) if x in (0, 15) or y in (0, 6) else (34, 22, 30), 230)
    nine(f"{G}/boss_back.png", bb, 2)
    for kind, (c0, c1, c2) in {"crypt": (hexc("#0a5a7a"), hexc("#5ae0ff"), hexc("#e8ffff")),
                               "archive": (hexc("#4a1a8a"), hexc("#b07aff"), hexc("#f4e8ff")),
                               "citadel": (hexc("#7a1a00"), hexc("#ff8a20"), hexc("#fff0b0"))}.items():
        img = new(180, 5)
        for x in range(180):
            base = mix(c0, c1, 0.5 + 0.5 * math.sin(x * 0.11))
            for y in range(5):
                c = mix(base, c2, 0.6) if y == 0 else shade(base, 0.65) if y == 4 else base
                if x % 18 == 17:
                    c = shade(c, 0.6)
                put(img, x, y, c)
        img.save(f"{G}/boss_fill_{kind}.png")

    # виньетка Призрачного шага
    v = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = v.load()
    nz = Noise(77)
    for y in range(256):
        for x in range(256):
            r = math.hypot((x - 127.5) / 128, (y - 127.5) / 128)
            n = nz.smooth(x, y, 32, 256)
            t = max(0.0, min(1.0, (r - 0.55 + n * 0.25) / 0.6))
            a = int(210 * t ** 1.6)
            c = mix((20, 160, 200), (6, 30, 50), t)
            px[x, y] = c + (a,)
    v = v.filter(ImageFilter.GaussianBlur(2))
    v.save(f"{RES}/gui/phantom_vignette.png")


def preview():
    sheet = Image.new("RGBA", (16 * 5 * 8, 16 * 5 * 3), (40, 40, 48, 255))
    files = ["rune_bricks", "cracked_rune_bricks", "rune_pillar", "rune_pillar_top", "phantom_lantern", "phantom_glass",
             "ash_bricks", "ember_bricks", "archive_shelf", "dungeon_trap_archive", "dungeon_trap_crypt", "dungeon_trap_citadel",
             "guardian_seal_top_archive", "guardian_seal_top_crypt", "guardian_seal_top_citadel", "guardian_seal_side",
             "seal_crystal_crypt", "guardian_seal_top_dormant"]
    for k, f in enumerate(files):
        img = Image.open(f"{RES}/block/{f}.png").crop((0, 0, 16, 16)).resize((72, 72), Image.NEAREST)
        sheet.alpha_composite(img, ((k % 8) * 80 + 4, (k // 8) * 80 + 4))
    sheet.save(f"{PREV}/blocks_v12.png")
    hud = Image.new("RGBA", (400, 120), (90, 120, 80, 255))
    x = 4
    for f in sorted(os.listdir(f"{RES}/gui/sprites/hud")):
        if f.endswith(".png"):
            im = Image.open(f"{RES}/gui/sprites/hud/{f}")
            im = im.resize((im.width * 2, im.height * 2), Image.NEAREST)
            if x + im.width > 400:
                continue
            hud.alpha_composite(im, (x, 4 if im.width < 60 else 60))
            x += im.width + 4 if im.width < 60 else 0
    hud.save(f"{PREV}/hud_v12.png")


if __name__ == "__main__":
    blocks()
    particles()
    gui()
    preview()
    print("art v1.2 ok")
