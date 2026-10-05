"""3D-модели оружия и инструментов (JSON с элементами) для Эфирных Пустошей 1.3.

Каждое оружие собирается из кубоидов в «кадре оружия»: ось длины — Y, рукоять внизу около y=0,
плоскость клинка — XY, центр по X и Z — 8. Затем вся модель наклоняется на −45° вокруг Z,
чтобы лечь на ту же диагональ, что и спрайт: так подходят стандартные трансформации
minecraft:item/handheld для руки от первого и третьего лица.

Для каждого предмета пишутся:
  textures/item/3d/<name>.png       — атлас материалов 64×64 (сетка 4×4 образцов по 16 px);
  models/item/3d/<name>.json        — объёмная модель;
  models/item/<name>.json           — neoforge:separate_transforms: в руке и на земле 3D,
                                      в инвентаре и рамке — прежний спрайт.
Светящиеся части получают shade=false и neoforge_data с полной яркостью (block/sky light 15).
"""
import json
import math
import os
import random
from PIL import Image

from texlib import hexc, clamp, Noise

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'aetherwastes')
TEX_DIR = os.path.join(ROOT, 'textures', 'item', '3d')
MODEL3D_DIR = os.path.join(ROOT, 'models', 'item', '3d')
ITEM_DIR = os.path.join(ROOT, 'models', 'item')

SLOTS = ['grip', 'metal', 'edge', 'accent', 'glow', 'dark', 'wood', 'crystal', 'glow2', 'cloth']


