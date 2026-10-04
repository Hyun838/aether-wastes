"""Модели мобов: описание → Java LayerDefinition + текстура (box-UV) + слой свечения + превью."""
import os, sys, math, random
sys.path.insert(0, os.path.dirname(__file__))
from texlib import *
from render3d import Scene, add_entity_part
from PIL import Image, ImageDraw

ROOT = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes/textures/entity"
JAVA = "/home/claude/aetherwastes/src/main/java/com/aetherwastes/client/ModModels.java"
HERE = "/tmp/claude-0/-home-claude/6e7c1073-f517-50dd-916e-c96eb0f32376/scratchpad/art"
R = math.radians


# ---------------- Описание модели ----------------
class Cube:
    def __init__(self, origin, size, mat, **kw):
        self.origin = origin
        self.size = tuple(int(s) for s in size)
        self.mat = mat
        self.kw = kw
        self.uv = None


class Part:
    _n = 0

    def __init__(self, name=None, pose=(0, 0, 0, 0, 0, 0), cubes=(), children=()):
        if name is None:
            Part._n += 1
            name = f"d{Part._n}"
        self.name = name
        self.pose = tuple(pose) + (0,) * (6 - len(pose))
        self.cubes = list(cubes)
        self.children = list(children)

    def add(self, *parts):
        self.children.extend(parts)
        return self


def P(name=None, pose=(0, 0, 0), *cubes, children=()):
    return Part(name, pose, cubes, children)


def all_cubes(parts):
    for p in parts:
        yield from p.cubes
        yield from all_cubes(p.children)


def pack(parts, width):
    cubes = sorted(all_cubes(parts), key=lambda c: -(c.size[2] + c.size[1]))
    x = y = row_h = 0
    for c in cubes:
        dx, dy, dz = c.size
        w, h = 2 * (dx + dz), dz + dy
        if x + w > width:
            x, y, row_h = 0, y + row_h, 0
        c.uv = (x, y)
        x += w
        row_h = max(row_h, h)
    height = y + row_h
    th = 32
    while th < height:
        th *= 2
    return th


