"""Простой рендер превью: модели блоков (JSON-элементы) и модели мобов (кубоиды с box-UV)."""
import math
from PIL import Image, ImageDraw

NORMALS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
# В пространстве модели моба (y вниз): верх = -y, «south» здесь = перед (-z).
ENT_NORMALS = {"up": (0, -1, 0), "down": (0, 1, 0), "south": (0, 0, -1), "north": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}
FACE_LIGHT = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.62, "west": 0.62}


def rot_axis(p, axis, ang, origin):
    a = math.radians(ang)
    x, y, z = p[0] - origin[0], p[1] - origin[1], p[2] - origin[2]
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


class Scene:
    def __init__(self):
        self.quads = []  # (pts3d, rgba, light)

    def add(self, pts, color, light, normal):
        if color[3] == 0:
            return
        self.quads.append((pts, color, light, normal))

    def render(self, size=256, yaw=-45, pitch=30, scale=None, center=None, bg=None):
        if not self.quads:
            return Image.new("RGBA", (size, size), bg or (0, 0, 0, 0))
        cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
        cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

        def proj(p):
            x, y, z = p
            x, z = x * cy + z * sy, -x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            return (x, -y, z)

        proj_quads = []
        for pts, col, light, nrm in self.quads:
            pp = [proj(p) for p in pts]
            nz = proj(nrm)[2]
            depth = sum(p[2] for p in pp) / 4
            proj_quads.append((pp, col, light, depth, -nz))
        xs = [p[0] for q in proj_quads for p in q[0]]
        ys = [p[1] for q in proj_quads for p in q[0]]
        minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
        span = max(maxx - minx, maxy - miny)
        sc = scale or (size * 0.86 / span)
        ox = size / 2 - (minx + maxx) / 2 * sc
        oy = size / 2 - (miny + maxy) / 2 * sc
        ss = 3
        img = Image.new("RGBA", (size * ss, size * ss), bg or (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        proj_quads.sort(key=lambda q: q[3])
        for pp, col, light, depth, cross in proj_quads:
            if cross > 0:
                continue
            c = tuple(max(0, min(255, int(col[i] * light))) for i in range(3)) + (255,)
            poly = [((p[0] * sc + ox) * ss, (p[1] * sc + oy) * ss) for p in pp]
            d.polygon(poly, fill=c, outline=c)
        return img.resize((size, size), Image.LANCZOS)


# ---------------- Модели блоков ----------------

def _face_point(face, u, v, f, t):
    """Авто-UV блока: (u,v) в 0..16 → точка на грани. f,t — from/to элемента."""
    if face == "up":
        return (u, t[1], v)
    if face == "down":
        return (u, f[1], 16 - v)
    if face == "north":
        return (16 - u, 16 - v, f[2])
    if face == "south":
        return (u, 16 - v, t[2])
    if face == "west":
        return (f[0], 16 - v, u)
    if face == "east":
        return (t[0], 16 - v, 16 - u)


def _auto_uv(face, f, t):
    if face == "up":
        return [f[0], f[2], t[0], t[2]]
    if face == "down":
        return [f[0], 16 - t[2], t[0], 16 - f[2]]
    if face == "north":
        return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]]
    if face == "south":
        return [f[0], 16 - t[1], t[0], 16 - f[1]]
    if face == "west":
        return [f[2], 16 - t[1], t[2], 16 - f[1]]
    if face == "east":
        return [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]]


