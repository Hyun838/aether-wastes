"""OBJ-модели Эфирных Пустошей 1.3: кристалл, кольцо рун, сфера печати.

Модели пишутся в models/block/obj/*.obj с библиотекой материалов *.mtl и грузятся
загрузчиком NeoForge (loader "neoforge:obj"). Координаты — в блоках с центром в нуле:
рендерер блок-сущности сам ставит модель на место, вращает и пульсирует ею.
Нормали плоские (по грани), поэтому грани кристалла по-разному освещаются.
Текстуры светлые и почти бесцветные: цвет задаётся тинтом при рендере (у печатей разный).
"""
import math
import os
import random
from PIL import Image

from texlib import clamp, Noise

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'aetherwastes')
OBJ_DIR = os.path.join(ROOT, 'models', 'block', 'obj')
TEX_DIR = os.path.join(ROOT, 'textures', 'block', 'obj')


class Mesh:
    def __init__(self, material):
        self.material = material
        self.v, self.vt, self.vn, self.f = [], [], [], []
        self.center = [0.0, 0.0, 0.0]

    def face(self, pts, uvs, want=None):
        """Многоугольник (3 или 4 вершины) с плоской нормалью.

        Порядок вершин выправляется так, чтобы нормаль смотрела по want (по умолчанию —
        от центра примитива self.center к центру грани): грань видна снаружи, изнутри отсекается.
        """
        def normal(p):
            a, b, c = p[0], p[1], p[2]
            u = [b[i] - a[i] for i in range(3)]
            w = [c[i] - a[i] for i in range(3)]
            return [u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]]
        if want is None:
            cen = [sum(p[i] for p in pts) / len(pts) for i in range(3)]
            want = [cen[i] - self.center[i] for i in range(3)]
        n = normal(pts)
        if sum(n[i] * want[i] for i in range(3)) < 0:
            pts, uvs = pts[::-1], uvs[::-1]
            n = normal(pts)
        ln = math.sqrt(sum(x * x for x in n)) or 1.0
        self.vn.append([x / ln for x in n])
        ni = len(self.vn)
        idx = []
        for p, t in zip(pts, uvs):
            self.v.append(p)
            self.vt.append(t)
            idx.append((len(self.v), len(self.vt), ni))
        self.f.append(idx)

    def write(self, name):
        os.makedirs(OBJ_DIR, exist_ok=True)
        with open(os.path.join(OBJ_DIR, name + '.mtl'), 'w') as m:
            m.write(f'newmtl {self.material}\nKa 0.8 0.8 0.8\nKd 1 1 1\nneoforge_TintIndex 0\nmap_Kd #{self.material}\nd 1\n')
        with open(os.path.join(OBJ_DIR, name + '.obj'), 'w') as o:
            o.write(f'# Aether Wastes 1.3 — {name}, {len(self.f)} faces\n')
            o.write(f'mtllib {name}.mtl\no {name}\n')
            for p in self.v:
                o.write('v %.5f %.5f %.5f\n' % tuple(p))
            for t in self.vt:
                o.write('vt %.5f %.5f\n' % tuple(t))
            for n in self.vn:
                o.write('vn %.5f %.5f %.5f\n' % tuple(n))
            o.write(f'usemtl {self.material}\n')
            for face in self.f:
                o.write('f ' + ' '.join(f'{a}/{b}/{c}' for a, b, c in face) + '\n')
        return len(self.f)


def _rot(p, ax, ay, az):
    x, y, z = p
    c, s = math.cos(ax), math.sin(ax)
    y, z = y * c - z * s, y * s + z * c
    c, s = math.cos(ay), math.sin(ay)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(az), math.sin(az)
    x, y = x * c - y * s, x * s + y * c
    return [x, y, z]