# ---------------- Материалы ----------------
def mat_color(mat, face, i, j, w, h, pal):
    """Возвращает (цвет, свечение|None) для текселя (i,j) грани face размера w×h."""
    rnd = random.Random(hash((mat, face, i, j, w, h)) & 0xffffffff)
    n = rnd.random()
    glow = None
    kind = mat.split(":")[0]
    arg = mat.split(":")[1] if ":" in mat else ""
    r = pal.get(kind) or pal["robe"]

    def pick(idx):
        return r[max(0, min(len(r) - 1, idx))]

    if kind in ("robe", "cloth", "skirt"):
        idx = 2 + (1 if i % 4 == 1 else -1 if i % 4 == 3 else 0) + (1 if n > 0.85 else 0)
        c = pick(idx)
        if face in ("front", "back", "left", "right") and j >= h - 2:
            c = pal["trim"][2 if j == h - 1 else 3]
        if face in ("front",) and arg == "runes" and 2 <= j < h - 3 and i in (w // 2 - 1, w // 2) and (j % 3 != 2):
            c = pal["glow"][1]
            glow = pal["glow"][2]
        if face == "up":
            c = pick(3)
    elif kind == "hood":
        c = pick(1 + (1 if n > 0.7 else 0))
        if face == "front" and (i == 0 or i == w - 1 or j == h - 1):
            c = pal["trim"][2]
    elif kind == "trim" or kind == "metal":
        c = pick(2 + (1 if (i + j) % 5 == 0 else 0))
        if j == 0 and face != "up":
            c = pick(4)
        if face == "up":
            c = pick(3)
    elif kind == "skin":
        c = pick(2 + (1 if n > 0.8 else -1 if n < 0.15 else 0))
    elif kind == "glowmat":
        c = pal["glow"][2 if n > 0.3 else 3]
        glow = pal["glow"][3 if n > 0.5 else 2]
    elif kind == "salt":
        c = pick(3 if n > 0.25 else 2)
        if (i * 3 + j * 5) % 11 == 0:
            c = pick(1)
        if face == "up":
            c = pick(4)
    elif kind == "glass":
        c = pick(2)
        if (i + j) % 6 == 0 or (i - j) % 9 == 0:
            c = pick(4)
        if i == 0 or j == 0:
            c = pick(3)
        if i == w - 1 or j == h - 1:
            c = pick(1)
    elif kind == "bark":
        c = pick(1 + (1 if (i + (j // 3)) % 3 == 0 else 0) + (1 if n > 0.8 else 0))
        if i % 4 == 0:
            c = pick(0)
        if face != "up" and arg == "resin" and i % 5 == 2 and (j + i) % 7 < 3:
            c = pal["resin"][2]
            glow = None
        if face == "up":
            c = pal["wood"][1 if (i + j) % 2 else 2]
    elif kind == "leaf":
        if n < 0.12:
            return None, None
        c = pick(1 + int(n * 3))
        if n > 0.93:
            c = (220, 120, 220)
    elif kind == "stone":
        c = pick(1 + int(n * 3))
        if i == 0 or j == 0:
            c = pick(4)
    elif kind == "fur":
        c = pick(1 + (1 if (i * 7 + j * 3) % 5 == 0 else 0) + (1 if n > 0.75 else 0))
        if arg == "embers" and n > 0.9:
            c = pal["glow"][2]
            glow = pal["glow"][2]
        if arg == "dark":
            c = pick(0 + (1 if n > 0.6 else 0))
        if face == "up":
            c = pick(3 if n > 0.5 else 2)
    elif kind == "chitin":
        c = pick(1 + (1 if n > 0.7 else 0))
        if (i + j) % 4 == 0:
            c = pick(3)
        if arg == "runes" and face in ("up", "back") and (i % 4 == 1) and (j % 3 != 1):
            c = pal["glow"][2]
            glow = pal["glow"][2]
    elif kind == "flame":
        t = j / max(1, h - 1)
        c = mix(pal["glow"][3], pal["glow"][1], t)
        glow = c
    else:
        c = pick(2)

    # --- лица ---
    if face == "front" and arg.startswith("face"):
        style = arg[4:] or "mask"
        cx = w / 2
        if style == "mask":
            # маска с прорезями глаз и тёмным капюшоном по краям
            if i in (0, w - 1) or j == 0:
                c = pal["hood"][0]
            else:
                c = pal["mask"][2 if n > 0.2 else 3]
                if j == h - 2 and 2 <= i < w - 2:
                    c = pal["mask"][1]
            ey = h // 2 - 1
            if j == ey and (i in (int(cx) - 3, int(cx) - 2, int(cx) + 1, int(cx) + 2)):
                c = pal["glow"][3]
                glow = pal["glow"][3]
            if j == ey + 1 and i in (int(cx) - 3, int(cx) + 2):
                c = pal["glow"][1]
                glow = pal["glow"][1]
            if i == int(cx) - 1 or i == int(cx):
                if j > ey + 2 and j < h - 2:
                    c = pal["mask"][1]
        elif style == "hollow":
            ex, ey = (i - (w - 1) / 2) / (w / 2), (j - (h - 1) / 2) / (h / 2)
            if ex * ex + ey * ey < 0.75:
                c = (18, 20, 30)
            if j == h // 2 - 1 and i in (2, 5):
                c = pal["glow"][3]
                glow = pal["glow"][3]
            if j == h // 2 and i in (2, 5):
                c = pal["glow"][2]
                glow = pal["glow"][2]
            if j == h - 2 and 3 <= i <= 4:
                c = (30, 30, 40)
        elif style == "bark":
            if j in (2, 3) and i in (1, 2, 5, 6):
                c = (16, 8, 6)
            if j == 3 and i in (2, 5):
                c = pal["glow"][3]
                glow = pal["glow"][3]
            if j == 5 and 2 <= i <= 5:
                c = (16, 8, 6)
            if j == 6 and i in (3, 4):
                c = (16, 8, 6)
        elif style == "hound":
            if j == 2 and i in (1, 4):
                c = pal["glow"][3]
                glow = pal["glow"][3]
            if j == h - 1 and 1 <= i <= w - 2:
                c = (20, 14, 12)
        elif style == "wisp":
            if j == 2 and i in (1, 4):
                c = (30, 10, 50)
                glow = None
            elif j == 4 and i in (2, 3):
                c = (60, 20, 90)
                glow = None
        elif style == "crawler":
            for ex, ey in ((1, 2), (2, 1), (5, 1), (6, 2), (3, 3), (4, 3)):
                if i == ex and j == ey:
                    c = pal["glow"][3]
                    glow = pal["glow"][3]
            if j >= h - 2 and i in (2, 5):
                c = (10, 6, 12)
        elif style == "glass":
            if j == 2 and i in (1, 4):
                c = pal["glow"][3]
                glow = pal["glow"][3]
            if j == 4 and 2 <= i <= 3:
                c = pick(1)

    k = {"up": 1.08, "down": 0.7}.get(face, 1.0)
    c = shade(c, k)
    return c, glow


def paint(parts, tw, th, pal):
    tex = new(tw, th)
    glow = new(tw, th)
    for c in all_cubes(parts):
        u, v = c.uv
        dx, dy, dz = c.size
        regions = [
            ("up", u + dz, v, dx, dz), ("down", u + dz + dx, v, dx, dz),
            ("right", u, v + dz, dz, dy), ("front", u + dz, v + dz, dx, dy),
            ("left", u + dz + dx, v + dz, dz, dy), ("back", u + 2 * dz + dx, v + dz, dx, dy),
        ]
        for face, x0, y0, w, h in regions:
            for i in range(w):
                for j in range(h):
                    col, g = mat_color(c.mat, face, i, j, w, h, pal)
                    if col is None:
                        continue
                    put(tex, x0 + i, y0 + j, col)
                    if g:
                        put(glow, x0 + i, y0 + j, g)
    return tex, glow


# ---------------- Java ----------------
def fmt(x):
    if abs(x - round(x)) < 1e-6:
        return f"{int(round(x))}.0F"
    return f"{x:.4f}F"


def java_part(p, parent_var, lines, counter):
    var = f"p{counter[0]}"
    counter[0] += 1
    cb = "CubeListBuilder.create()"
    for c in p.cubes:
        ox, oy, oz = c.origin
        dx, dy, dz = c.size
        cb += f".texOffs({c.uv[0]}, {c.uv[1]}).addBox({fmt(ox)}, {fmt(oy)}, {fmt(oz)}, {fmt(dx)}, {fmt(dy)}, {fmt(dz)})"
    px, py, pz, xr, yr, zr = p.pose
    if xr or yr or zr:
        pose = f"PartPose.offsetAndRotation({fmt(px)}, {fmt(py)}, {fmt(pz)}, {fmt(xr)}, {fmt(yr)}, {fmt(zr)})"
    else:
        pose = f"PartPose.offset({fmt(px)}, {fmt(py)}, {fmt(pz)})"
    lines.append(f"        PartDefinition {var} = {parent_var}.addOrReplaceChild(\"{p.name}\", {cb}, {pose});")
    for ch in p.children:
        java_part(ch, var, lines, counter)


def java_method(name, parts, tw, th):
    lines = [f"    public static LayerDefinition {name}() {{",
             "        MeshDefinition mesh = new MeshDefinition();",
             "        PartDefinition root = mesh.getRoot();"]
    counter = [0]
    for p in parts:
        java_part(p, "root", lines, counter)
    lines.append(f"        return LayerDefinition.create(mesh, {tw}, {th});")
    lines.append("    }")
    return "\n".join(lines)


# ---------------- Превью ----------------
def to_render(p):
    return {"pose": (p.pose[0], p.pose[1], p.pose[2], p.pose[3], p.pose[4], p.pose[5]),
            "cubes": [{"uv": c.uv, "origin": c.origin, "size": c.size} for c in p.cubes],
            "children": [to_render(ch) for ch in p.children]}


def preview(parts, tex, size=300, yaw=-28):
    s = Scene()
    for p in parts:
        add_entity_part(s, to_render(p), tex)
    return s.render(size, yaw=yaw, pitch=12)


# ---------------- Модели ----------------
def salt_wraith():
    Part._n = 0
    head = P("head", (0, 0, 0), Cube((-4, -8, -4), (8, 8, 8), "salt:facehollow"),
             children=[P(None, (0, 0, 0), Cube((-5, -9, -5), (10, 2, 10), "hood"),
                         Cube((-5, -7, 3), (10, 8, 2), "hood"),
                         Cube((-5, -7, -5), (1, 7, 8), "hood"), Cube((4, -7, -5), (1, 7, 8), "hood"))])
    body = P("body", (0, 0, 0), Cube((-4, 0, -2), (8, 12, 4), "robe"),
             children=[P(None, (-5, 0, 0, 0, 0, R(25)), Cube((-1, -3, -1), (2, 4, 2), "salt")),
                       P(None, (5, 0, 0, 0, 0, R(-25)), Cube((-1, -3, -1), (2, 4, 2), "salt")),
                       P(None, (0, -1, 2, R(-20), 0, 0), Cube((-1, -3, 0), (2, 3, 2), "salt"))])
    skirt = P("skirt", (0, 12, 0), Cube((-5, 0, -3), (10, 5, 6), "robe"), Cube((-4, 5, -2), (8, 3, 4), "robe"),
              children=[P(None, (-3, 8, 0, R(10), 0, 0), Cube((-1, 0, -1), (2, 3, 2), "robe")),
                        P(None, (2, 8, 1, R(-12), 0, 0), Cube((-1, 0, -1), (2, 2, 2), "robe"))])
    ra = P("right_arm", (-5, 2, 0), Cube((-2, -2, -1.5), (3, 12, 3), "robe"),
           children=[P(None, (-0.5, 10, 0), Cube((-1.5, 0, -1.5), (3, 2, 3), "salt"),
                       Cube((-1.5, 2, -1.5), (1, 2, 1), "salt"), Cube((0.5, 2, -1.5), (1, 3, 1), "salt"))])
    la = P("left_arm", (5, 2, 0), Cube((-1, -2, -1.5), (3, 12, 3), "robe"),
           children=[P(None, (0.5, 10, 0), Cube((-1.5, 0, -1.5), (3, 2, 3), "salt"),
                       Cube((-1.5, 2, -1.5), (1, 3, 1), "salt"), Cube((0.5, 2, -1.5), (1, 2, 1), "salt"))])
    pal = {"robe": ramp("#6d6a62", "#8e8a80", "#aaa59a", "#c4bfb3", "#dcd8ce"),
           "hood": ramp("#4e4b45", "#67635b", "#7d786e"),
           "salt": ramp("#9fb4c7", "#c9d6e0", "#e4ecf2", "#f4f8fb", "#ffffff"),
           "trim": ramp("#3e4c5c", "#526478", "#6f87a0", "#9fb4c7"),
           "glow": ramp("#1a5c8a", "#3f9fe0", "#8fd4ff", "#e6f8ff")}
    return [head, body, skirt, ra, la], pal


def resin_walker():
    Part._n = 0
    head = P("head", (0, -2, 0), Cube((-4, -7, -4), (8, 7, 8), "bark:facebark"),
             children=[P(None, (-2, -7, 0, 0, 0, R(-25)), Cube((-0.5, -6, -0.5), (1, 6, 1), "bark")),
                       P(None, (2, -7, -1, R(10), 0, R(20)), Cube((-0.5, -7, -0.5), (1, 7, 1), "bark")),
                       P(None, (0, -11, 0), Cube((-5, -3, -4), (10, 4, 8), "leaf"), Cube((-3, -5, -2), (6, 2, 5), "leaf"))])
    body = P("body", (0, -2, 0), Cube((-5, 0, -3), (10, 14, 6), "bark:resin"),
             children=[P(None, (0, 0, 0), Cube((-6, -1, -2), (3, 4, 5), "leaf"), Cube((3, 2, -3), (3, 3, 4), "leaf"))])
    ra = P("right_arm", (-6, 0, 0), Cube((-3, -2, -2), (4, 16, 4), "bark:resin"),
           children=[P(None, (-1, 14, 0), Cube((-2, 0, -2), (1, 4, 1), "bark"), Cube((0, 0, -2), (1, 5, 1), "bark"),
                       Cube((-1, 0, 1), (1, 4, 1), "bark"))])
    la = P("left_arm", (6, 0, 0), Cube((-1, -2, -2), (4, 16, 4), "bark"),
           children=[P(None, (1, 14, 0), Cube((-1, 0, -2), (1, 5, 1), "bark"), Cube((1, 0, -2), (1, 4, 1), "bark"),
                       Cube((0, 0, 1), (1, 4, 1), "bark"))])
    rl = P("right_leg", (-2.5, 12, 0), Cube((-2.5, 0, -2.5), (5, 12, 5), "bark"),
           children=[P(None, (0, 10, -2), Cube((-3, 0, -2), (6, 2, 2), "bark"))])
    ll = P("left_leg", (2.5, 12, 0), Cube((-2.5, 0, -2.5), (5, 12, 5), "bark"),
           children=[P(None, (0, 10, -2), Cube((-3, 0, -2), (6, 2, 2), "bark"))])
    pal = {"bark": ramp("#2a0e0c", "#3d1512", "#541e19", "#6c2a22", "#80362c"),
           "wood": ramp("#5c3020", "#7a4430", "#94583e"),
           "leaf": ramp("#1f4a1c", "#2c6626", "#3d8a32", "#56a843"),
           "resin": ramp("#5c0a0a", "#8f1414", "#e04a1a", "#ff8a3a"),
           "trim": ramp("#2a0e0c", "#3d1512", "#541e19", "#6c2a22"),
           "glow": ramp("#7a2a00", "#c45a00", "#ff9a20", "#ffd060"),
           "robe": ramp("#2a0e0c", "#3d1512", "#541e19", "#6c2a22", "#80362c")}
    return [head, body, ra, la, rl, ll], pal


def glassman():
    Part._n = 0
    head = P("head", (0, 0, 0), Cube((-3, -7, -3), (6, 7, 6), "glass:faceglass"),
             children=[P(None, (0, -7, 0, 0, R(45), 0), Cube((-2, -5, -2), (4, 5, 4), "glass")),
                       P(None, (0, -11, 0, 0, R(45), 0), Cube((-1, -3, -1), (2, 3, 2), "glowmat"))])
    body = P("body", (0, 0, 0), Cube((-3, 0, -2), (6, 11, 4), "glass"),
             children=[P(None, (0, 4, -2), Cube((-1.5, -1.5, -1), (3, 3, 1), "glowmat")),
                       P(None, (-4, -1, 0, 0, 0, R(30)), Cube((-1, -4, -1), (2, 5, 2), "glass")),
                       P(None, (4, -1, 0, 0, 0, R(-30)), Cube((-1, -4, -1), (2, 5, 2), "glass")),
                       P(None, (0, 2, 2, R(-30), 0, 0), Cube((-1, -5, 0), (2, 6, 2), "glass"))])
    ra = P("right_arm", (-5, 2, 0), Cube((-1, -2, -1), (2, 12, 2), "glass"))
    la = P("left_arm", (5, 2, 0), Cube((-1, -2, -1), (2, 12, 2), "glass"))
    rl = P("right_leg", (-2, 12, 0), Cube((-1, 0, -1), (2, 12, 2), "glass"))
    ll = P("left_leg", (2, 12, 0), Cube((-1, 0, -1), (2, 12, 2), "glass"))
    pal = {"glass": ramp("#5a8fa0", "#86bfcf", "#b3e3ee", "#dff7fb", "#ffffff"),
           "trim": ramp("#5a8fa0", "#86bfcf", "#b3e3ee", "#dff7fb"),
           "glow": ramp("#4a1a8a", "#7d45c9", "#c9a2ff", "#ffffff"),
           "robe": ramp("#5a8fa0", "#86bfcf", "#b3e3ee", "#dff7fb", "#ffffff")}
    return [head, body, ra, la, rl, ll], pal


WANDERER_PAL = {
    "spark": {"robe": ramp("#1c0f0c", "#2e1712", "#45221a", "#5c2e22", "#733a2a"), "hood": ramp("#140a08", "#24120e", "#331a14"),
              "trim": ramp("#5c2a08", "#8f4a10", "#d2741c", "#ffb040"), "mask": ramp("#4a2c14", "#7a4a22", "#a8703a", "#c89058"),
              "metal": ramp("#2a1a10", "#4a2c18", "#6e4224", "#8f5a30", "#b07040"), "skin": ramp("#3a2418", "#4e3020", "#6a4430", "#805438"),
              "glow": ramp("#8a2a00", "#ff6a00", "#ffb020", "#fff0a0"), "stone": ramp("#2a2020", "#3a2c28", "#4a3a34", "#5a4840", "#6e5a50")},
    "resonance": {"robe": ramp("#2a2a24", "#3c3a32", "#504c42", "#646052", "#787462"), "hood": ramp("#1e1e1a", "#2a2a24", "#383630"),
                  "trim": ramp("#3a3a3a", "#555555", "#7a7a72", "#a0a094"), "mask": ramp("#4a4a46", "#6a6a64", "#8a8a82", "#a8a8a0"),
                  "metal": ramp("#2e2e2e", "#454545", "#606060", "#7a7a7a", "#949494"), "skin": ramp("#3a3a34", "#4e4e46", "#62625a", "#76766c"),
                  "glow": ramp("#1a6a3a", "#3ac070", "#90ffb0", "#e8fff0"), "stone": ramp("#3a3a40", "#4e4e56", "#64646c", "#7a7a84", "#90909a")},
    "convergence": {"robe": ramp("#120a24", "#1e1238", "#2c1a50", "#3c2468", "#4c2e80"), "hood": ramp("#0a0614", "#140c24", "#1e1232"),
                    "trim": ramp("#5a5a70", "#80809a", "#b0b0c8", "#e0e0f0"), "mask": ramp("#30305a", "#50508a", "#8080b8", "#b0b0e0"),
                    "metal": ramp("#2a2a40", "#40405c", "#5a5a7a", "#7a7a9a", "#a0a0c0"), "skin": ramp("#2a2040", "#3a2c58", "#4c3a70", "#5c4a84"),
                    "glow": ramp("#3a1a8a", "#8a45ff", "#c9a2ff", "#f4e8ff"), "stone": ramp("#1a1430", "#261e44", "#322858", "#40346c", "#504080")},
    "heart": {"robe": ramp("#1a0612", "#2e0a20", "#46102e", "#5e183e", "#78204e"), "hood": ramp("#10040c", "#1e0816", "#2c0c20"),
              "trim": ramp("#5c3d0a", "#8a5e14", "#bf8a24", "#ffe080"), "mask": ramp("#6a5020", "#9a7830", "#d0a848", "#f0d070"),
              "metal": ramp("#3a2a0a", "#5c4214", "#8a6420", "#b88a30", "#e0b848"), "skin": ramp("#3a1a28", "#4e2436", "#663046", "#7a3c56"),
              "glow": ramp("#8a0a5a", "#ff3aa8", "#ff9ad8", "#fff0fa"), "stone": ramp("#2a0a1e", "#3e1030", "#541842", "#6c2254", "#842c66")},
}


def wanderer(variant):
    Part._n = 0
    halo_cubes = [Cube((-5, 0, -5), (10, 1, 1), "trim"), Cube((-5, 0, 4), (10, 1, 1), "trim"),
                  Cube((-5, 0, -4), (1, 1, 8), "trim"), Cube((4, 0, -4), (1, 1, 8), "trim")]
    halo_children = []
    if variant == "spark":
        for k, (x, z) in enumerate(((-4, -5), (3, -5), (-4, 4), (3, 4), (-0.5, -5))):
            halo_children.append(P(None, (x + 0.5, 0, z + 0.5), Cube((-0.5, -4 + (k % 2), -0.5), (1, 4 - (k % 2), 1), "flame")))
    if variant == "heart":
        halo_cubes = [Cube((-6, 0, -6), (12, 1, 1), "trim"), Cube((-6, 0, 5), (12, 1, 1), "trim"),
                      Cube((-6, 0, -5), (1, 1, 10), "trim"), Cube((5, 0, -5), (1, 1, 10), "trim")]
        for x, z in ((-6, -6), (5, -6), (-6, 5), (5, 5)):
            halo_children.append(P(None, (x + 0.5, -1, z + 0.5, 0, R(45), 0), Cube((-1, -2, -1), (2, 2, 2), "glowmat")))
    halo = P("halo", (0, -12, 0), *halo_cubes, children=halo_children)
    head = P("head", (0, 0, 0), Cube((-4, -8, -4), (8, 8, 8), "robe:facemask"),
             children=[P(None, (0, 0, 0), Cube((-5, -9, -5), (10, 1, 10), "hood"), Cube((-5, -8, 4), (10, 9, 1), "hood"),
                         Cube((-5, -8, -5), (1, 9, 9), "hood"), Cube((4, -8, -5), (1, 9, 9), "hood")), halo])
    paul = "stone" if variant == "resonance" else "metal"
    pw = 5 if variant == "resonance" else 4
    body = P("body", (0, 0, 0), Cube((-4, 0, -2.5), (8, 12, 5), "robe:runes"),
             children=[P(None, (0, 0, 0), Cube((-4.5, 8, -3), (9, 1, 6), "trim")),
                       P(None, (-5, 0, 0, 0, 0, R(-12)), Cube((-pw + 1, -2, -3.5), (pw, 3, 7), paul)),
                       P(None, (5, 0, 0, 0, 0, R(12)), Cube((-1, -2, -3.5), (pw, 3, 7), paul))])
    if variant == "heart":
        body.add(P("wing_right", (-2, 1, 2.5, 0, R(-35), 0), Cube((-14, -8, 0), (14, 18, 1), "trim")),
                 P("wing_left", (2, 1, 2.5, 0, R(35), 0), Cube((0, -8, 0), (14, 18, 1), "trim")))
    skirt = P("skirt", (0, 12, 0), Cube((-5, 0, -3.5), (10, 11, 7), "skirt"),
              children=[P(None, (0, 0, -3.5), Cube((-1, 0, -0.5), (2, 10, 1), "trim"))])
    staff = P("staff", (0, 10, 0), Cube((-0.5, -26, -0.5), (1, 34, 1), "metal"),
              children=[P(None, (0, -28, 0, 0, R(45), 0), Cube((-2, -2, -2), (4, 4, 4), "glowmat")),
                        P(None, (0, -26, 0), Cube((-2, -1, -0.5), (1, 3, 1), "trim"), Cube((1, -1, -0.5), (1, 3, 1), "trim"))])
    ra = P("right_arm", (-6, 2, 0), Cube((-2, -2, -2), (4, 10, 4), "robe"),
           children=[P(None, (0, 8, 0), Cube((-1.5, 0, -1.5), (3, 4, 3), "skin")), staff])
    la = P("left_arm", (6, 2, 0), Cube((-2, -2, -2), (4, 10, 4), "robe"),
           children=[P(None, (0, 8, 0), Cube((-1.5, 0, -1.5), (3, 4, 3), "skin"))])
    orbit_cubes = [Cube((9, -1, -1), (2, 3, 2), "glowmat"), Cube((-11, -2, -1), (2, 3, 2), "glowmat"), Cube((-1, 1, 9), (2, 2, 2), "glowmat")]
    if variant == "resonance":
        orbit_cubes = [Cube((9, -2, -2), (4, 3, 3), "stone"), Cube((-12, -1, -1), (3, 3, 3), "stone"),
                       Cube((-2, 0, 9), (3, 2, 3), "stone"), Cube((-2, -3, -12), (3, 3, 3), "stone")]
    if variant == "convergence":
        orbit_cubes += [Cube((-1, -4, -11), (2, 2, 2), "glowmat"), Cube((7, 3, 7), (2, 2, 2), "glowmat")]
    orbit = P("orbit", (0, 4, 0), *orbit_cubes)
    return [head, body, skirt, ra, la, orbit], WANDERER_PAL[variant]


def ash_hound():
    Part._n = 0
    body = P("body", (0, 13, 0), Cube((-3, -3, -6), (6, 6, 12), "fur:embers"),
             children=[P(None, (0, 0, 0), Cube((-4, -4, -7), (8, 7, 5), "fur:dark"))])
    head = P("head", (0, 11, -7), Cube((-3, -3, -5), (6, 6, 5), "fur:facehound"),
             children=[P(None, (0, 0, 0), Cube((-1.5, 0, -8), (3, 3, 3), "fur"), Cube((-1.5, 3, -7.5), (3, 1, 2), "bone"),
                         Cube((-3, -5, -3), (2, 2, 1), "fur:dark"), Cube((1, -5, -3), (2, 2, 1), "fur:dark"))])
    legs = [P("leg_fr", (-2, 16, -4), Cube((-1, 0, -1), (2, 8, 2), "fur")),
            P("leg_fl", (2, 16, -4), Cube((-1, 0, -1), (2, 8, 2), "fur")),
            P("leg_br", (-2, 16, 4), Cube((-1, 0, -1), (2, 8, 2), "fur")),
            P("leg_bl", (2, 16, 4), Cube((-1, 0, -1), (2, 8, 2), "fur"))]
    tail = P("tail", (0, 11, 6, R(35), 0, 0), Cube((-1, -1, 0), (2, 2, 7), "fur"),
             children=[P(None, (0, 0, 7), Cube((-1, -1, 0), (2, 2, 2), "flame"))])
    pal = {"fur": ramp("#1e1c1a", "#2e2b28", "#423e3a", "#56514c", "#6a645e"),
           "bone": ramp("#8a8070", "#b0a690", "#d8d0bc", "#f0eadc"),
           "glow": ramp("#8a2a00", "#ff6a00", "#ffb020", "#fff0a0"),
           "trim": ramp("#1e1c1a", "#2e2b28", "#423e3a", "#56514c"),
           "robe": ramp("#1e1c1a", "#2e2b28", "#423e3a", "#56514c", "#6a645e")}
    return [body, head, tail] + legs, pal


def ether_wisp():
    Part._n = 0
    body = P("body", (0, 14, 0), Cube((-3, -3, -3), (6, 6, 6), "glowmat:facewisp"),
             children=[P(None, (0, 0, 0, R(45), R(45), 0), Cube((-1, -6, -1), (2, 3, 2), "crystal"), Cube((-1, 3, -1), (2, 3, 2), "crystal")),
                       P(None, (0, 0, 0, 0, R(45), R(45)), Cube((-6, -1, -1), (3, 2, 2), "crystal"), Cube((3, -1, -1), (3, 2, 2), "crystal"))])
    skirt = P("skirt", (0, 17, 0), Cube((-2, 0, -2), (4, 3, 4), "glowmat"), Cube((-1, 3, -1), (2, 3, 2), "glowmat"),
              children=[P(None, (0, 6, 0), Cube((-0.5, 0, -0.5), (1, 2, 1), "glowmat"))])
    orbit = P("orbit", (0, 14, 0), Cube((6, -1, -0.5), (1, 1, 1), "glowmat"), Cube((-7, 1, -0.5), (1, 1, 1), "glowmat"),
              Cube((-0.5, -2, 6), (1, 1, 1), "glowmat"))
    pal = {"glow": ramp("#5a2d9a", "#a06ae6", "#d6b8ff", "#ffffff"),
           "crystal": ramp("#3d1c6b", "#5a2d9a", "#7d45c9", "#a06ae6", "#c9a2ff"),
           "trim": ramp("#3d1c6b", "#5a2d9a", "#7d45c9", "#a06ae6"),
           "robe": ramp("#3d1c6b", "#5a2d9a", "#7d45c9", "#a06ae6", "#c9a2ff")}
    return [body, skirt, orbit], pal


def scar_crawler():
    Part._n = 0
    body = P("body", (0, 15, 0), Cube((-3, -3, -3), (6, 6, 6), "chitin"))
    abdomen = P("abdomen", (0, 15, 9), Cube((-5, -4, -6), (10, 8, 12), "chitin:runes"),
                children=[P(None, (0, -4, -2, R(-20), 0, R(15)), Cube((-1, -4, -1), (2, 4, 2), "crystal")),
                          P(None, (-2, -4, 2, R(15), 0, R(-25)), Cube((-1, -3, -1), (2, 3, 2), "crystal")),
                          P(None, (3, -4, 3, R(25), 0, R(20)), Cube((-1, -5, -1), (2, 5, 2), "crystal"))])
    head = P("head", (0, 15, -3), Cube((-4, -4, -8), (8, 8, 8), "chitin:facecrawler"),
             children=[P(None, (0, 2, -8), Cube((-3, 0, -2), (2, 3, 2), "bone"), Cube((1, 0, -2), (2, 3, 2), "bone"))])
    legs = []
    yr = [R(45), R(-45), R(22.5), R(-22.5), R(-22.5), R(22.5), R(-45), R(45)]
    zs = [2, 2, 1, 1, 0, 0, -1, -1]
    for i in range(8):
        right = i % 2 == 0
        zr = R(-45 if i in (0, 6) else -33) if right else R(45 if i in (1, 7) else 33)
        x0 = -15 if right else -1
        legs.append(P(f"leg{i}", (-4 if right else 4, 15, zs[i], 0, yr[i], zr), Cube((x0, -1, -1), (16, 2, 2), "chitin")))
    pal = {"chitin": ramp("#120814", "#1e0e22", "#2c1532", "#3d1f44", "#4e2a56"),
           "crystal": ramp("#4a0a36", "#7a1458", "#b0207e", "#e04aa8", "#ff9ad8"),
           "bone": ramp("#4a3a40", "#6a5660", "#8a7480", "#a8909c"),
           "glow": ramp("#8a0a5a", "#ff3aa8", "#ff9ad8", "#fff0fa"),
           "trim": ramp("#120814", "#1e0e22", "#2c1532", "#3d1f44"),
           "robe": ramp("#120814", "#1e0e22", "#2c1532", "#3d1f44", "#4e2a56")}
    return [body, abdomen, head] + legs, pal


MODELS = [
    ("ash_hound", "ashHound", ash_hound, 64),
    ("ether_wisp", "etherWisp", ether_wisp, 64),
    ("scar_crawler", "scarCrawler", scar_crawler, 128),
    ("salt_wraith", "saltWraith", salt_wraith, 64),
    ("resin_walker", "resinWalker", resin_walker, 64),
    ("glassman", "glassman", glassman, 64),
    ("wanderer_spark", "wandererSpark", lambda: wanderer("spark"), 128),
    ("wanderer_resonance", "wandererResonance", lambda: wanderer("resonance"), 128),
    ("wanderer_convergence", "wandererConvergence", lambda: wanderer("convergence"), 128),
    ("wanderer_heart", "wandererHeart", lambda: wanderer("heart"), 128),
]


def main():
    os.makedirs(ROOT, exist_ok=True)
    for f in os.listdir(ROOT):
        os.remove(os.path.join(ROOT, f))
    methods, consts = [], []
    previews = []
    for name, method, fn, width in MODELS:
        parts, pal = fn()
        th = pack(parts, width)
        tex, glow = paint(parts, width, th, pal)
        tex.save(f"{ROOT}/{name}.png")
        glow.save(f"{ROOT}/{name}_glow.png")
        methods.append(java_method(method, parts, width, th))
        consts.append(f"    public static final ModelLayerLocation {name.upper()} = new ModelLayerLocation(AetherWastes.id(\"{name}\"), \"main\");")
        previews.append((name, preview(parts, tex), preview(parts, tex, yaw=150)))
    java = """package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Сгенерировано из описаний моделей (art/entityart.py). Не править вручную. */
public final class ModModels {
""" + "\n".join(consts) + "\n\n" + "\n\n".join(methods) + """

    private ModModels() {}
}
"""
    with open(JAVA, "w", encoding="utf-8") as f:
        f.write(java)
    cell = 300
    sheet = Image.new("RGBA", (cell * 4, cell * ((len(previews) + 1) // 2)), (52, 56, 64, 255))
    d = ImageDraw.Draw(sheet)
    for k, (name, a, b) in enumerate(previews):
        x, y = (k % 2) * cell * 2, (k // 2) * cell
        sheet.alpha_composite(a, (x, y))
        sheet.alpha_composite(b, (x + cell, y))
        d.text((x + 8, y + 8), name, fill=(240, 240, 240, 255))
    sheet.save(os.path.join(HERE, "mobs_preview.png"))
    print("mobs ok")


if __name__ == "__main__":
    main()
