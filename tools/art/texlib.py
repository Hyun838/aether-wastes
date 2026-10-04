"""Библиотека пиксель-арта: палитры-рампы, шум, фаска, руды, кирпич, доски, металл, руны."""
import math
import random
from PIL import Image


def clamp(v):
    return max(0, min(255, int(round(v))))


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*cols):
    """Список цветов от тёмного к светлому."""
    return [hexc(c) if isinstance(c, str) else c for c in cols]


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, k):
    return tuple(clamp(x * k) for x in c)


class Noise:
    def __init__(self, seed):
        self.seed = seed
        self.cache = {}

    def h(self, x, y):
        k = (x, y)
        if k not in self.cache:
            r = random.Random((self.seed * 73856093) ^ (x * 19349663) ^ (y * 83492791))
            self.cache[k] = r.random()
        return self.cache[k]

    def smooth(self, x, y, scale, wrap=16):
        gx, gy = x / scale, y / scale
        x0, y0 = int(math.floor(gx)), int(math.floor(gy))
        fx, fy = gx - x0, gy - y0
        n = max(1, int(wrap / scale))
        f = lambda a, b: self.h(a % n, b % n)
        sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = f(x0, y0) * (1 - sx) + f(x0 + 1, y0) * sx
        bot = f(x0, y0 + 1) * (1 - sx) + f(x0 + 1, y0 + 1) * sx
        return top * (1 - sy) + bot * sy

    def fbm(self, x, y, wrap=16):
        return 0.5 * self.smooth(x, y, 8, wrap) + 0.3 * self.smooth(x, y, 4, wrap) + 0.2 * self.h(x % wrap, y % wrap)


def new(w=16, h=16, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), fill)


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (c[3] if len(c) == 4 else a,))


def get(img, x, y):
    return img.getpixel((x % img.width, y % img.height))


def material(r, seed, w=16, h=16, contrast=1.0, bias=0.0):
    """Залить текстуру шумом, квантованным в рампу (как ванильный камень)."""
    nz = Noise(seed)
    img = new(w, h)
    for x in range(w):
        for y in range(h):
            v = nz.fbm(x, y, max(w, h))
            v = 0.5 + (v - 0.5) * 1.8 * contrast + bias
            i = max(0, min(len(r) - 1, int(v * len(r))))
            put(img, x, y, r[i])
    return img


def speckle(img, r, seed, count=10, size=2):
    """Пятна-«камешки» с тенью снизу справа и бликом сверху слева."""
    rnd = random.Random(seed)
    for _ in range(count):
        cx, cy = rnd.randrange(img.width), rnd.randrange(img.height)
        s = rnd.randint(1, size)
        dark = rnd.random() < 0.5
        for dx in range(s):
            for dy in range(s):
                put(img, (cx + dx) % img.width, (cy + dy) % img.height, r[0] if dark else r[-1])
        if not dark:
            put(img, (cx + s) % img.width, (cy + s) % img.height, r[1])
    return img


def bevel(img, light=1.25, dark=0.62, width=1):
    w, h = img.size
    for x in range(w):
        for y in range(h):
            p = img.getpixel((x, y))
            if p[3] == 0:
                continue
            k = 1.0
            if x < width or y < width:
                k = light
            if x >= w - width or y >= h - width:
                k = dark
            put(img, x, y, shade(p, k), p[3])
    return img