def crystal(mesh, r, y0, y1, tip_up, tip_down, sides=6, tilt=(0, 0, 0), offset=(0, 0, 0), twist=0.0):
    """Бипирамида с призмой посередине. UV: колонна грани по u, высота по v."""
    def tr(p):
        q = _rot(p, *tilt)
        return [q[0] + offset[0], q[1] + offset[1], q[2] + offset[2]]

    mesh.center = [offset[0], offset[1] + (y0 + y1) / 2, offset[2]]
    ring0 = [[r * math.cos(2 * math.pi * i / sides + twist), y0, r * math.sin(2 * math.pi * i / sides + twist)] for i in range(sides)]
    ring1 = [[p[0], y1, p[2]] for p in ring0]
    top = [0, y1 + tip_up, 0]
    bot = [0, y0 - tip_down, 0]
    for i in range(sides):
        j = (i + 1) % sides
        u0, u1 = i / sides, (i + 1) / sides
        # бок призмы (снаружи — против часовой при взгляде снаружи)
        mesh.face([tr(ring0[i]), tr(ring1[i]), tr(ring1[j]), tr(ring0[j])], [[u0, 0.3], [u0, 0.75], [u1, 0.75], [u1, 0.3]])
        mesh.face([tr(ring1[i]), tr(top), tr(ring1[j])], [[u0, 0.75], [(u0 + u1) / 2, 1.0], [u1, 0.75]])
        mesh.face([tr(ring0[j]), tr(bot), tr(ring0[i])], [[u1, 0.3], [(u0 + u1) / 2, 0.0], [u0, 0.3]])


def ring(mesh, radius, height, thick, segments=32, repeats=4):
    """Кольцо-лента: внешняя и внутренняя поверхности и торцы."""
    rin = radius - thick
    per = segments // repeats
    for i in range(segments):
        a0 = 2 * math.pi * i / segments
        a1 = 2 * math.pi * (i + 1) / segments
        u0, u1 = (i % per) / per, (i % per + 1) / per
        o0 = [radius * math.cos(a0), 0, radius * math.sin(a0)]
        o1 = [radius * math.cos(a1), 0, radius * math.sin(a1)]
        n0 = [rin * math.cos(a0), 0, rin * math.sin(a0)]
        n1 = [rin * math.cos(a1), 0, rin * math.sin(a1)]
        up = lambda p, h: [p[0], h, p[2]]
        h0, h1 = -height / 2, height / 2
        rad = [math.cos((a0 + a1) / 2), 0, math.sin((a0 + a1) / 2)]
        mesh.face([up(o0, h0), up(o1, h0), up(o1, h1), up(o0, h1)], [[u0, 0.75], [u1, 0.75], [u1, 0.25], [u0, 0.25]], rad)
        mesh.face([up(n1, h0), up(n0, h0), up(n0, h1), up(n1, h1)], [[u1, 0.75], [u0, 0.75], [u0, 0.25], [u1, 0.25]], [-x for x in rad])
        mesh.face([up(o0, h1), up(o1, h1), up(n1, h1), up(n0, h1)], [[u0, 0.2], [u1, 0.2], [u1, 0.0], [u0, 0.0]], [0, 1, 0])
        mesh.face([up(n0, h0), up(n1, h0), up(o1, h0), up(o0, h0)], [[u0, 1.0], [u1, 1.0], [u1, 0.8], [u0, 0.8]], [0, -1, 0])


def icosphere(mesh, r, subdiv=1):
    t = (1 + 5 ** 0.5) / 2
    vs = [[-1, t, 0], [1, t, 0], [-1, -t, 0], [1, -t, 0], [0, -1, t], [0, 1, t], [0, -1, -t], [0, 1, -t],
          [t, 0, -1], [t, 0, 1], [-t, 0, -1], [-t, 0, 1]]
    fs = [(0, 11, 5), (0, 5, 1), (0, 1, 7), (0, 7, 10), (0, 10, 11), (1, 5, 9), (5, 11, 4), (11, 10, 2), (10, 7, 6),
          (7, 1, 8), (3, 9, 4), (3, 4, 2), (3, 2, 6), (3, 6, 8), (3, 8, 9), (4, 9, 5), (2, 4, 11), (6, 2, 10),
          (8, 6, 7), (9, 8, 1)]
    norm = lambda p: [x / math.sqrt(sum(c * c for c in p)) for x in p]
    vs = [norm(v) for v in vs]
    for _ in range(subdiv):
        nf = []
        cache = {}

        def mid(a, b):
            k = (min(a, b), max(a, b))
            if k not in cache:
                vs.append(norm([(vs[a][i] + vs[b][i]) / 2 for i in range(3)]))
                cache[k] = len(vs) - 1
            return cache[k]

        for a, b, c in fs:
            ab, bc, ca = mid(a, b), mid(b, c), mid(c, a)
            nf += [(a, ab, ca), (b, bc, ab), (c, ca, bc), (ab, bc, ca)]
        fs = nf
    mesh.center = [0.0, 0.0, 0.0]
    rnd = random.Random(7)
    for a, b, c in fs:
        cell = rnd.randrange(4)
        u0 = cell * 0.25
        mesh.face([[x * r for x in vs[a]], [x * r for x in vs[b]], [x * r for x in vs[c]]],
                  [[u0 + 0.02, 0.05], [u0 + 0.23, 0.05], [u0 + 0.125, 0.45]])