def add_block_model(scene, model, textures, offset=(0, 0, 0)):
    """model — dict с 'textures' и 'elements'; textures — {имя: PIL}."""
    texmap = model.get("textures", {})

    def resolve(ref):
        while ref.startswith("#"):
            ref = texmap[ref[1:]]
        return textures[ref.split(":")[-1]]

    for el in model["elements"]:
        f, t = el["from"], el["to"]
        rot = el.get("rotation")
        for face, fd in el["faces"].items():
            tex = resolve(fd["texture"])
            W, H = tex.size
            frames = H // W if H > W else 1
            texh = H // frames
            auto = _auto_uv(face, f, t)
            uv = fd.get("uv", auto)
            # проходим по текселям в области uv
            u0, v0, u1, v1 = uv
            su = 1 if u1 >= u0 else -1
            sv = 1 if v1 >= v0 else -1
            nu = max(1, int(round(abs(u1 - u0) * W / 16)))
            nv = max(1, int(round(abs(v1 - v0) * texh / 16)))
            for i in range(nu):
                for j in range(nv):
                    px = int(min(u0, u1) * W / 16) + i
                    py = int(min(v0, v1) * texh / 16) + j
                    col = tex.getpixel((px % W, py % texh))
                    if col[3] < 128:
                        continue
                    # доля в области → координаты авто-UV
                    a0, a1 = i / nu, (i + 1) / nu
                    b0, b1 = j / nv, (j + 1) / nv
                    if su < 0:
                        a0, a1 = 1 - a1, 1 - a0
                    if sv < 0:
                        b0, b1 = 1 - b1, 1 - b0
                    au = lambda a: auto[0] + (auto[2] - auto[0]) * a
                    av = lambda b: auto[1] + (auto[3] - auto[1]) * b
                    corners = [(au(a0), av(b0)), (au(a1), av(b0)), (au(a1), av(b1)), (au(a0), av(b1))]
                    pts = [_face_point(face, cu, cv, f, t) for cu, cv in corners]
                    if rot:
                        pts = [rot_axis(p, rot["axis"], rot["angle"], rot["origin"]) for p in pts]
                    pts = [(p[0] + offset[0] - 8, p[1] + offset[1], p[2] + offset[2] - 8) for p in pts]
                    n = NORMALS[face]
                    if rot:
                        n = rot_axis(n, rot["axis"], rot["angle"], (0, 0, 0))
                    scene.add(pts, col, FACE_LIGHT[face], n)


# ---------------- Модели мобов ----------------

def _rotZYX(p, xr, yr, zr):
    x, y, z = p
    # Minecraft: rotationZYX(z, y, x) → применяется X, затем Y, затем Z
    c, s = math.cos(xr), math.sin(xr)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(yr), math.sin(yr)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(zr), math.sin(zr)
    x, y = x * c - y * s, x * s + y * c
    return (x, y, z)


def add_entity_part(scene, part, tex, chain=()):
    """part: {'pose':(px,py,pz,xr,yr,zr), 'cubes':[...], 'children':[...]}.
    cube: {'uv':(u,v), 'origin':(x,y,z), 'size':(dx,dy,dz)}. Пространство модели: y вниз, лицом к -z."""
    chain = chain + (part["pose"],)
    for cube in part["cubes"]:
        u, v = cube["uv"]
        x0, y0, z0 = cube["origin"]
        dx, dy, dz = cube["size"]
        x1, y1, z1 = x0 + dx, y0 + dy, z0 + dz
        faces = []
        # (texU, texV, w, h, точка(i,j)→3D угол, свет)
        faces.append((u + dz, v, dx, dz, lambda i, j: (x0 + i, y0, z1 - j), "up"))
        faces.append((u + dz, v + dz, dx, dy, lambda i, j: (x0 + i, y0 + j, z0), "south"))   # перед модели → к зрителю
        faces.append((u + dz + dx, v + dz, dz, dy, lambda i, j: (x1, y0 + j, z0 + i), "east"))
        faces.append((u, v + dz, dz, dy, lambda i, j: (x0, y0 + j, z1 - i), "west"))
        faces.append((u + 2 * dz + dx, v + dz, dx, dy, lambda i, j: (x1 - i, y0 + j, z1), "north"))
        faces.append((u + dz + dx, v, dx, dz, lambda i, j: (x0 + i, y1, z0 + j), "down"))
        for (tu, tv, w, h, fn, name) in faces:
            for i in range(int(math.ceil(w))):
                for j in range(int(math.ceil(h))):
                    col = tex.getpixel((min(tex.width - 1, int(tu) + i), min(tex.height - 1, int(tv) + j)))
                    if col[3] < 128:
                        continue
                    i1, j1 = min(w, i + 1), min(h, j + 1)
                    pts = [fn(i, j), fn(i1, j), fn(i1, j1), fn(i, j1)]
                    out = []
                    for p in pts:
                        for (px, py, pz, xr, yr, zr) in reversed(chain):
                            p = _rotZYX(p, xr, yr, zr)
                            p = (p[0] + px, p[1] + py, p[2] + pz)
                        out.append((p[0], -p[1], -p[2]))
                    n = ENT_NORMALS[name]
                    for (px, py, pz, xr, yr, zr) in reversed(chain):
                        n = _rotZYX(n, xr, yr, zr)
                    n = (n[0], -n[1], -n[2])
                    scene.add(out, col, FACE_LIGHT[name], n)
    for ch in part.get("children", []):
        add_entity_part(scene, ch, tex, chain)