def _swatch(kind, base, seed):
    """Один образец материала 16×16."""
    n = Noise(seed)
    rnd = random.Random(seed)
    img = Image.new('RGBA', (16, 16))
    b = hexc(base)
    for y in range(16):
        for x in range(16):
            if kind == 'metal':          # шлифованный металл: вдоль длины полосы
                k = 0.86 + 0.22 * n.smooth(x * 4, y, 4) + 0.06 * n.h(x, y // 3)
            elif kind == 'edge':         # кромка: светлая полоса посередине
                k = 1.25 - 0.25 * abs(x - 7.5) / 7.5 + 0.05 * n.h(x, y)
            elif kind == 'grip':         # обмотка: косые витки
                stripe = ((x + y) % 6) < 4
                k = (0.95 if stripe else 0.6) + 0.08 * n.h(x, y)
            elif kind == 'accent':       # золото/латунь с бликом
                k = 0.8 + 0.4 * (1 - abs(x - y) / 16) + 0.08 * n.h(x, y)
            elif kind in ('glow', 'glow2'):  # светящееся ядро: яркий центр
                d = math.hypot(x - 7.5, y - 7.5) / 10.6
                k = 1.25 - 0.45 * d + 0.06 * n.h(x, y)
            elif kind == 'dark':
                k = 0.7 + 0.25 * n.fbm(x, y)
            elif kind == 'wood':         # волокна вдоль длины
                k = 0.75 + 0.3 * n.smooth(x * 3, y * 0.5, 3) + 0.05 * n.h(x, y)
            elif kind == 'crystal':      # грани кристалла: диагональные плоскости
                facet = ((x * 3 + y * 2) // 9) % 3
                k = (0.8, 1.05, 1.3)[facet] + 0.05 * n.h(x, y)
            else:                        # ткань
                k = 0.78 + 0.18 * ((x // 2 + y // 2) % 2) + 0.06 * n.h(x, y)
            img.putpixel((x, y), (clamp(b[0] * k), clamp(b[1] * k), clamp(b[2] * k), 255))
    if kind in ('glow', 'glow2'):  # мелкие искры
        for _ in range(5):
            img.putpixel((rnd.randrange(16), rnd.randrange(16)), (255, 255, 255, 255))
    return img


class Weapon:
    def __init__(self, name, mats, scale=1.0):
        self.name = name
        self.mats = mats          # slot -> hex
        self.scale = scale        # множитель трансформаций в руке
        self.elements = []
        self.emissive = {'glow', 'glow2'}

    # ---------- геометрия ----------
    def box(self, x0, y0, z0, x1, y1, z1, mat, glow=None):
        if mat not in self.mats:
            raise KeyError(f'{self.name}: no material {mat}')
        glow = (mat in self.emissive) if glow is None else glow
        self.elements.append((tuple(sorted((x0, x1))), tuple(sorted((y0, y1))), tuple(sorted((z0, z1))), mat, glow))
        return self

    def cbox(self, cx, y0, cz, w, h, d, mat, glow=None):
        """Кубоид, центрированный по X и Z."""
        return self.box(cx - w / 2, y0, cz - d / 2, cx + w / 2, y0 + h, cz + d / 2, mat, glow)

    # ---------- типовые узлы ----------
    def handle(self, y0, length, w=1.6, mat='grip'):
        self.cbox(8, y0, 8, w, length, w, mat)
        return self

    def pommel(self, y0, size=2.4, mat='accent', gem=None):
        self.cbox(8, y0, 8, size, 1.6, size, mat)
        if gem:
            self.cbox(8, y0 - 0.6, 8, size * 0.5, 0.7, size * 0.5, gem)
        return self

    def guard(self, y0, width, mat='accent', depth=2.6, h=1.4, gem=None, horns=True):
        self.cbox(8, y0, 8, width, h, depth, mat)
        if horns:
            self.cbox(8 - width / 2 + 0.5, y0 + h, 8, 1.0, 1.0, depth * 0.7, mat)
            self.cbox(8 + width / 2 - 0.5, y0 + h, 8, 1.0, 1.0, depth * 0.7, mat)
        if gem:
            self.cbox(8, y0 + 0.2, 8, 1.4, 1.0, depth + 0.5, gem)
        return self

    def blade(self, y0, length, width, thick=1.0, mat='metal', edge='edge', fuller=None, taper=5):
        """Клинок: тело, две кромки, дол (светящийся желоб) и сужающееся остриё."""
        body = length - taper
        self.cbox(8, y0, 8, width, body, thick, mat)
        self.cbox(8 - width / 2 - 0.35, y0 + 0.5, 8, 0.7, body - 0.5, thick * 0.55, edge)
        self.cbox(8 + width / 2 + 0.35, y0 + 0.5, 8, 0.7, body - 0.5, thick * 0.55, edge)
        if fuller:
            self.cbox(8, y0 + 0.8, 8, max(0.5, width * 0.28), body - 1.2, thick + 0.12, fuller)
        w = width
        y = y0 + body
        steps = max(2, int(taper))
        for i in range(steps):
            w2 = width * (1 - (i + 1) / (steps + 1))
            self.cbox(8, y, 8, max(0.4, w2 + 0.8), taper / steps, thick * (1 - 0.35 * i / steps), edge if i else mat)
            y += taper / steps
        return self

    def shaft(self, y0, length, w=1.4, mat='wood', bands='accent', every=6):
        self.cbox(8, y0, 8, w, length, w, mat)
        if bands:
            y = y0 + every / 2
            while y < y0 + length - 1:
                self.cbox(8, y, 8, w + 0.5, 0.7, w + 0.5, bands)
                y += every
        return self

    def orb(self, cy, r, mat):
        """Округлое ядро из трёх вложенных кубоидов (крест)."""
        self.cbox(8, cy - r, 8, r * 1.3, r * 2, r * 1.3, mat)
        self.cbox(8, cy - r * 0.65, 8, r * 2, r * 1.3, r * 1.3, mat)
        self.cbox(8, cy - r * 0.65, 8, r * 1.3, r * 1.3, r * 2, mat)
        return self

    def scythe_head(self, y_top, reach, height=2.2, mat='metal', edge='edge', glow=None, side=-1):
        """Изогнутое лезвие косы: ступенчатая дуга, уходящая вбок и вниз."""
        x = 8
        for i in range(reach):
            t = i / max(1, reach - 1)
            seg_h = height * (1 - 0.55 * t)
            drop = 3.2 * t * t
            x0 = x + side * i * 1.3
            self.box(x0, y_top - seg_h - drop, 7.5, x0 + side * 1.3, y_top - drop, 8.5, mat)
            self.box(x0, y_top - seg_h - drop - 0.6, 7.75, x0 + side * 1.3, y_top - seg_h - drop, 8.25, edge)
            if glow and i % 2 == 1:
                self.box(x0, y_top - drop - seg_h * 0.65, 7.4, x0 + side * 1.3, y_top - drop - seg_h * 0.4, 8.6, glow)
        return self

    def axe_head(self, y0, h, reach, mat='metal', edge='edge', glow=None, double=False):
        for side in ((-1, 1) if double else (-1,)):
            for i in range(reach):
                t = i / max(1, reach - 1)
                hh = h * (0.55 + 0.45 * t)
                x0 = 8 + side * (0.8 + i * 1.2)
                yc = y0 + h / 2
                self.box(x0, yc - hh / 2, 7.4, x0 + side * 1.2, yc + hh / 2, 8.6, mat)
            xe = 8 + side * (0.8 + reach * 1.2)
            self.box(xe, y0 - 0.3, 7.6, xe + side * 0.7, y0 + h + 0.3, 8.4, edge)
            if glow:
                self.box(xe - side * 0.1, y0 + 0.4, 7.5, xe + side * 0.75, y0 + h - 0.4, 8.5, glow)
        self.cbox(8, y0 - 0.4, 8, 2.4, h + 0.8, 2.4, 'dark')
        return self

    def hammer_head(self, y0, w, h, d, mat='metal', face='dark', glow=None):
        self.cbox(8, y0, 8, w, h, d, mat)
        self.cbox(8 - w / 2 - 0.4, y0 + 0.4, 8, 0.8, h - 0.8, d - 0.8, face)
        self.cbox(8 + w / 2 + 0.4, y0 + 0.4, 8, 0.8, h - 0.8, d - 0.8, face)
        self.cbox(8, y0 - 0.5, 8, w * 0.45, 0.5, d * 0.7, 'accent')
        self.cbox(8, y0 + h, 8, w * 0.45, 0.5, d * 0.7, 'accent')
        if glow:
            self.cbox(8, y0 + h * 0.3, 8, w + 0.12, h * 0.4, 0.6, glow)
            self.cbox(8, y0 + h * 0.3, 8, 0.6, h * 0.4, d + 0.12, glow)
        return self

    # ---------- вывод ----------
    def texture(self):
        img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        seed = sum(map(ord, self.name))
        for i, slot in enumerate(SLOTS):
            if slot not in self.mats:
                continue
            kind = 'glow' if slot.startswith('glow') else slot
            img.paste(_swatch(kind, self.mats[slot], seed + i * 31), ((i % 4) * 16, (i // 4) * 16))
        return img

    def uv(self, mat, a, b):
        """UV образца материала; длинные грани растягивают его вдоль длины."""
        i = SLOTS.index(mat)
        u0, v0 = (i % 4) * 4, (i // 4) * 4
        du = min(4.0, max(0.5, a))
        dv = min(4.0, max(0.5, b))
        return [round(u0, 3), round(v0, 3), round(u0 + du, 3), round(v0 + dv, 3)]

    def model(self):
        els = []
        for (x0, x1), (y0, y1), (z0, z1), mat, glow in self.elements:
            for v in (x0, x1, y0, y1, z0, z1):
                if not -16 <= v <= 32:
                    raise ValueError(f'{self.name}: element out of bounds {v}')
            sx, sy, sz = x1 - x0, y1 - y0, z1 - z0
            faces = {}
            for f, (a, b) in {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy),
                              'up': (sx, sz), 'down': (sx, sz)}.items():
                faces[f] = {'uv': self.uv(mat, a, b), 'texture': '#t'}
            el = {
                'from': [round(x0, 3), round(y0, 3), round(z0, 3)],
                'to': [round(x1, 3), round(y1, 3), round(z1, 3)],
                'rotation': {'angle': -45, 'axis': 'z', 'origin': [8, 8, 8]},
                'faces': faces,
            }
            if glow:
                el['shade'] = False
                el['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
            els.append(el)
        k = self.scale
        display = {
            'thirdperson_righthand': {'rotation': [0, -90, 55], 'translation': [0, 4.0, 0.5], 'scale': [0.85 * k] * 3},
            'thirdperson_lefthand': {'rotation': [0, 90, -55], 'translation': [0, 4.0, 0.5], 'scale': [0.85 * k] * 3},
            'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68 * k] * 3},
            'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [0.68 * k] * 3},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.5] * 3},
            'head': {'rotation': [0, 180, 0], 'translation': [0, 13, 7], 'scale': [1, 1, 1]},
        }
        tex = f'aetherwastes:item/3d/{self.name}'
        return {'textures': {'t': tex, 'particle': tex}, 'display': display, 'elements': els}

    def item_model(self):
        sprite = {'parent': 'minecraft:item/handheld', 'textures': {'layer0': f'aetherwastes:item/{self.name}'}}
        return {
            'loader': 'neoforge:separate_transforms',
            'gui_light': 'front',
            'textures': {'particle': f'aetherwastes:item/{self.name}'},
            'base': {'parent': f'aetherwastes:item/3d/{self.name}'},
            'perspectives': {'gui': sprite, 'fixed': sprite},
        }

    def write(self):
        os.makedirs(TEX_DIR, exist_ok=True)
        os.makedirs(MODEL3D_DIR, exist_ok=True)
        self.texture().save(os.path.join(TEX_DIR, self.name + '.png'))
        with open(os.path.join(MODEL3D_DIR, self.name + '.json'), 'w') as f:
            json.dump(self.model(), f, separators=(',', ':'))
        with open(os.path.join(ITEM_DIR, self.name + '.json'), 'w') as f:
            json.dump(self.item_model(), f, indent=2)
        return len(self.elements)


# ---------------- Палитры ----------------
ETHER = {'grip': '#3a2f52', 'metal': '#8fa6c4', 'edge': '#d8e6f5', 'accent': '#b88a3e', 'glow': '#4fe3ff',
         'dark': '#2c3446', 'wood': '#5b4632'}
STAR = {'grip': '#1d2140', 'metal': '#3b4a86', 'edge': '#c7d2ff', 'accent': '#e2c46a', 'glow': '#fff2a8',
        'dark': '#141a33', 'wood': '#2a2440', 'glow2': '#8fb4ff'}


def weapons():
    out = []

    w = Weapon('ether_blade', ETHER)
    w.pommel(-3.4, gem='glow').handle(-1.8, 5.0).guard(3.2, 6.0, gem='glow').blade(4.6, 15.0, 2.6, fuller='glow')
    out.append(w)

    w = Weapon('ether_steel_sword', ETHER)
    w.pommel(-3.4).handle(-1.8, 5.0).guard(3.2, 5.0, horns=False).blade(4.6, 13.0, 2.4)
    out.append(w)

    w = Weapon('star_iron_sword', STAR)
    w.pommel(-3.4, gem='glow2').handle(-1.8, 5.0).guard(3.2, 5.4, gem='glow2').blade(4.6, 13.5, 2.4, fuller='glow2')
    out.append(w)

    w = Weapon('star_longsword', STAR, scale=1.1)
    w.pommel(-4.4, size=2.6, gem='glow').handle(-2.8, 6.0).guard(3.2, 7.4, gem='glow2').blade(4.6, 18.0, 2.8, fuller='glow')
    for y in (8, 12, 16):
        w.cbox(8, y, 8, 1.2, 1.2, 1.25, 'glow')
    out.append(w)

    w = Weapon('prism_dagger', {'grip': '#3b2a3f', 'metal': '#c7b4ff', 'edge': '#f3eaff', 'accent': '#9fe7ff',
                                'glow': '#ff9cf0', 'crystal': '#b49cff', 'dark': '#2a2235'})
    w.pommel(-2.4, size=2.0, mat='crystal').handle(-0.8, 3.6).guard(2.8, 3.6, mat='accent', horns=False, gem='glow')
    w.blade(4.0, 8.0, 2.2, thick=1.2, mat='crystal', edge='edge', fuller='glow', taper=4)
    out.append(w)

    w = Weapon('heart_blade', {'grip': '#3a1218', 'metal': '#7a1f2c', 'edge': '#ffb3b8', 'accent': '#d9a441',
                               'glow': '#ff3b4f', 'dark': '#2b0a10', 'crystal': '#ff6b7c'}, scale=1.1)
    w.pommel(-3.8, gem='glow').handle(-2.2, 5.4).guard(3.2, 7.0, gem=None)
    w.orb(4.0, 1.4, 'glow')
    w.blade(5.4, 15.5, 3.6, thick=1.1, fuller='crystal')
    out.append(w)

    w = Weapon('chronicle_blade', {'grip': '#4a3524', 'metal': '#d8c79a', 'edge': '#fff6d8', 'accent': '#c99a2e',
                                   'glow': '#ffd36b', 'dark': '#3b2a1a', 'cloth': '#7a2a24'}, scale=1.05)
    w.pommel(-3.4).handle(-1.8, 5.0, mat='cloth').guard(3.2, 6.4, gem='glow')
    w.cbox(8, 2.6, 8, 3.4, 0.6, 3.4, 'cloth')  # закладка-страница
    w.blade(4.6, 16.0, 2.8, fuller='glow')
    for y in (7, 10, 13):
        w.cbox(8, y, 8, 3.0, 0.4, 1.15, 'dark')  # строки рун
    out.append(w)

    w = Weapon('salt_scythe', {'grip': '#6b5a4a', 'metal': '#e9e4da', 'edge': '#ffffff', 'accent': '#9aa6b5',
                               'glow': '#bfefff', 'dark': '#4d4a47', 'wood': '#7d6b58'}, scale=1.15)
    w.shaft(-5, 25, mat='wood', bands='accent', every=7).cbox(8, 19.5, 8, 2.2, 1.6, 2.2, 'accent')
    w.scythe_head(20.6, 8, height=2.4, glow='glow')
    w.cbox(8, 4.5, 8, 2.2, 1.0, 2.2, 'grip')
    out.append(w)

    w = Weapon('echo_reaper', {'grip': '#2a1f3d', 'metal': '#5d4b8c', 'edge': '#d2c3ff', 'accent': '#8c7bd1',
                               'glow': '#b388ff', 'glow2': '#6effe0', 'dark': '#1a1426', 'wood': '#30264a'}, scale=1.15)
    w.shaft(-5, 25, mat='wood', bands='glow2', every=6).cbox(8, 19.5, 8, 2.4, 1.8, 2.4, 'accent')
    w.scythe_head(20.8, 8, height=2.6, glow='glow', side=-1)
    w.scythe_head(19.0, 4, height=1.6, glow='glow2', side=1)
    w.orb(22.6, 0.9, 'glow2')
    out.append(w)

    w = Weapon('convergence_glaive', {'grip': '#2f2a45', 'metal': '#9db4d8', 'edge': '#f0f6ff', 'accent': '#e0b860',
                                      'glow': '#7dffcf', 'glow2': '#ff8ce8', 'dark': '#20233a', 'wood': '#3b3550'},
               scale=1.15)
    w.shaft(-5, 18, mat='wood', bands='accent', every=6)
    w.guard(13, 5.0, gem='glow2')
    w.blade(14.4, 11.0, 2.8, thick=1.0, fuller='glow', taper=4)
    w.box(8 - 2.5, 15, 7.6, 8 - 4.2, 18, 8.4, 'edge').box(8 + 2.5, 15, 7.6, 8 + 4.2, 18, 8.4, 'edge')
    out.append(w)

    w = Weapon('ember_greataxe', {'grip': '#3a2418', 'metal': '#4a4140', 'edge': '#ffc48a', 'accent': '#a8743a',
                                  'glow': '#ff6a1f', 'dark': '#211a18', 'wood': '#5a3a24'}, scale=1.15)
    w.shaft(-5, 24, mat='wood', bands='accent', every=6)
    w.axe_head(13, 7.5, 5, glow='glow', double=True)
    w.cbox(8, 19, 8, 1.6, 2.5, 1.6, 'edge')
    out.append(w)

    w = Weapon('colossus_hammer', {'grip': '#3a2418', 'metal': '#3d3633', 'edge': '#ffb070', 'accent': '#8a5a2e',
                                   'glow': '#ff5a14', 'dark': '#1d1715', 'wood': '#4a3220'}, scale=1.2)
    w.shaft(-5, 20, mat='wood', bands='accent', every=5)
    w.hammer_head(14, 10.0, 6.0, 6.0, glow='glow')
    w.cbox(8, 20, 8, 2.0, 2.5, 2.0, 'edge')
    out.append(w)

    w = Weapon('resonance_maul', {'grip': '#252a3a', 'metal': '#7f8aa3', 'edge': '#e3e9ff', 'accent': '#5fd1c4',
                                  'glow': '#5fffe6', 'dark': '#2a3040', 'wood': '#38384a'}, scale=1.15)
    w.shaft(-5, 20, mat='wood', bands='glow', every=5)
    w.hammer_head(14, 8.0, 5.0, 5.0, glow='glow')
    for dy in (13.4, 19.6):
        w.cbox(8, dy, 8, 5.6, 0.5, 5.6, 'accent')  # кольца резонанса
    out.append(w)

    w = Weapon('spark_scepter', {'grip': '#3a1f1a', 'metal': '#c9a04a', 'edge': '#ffe7a8', 'accent': '#e8c15a',
                                 'glow': '#ffb347', 'glow2': '#ff5e3a', 'dark': '#2a1a14', 'wood': '#5a2f22'})
    w.shaft(-4, 16, mat='wood', bands='accent', every=5)
    w.cbox(8, 11.6, 8, 3.2, 0.8, 3.2, 'accent')
    for dx, dz in ((-1.4, -1.4), (1.4, -1.4), (-1.4, 1.4), (1.4, 1.4)):
        w.box(8 + dx - 0.35, 12.4, 8 + dz - 0.35, 8 + dx + 0.35, 17.2, 8 + dz + 0.35, 'metal')
    w.cbox(8, 17.2, 8, 3.6, 0.7, 3.6, 'accent')
    w.orb(14.8, 1.2, 'glow')
    w.cbox(8, 14.3, 8, 0.8, 1.0, 0.8, 'glow2')
    out.append(w)

    w = Weapon('ether_staff', ETHER)
    w.shaft(-4, 18, mat='wood', bands='accent', every=6)
    w.cbox(8, 14, 8, 2.6, 0.8, 2.6, 'accent')
    w.orb(16.4, 1.3, 'glow')
    for dx in (-1.6, 1.6):
        w.box(8 + dx - 0.3, 14.8, 7.7, 8 + dx + 0.3, 18.2, 8.3, 'accent')
    out.append(w)

    # Инструменты из эфирной стали и звёздного железа
    for prefix, pal, glow in (('ether_steel', ETHER, 'glow'), ('star_iron', STAR, 'glow2')):
        w = Weapon(prefix + '_pickaxe', pal)
        w.shaft(-4, 18, mat='wood', bands=None)
        w.cbox(8, 12.6, 8, 2.4, 2.4, 2.4, 'dark')
        for side in (-1, 1):
            for i in range(5):
                x0 = 8 + side * (1.2 + i * 1.2)
                drop = 0.18 * i * i
                w.box(x0, 12.8 - drop, 7.4, x0 + side * 1.2, 14.6 - drop, 8.6, 'metal')
            w.box(8 + side * 7.2, 9.4, 7.6, 8 + side * 7.8, 11.2, 8.4, 'edge')
        w.cbox(8, 13.2, 8, 0.8, 1.2, 2.5, glow)
        out.append(w)

        w = Weapon(prefix + '_axe', pal)
        w.shaft(-4, 18, mat='wood', bands=None)
        w.axe_head(10.5, 5.5, 4, glow=glow)
        out.append(w)

        w = Weapon(prefix + '_shovel', pal)
        w.shaft(-4, 14, mat='wood', bands=None)
        w.cbox(8, 9.6, 8, 1.8, 1.0, 1.8, 'dark')
        w.cbox(8, 10.6, 8, 4.0, 5.5, 0.8, 'metal')
        w.cbox(8, 16.1, 8, 3.0, 0.8, 0.6, 'edge')
        w.cbox(8, 11.5, 8, 0.8, 3.0, 0.92, glow)
        out.append(w)

    return out


if __name__ == '__main__':
    total = 0
    for w in weapons():
        total += w.write()
        print(f'{w.name}: {len(w.elements)} elements')
    print('elements total', total)