# ---------------- Текстуры ----------------

def crystal_texture():
    n = Noise(41)
    img = Image.new('RGBA', (32, 32))
    for y in range(32):
        for x in range(32):
            col = x % 5 == 0
            k = 0.78 + 0.32 * (1 - y / 31) * 0.5 + 0.22 * n.smooth(x, y * 2, 4, 32) + (0.18 if col else 0)
            core = 1.0 if 10 < y < 22 and (x + y) % 9 == 0 else 0
            v = clamp(220 * k + 35 * core)
            img.putpixel((x, y), (clamp(v * 0.92), clamp(v * 0.97), v, 235))
    return img


GLYPHS = [
    ["0110", "1001", "1111", "1001"], ["1110", "0100", "0100", "1110"], ["1010", "0100", "1010", "0001"],
    ["1111", "0001", "0110", "1000"], ["0100", "1110", "0100", "0100"], ["1001", "0110", "0110", "1001"],
]


def rune_texture():
    img = Image.new('RGBA', (64, 16), (0, 0, 0, 0))
    for x in range(64):
        for y in range(16):
            if y in (3, 12):
                img.putpixel((x, y), (255, 255, 255, 230))
            elif 4 <= y <= 11:
                img.putpixel((x, y), (160, 170, 200, 90))
    for gi in range(8):
        g = GLYPHS[gi % len(GLYPHS)]
        ox = gi * 8 + 2
        for yy, row in enumerate(g):
            for xx, ch in enumerate(row):
                if ch == '1':
                    img.putpixel((ox + xx, 6 + yy), (255, 255, 255, 255))
    return img


def orb_texture():
    n = Noise(9)
    img = Image.new('RGBA', (32, 16))
    for y in range(16):
        for x in range(32):
            cell = x // 8
            k = (0.82, 0.95, 1.08, 0.9)[cell] + 0.1 * n.h(x, y)
            v = clamp(215 * k)
            img.putpixel((x, y), (v, v, v, 245))
    return img


if __name__ == '__main__':
    os.makedirs(TEX_DIR, exist_ok=True)
    m = Mesh('crystal')
    crystal(m, 0.16, -0.08, 0.26, 0.24, 0.2, twist=0.3)
    for i, (dx, dz) in enumerate(((0.17, 0.06), (-0.12, 0.14), (-0.06, -0.17))):
        a = 2 * math.pi * i / 3
        crystal(m, 0.07, -0.05, 0.08, 0.12, 0.06, sides=5,
                tilt=(0.5 * math.sin(a + 1), 0, -0.5 * math.cos(a + 1)), offset=(dx, -0.12, dz))
    print('crystal', m.write('ether_crystal'))
    crystal_texture().save(os.path.join(TEX_DIR, 'ether_crystal.png'))

    m = Mesh('runes')
    ring(m, 0.62, 0.11, 0.04, segments=40, repeats=5)
    print('ring', m.write('rune_ring'))
    rune_texture().save(os.path.join(TEX_DIR, 'rune_ring.png'))

    m = Mesh('orb')
    icosphere(m, 0.3, subdiv=1)
    print('orb', m.write('seal_orb'))
    orb_texture().save(os.path.join(TEX_DIR, 'seal_orb.png'))

    import json
    for name, tex in (('ether_crystal', 'crystal'), ('rune_ring', 'runes'), ('seal_orb', 'orb')):
        model = {
            'loader': 'neoforge:obj',
            'model': f'aetherwastes:models/block/obj/{name}.obj',
            'flip_v': True,
            'automatic_culling': False,
            'shade_quads': True,
            'emissive_ambient': True,
            'textures': {tex: f'aetherwastes:block/obj/{name}', 'particle': f'aetherwastes:block/obj/{name}'},
        }
        with open(os.path.join(ROOT, 'models', 'block', 'obj', name + '.json'), 'w') as f:
            json.dump(model, f, indent=2)