def ore(img, gem, seed, clusters=4, outline=(30, 30, 36)):
    """Кристаллы руды: контур, грань, блик (gem — рампа из 3–4 цветов)."""
    rnd = random.Random(seed)
    shapes = [
        [(0, 0), (1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
        [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2), (2, 2)],
        [(0, 0), (1, 0), (1, 1), (2, 1), (2, 2)],
        [(0, 1), (1, 0), (1, 1), (1, 2), (2, 1)],
    ]
    placed = []
    for _ in range(clusters * 6):
        if len(placed) >= clusters:
            break
        cx, cy = rnd.randint(1, img.width - 4), rnd.randint(1, img.height - 4)
        if any(abs(cx - a) < 4 and abs(cy - b) < 4 for a, b in placed):
            continue
        placed.append((cx, cy))
        shp = rnd.choice(shapes)
        cells = {(cx + dx, cy + dy) for dx, dy in shp}
        for (x, y) in cells:
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + ox, y + oy) not in cells:
                    put(img, x + ox, y + oy, outline)
        for (x, y) in cells:
            top = (x - 1, y) not in cells or (x, y - 1) not in cells
            bot = (x + 1, y) not in cells and (x, y + 1) not in cells
            put(img, x, y, gem[-1] if top else gem[0] if bot else gem[len(gem) // 2])
        hx, hy = min(cells)
        put(img, hx, hy, (255, 255, 255))
    return img


def bricks(w, h, brick, mortar, seed, bw=8, bh=4):
    img = new(w, h)
    nz = Noise(seed)
    for y in range(h):
        row = y // bh
        off = (bw // 2) * (row % 2)
        for x in range(w):
            in_m = (y % bh == bh - 1) or ((x + off) % bw == bw - 1)
            if in_m:
                put(img, x, y, mortar[1] if nz.h(x, y) > 0.3 else mortar[0])
            else:
                bx = (x + off) // bw
                tone = nz.h(bx + 50, row + 50)
                i = 1 + int(tone * (len(brick) - 2))
                c = brick[i]
                if y % bh == 0:
                    c = brick[min(len(brick) - 1, i + 1)]
                if (x + off) % bw == bw - 2 or y % bh == bh - 2:
                    c = brick[max(0, i - 1)]
                if nz.h(x * 3, y * 7) > 0.92:
                    c = brick[0]
                put(img, x, y, c)
    return img


def planks(w, h, r, seed, ph=4):
    img = new(w, h)
    nz = Noise(seed)
    for y in range(h):
        for x in range(w):
            row = y // ph
            g = nz.smooth(x * 3 + row * 40, row * 9, 4, 64)
            i = 1 + int(g * (len(r) - 2))
            c = r[i]
            if y % ph == ph - 1:
                c = r[0]
            elif y % ph == 0:
                c = r[min(len(r) - 1, i + 1)]
            if (x + row * 5) % 16 == 0 and y % ph != ph - 1:
                c = r[0]
            put(img, x, y, c)
    for row in range(h // ph):
        rnd = random.Random(seed + row)
        for _ in range(2):
            put(img, rnd.randrange(w), row * ph + 1 + rnd.randrange(ph - 2), r[0])
    return img


def metal(w, h, r, seed, rivets=True, plates=8):
    img = material(r[1:-1], seed, w, h, contrast=0.4)
    for x in range(w):
        for y in range(h):
            if x % plates == plates - 1 or y % plates == plates - 1:
                put(img, x, y, r[0])
            elif x % plates == 0 or y % plates == 0:
                put(img, x, y, r[-1])
    if rivets:
        for px in range(0, w, plates):
            for py in range(0, h, plates):
                put(img, px + 1, py + 1, r[-1])
                put(img, px + plates - 3, py + plates - 3, r[-1])
                put(img, px + 2, py + 2, r[0])
    return img


def frame(img, dark, light=None, inset=0):
    w, h = img.size
    for i in range(inset, w - inset):
        put(img, i, inset, light or dark)
        put(img, i, h - 1 - inset, dark)
    for j in range(inset, h - inset):
        put(img, inset, j, light or dark)
        put(img, w - 1 - inset, j, dark)
    return img


RUNES = [
    ["010", "111", "010", "010"], ["101", "010", "101", "010"], ["110", "010", "011", "001"],
    ["111", "100", "110", "100"], ["010", "101", "111", "101"], ["001", "011", "110", "100"],
    ["111", "010", "010", "111"], ["100", "110", "011", "001"],
]


def rune(img, x, y, idx, color, glow=None):
    pat = RUNES[idx % len(RUNES)]
    for j, row in enumerate(pat):
        for i, ch in enumerate(row):
            if ch == "1":
                if glow:
                    for ox, oy in ((1, 0), (0, 1)):
                        px, py = x + i + ox, y + j + oy
                        if 0 <= px < img.width and 0 <= py < img.height and pat[min(3, j + oy)][min(2, i + ox)] != "1":
                            put(img, px, py, glow)
                put(img, x + i, y + j, color)
    return img


def overlay(base, top):
    out = base.copy()
    out.alpha_composite(top)
    return out


def sprite(rows, palette):
    """Пиксель-арт из ASCII: символ → цвет; '.' — прозрачно."""
    h = len(rows)
    w = max(len(r) for r in rows)
    img = new(w, h)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                c = palette[ch]
                put(img, x, y, c if len(c) == 4 else tuple(c) + (255,))
    return img


def outline_sprite(img, color=(24, 18, 30)):
    """Тёмный контур вокруг непрозрачных пикселей (стиль иконок Minecraft)."""
    out = img.copy()
    w, h = img.size
    for x in range(w):
        for y in range(h):
            if img.getpixel((x, y))[3] == 0:
                for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + ox, y + oy
                    if 0 <= nx < w and 0 <= ny < h and img.getpixel((nx, ny))[3] > 0:
                        put(out, x, y, color)
                        break
    return out


def anim_strip(frames):
    w, h = frames[0].size
    strip = new(w, h * len(frames))
    for i, f in enumerate(frames):
        strip.paste(f, (0, i * h))
    return strip
