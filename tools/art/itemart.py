"""Иконки предметов: пиксель-арт 16×16 из ASCII, с контуром и тенями."""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from texlib import *
from PIL import Image, ImageDraw

OUT = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes/textures/item"
HERE = os.path.dirname(__file__)
ICONS = {}

PURPLE = {"a": hexc("#3d1c6b"), "b": hexc("#5a2d9a"), "c": hexc("#7d45c9"), "d": hexc("#a06ae6"), "e": hexc("#d6b8ff"), "w": (255, 255, 255)}
BLUE = {"a": hexc("#1c2a6b"), "b": hexc("#2d4a9a"), "c": hexc("#4570c9"), "d": hexc("#7aa6ff"), "e": hexc("#fff4a8"), "w": (255, 255, 255)}
MAGENTA = {"a": hexc("#4a0a36"), "b": hexc("#7a1458"), "c": hexc("#b0207e"), "d": hexc("#e04aa8"), "e": hexc("#ffb0e0"), "w": (255, 255, 255)}
WOOD = {"m": hexc("#4e321e"), "n": hexc("#6e4a2c"), "N": hexc("#8f6640")}
GOLD = {"g": hexc("#8a5e14"), "G": hexc("#bf8a24"), "h": hexc("#e8b84a"), "H": hexc("#fff0a8")}
COPPER = {"o": hexc("#7d4026"), "O": hexc("#a5582f"), "p": hexc("#c8733c"), "P": hexc("#e8a070")}
GREY = {"s": hexc("#4a4a52"), "S": hexc("#6a6a74"), "t": hexc("#9a9aa4"), "T": hexc("#cacad2")}


def icon(name, rows, *pals, outline=True):
    pal = {}
    for p in pals:
        pal.update(p)
    img = sprite(rows, pal)
    if outline:
        img = outline_sprite(img, (26, 18, 34))
    ICONS[name] = img


SHARD = [
    "................",
    "...........e....",
    "..........edw...",
    ".........edcd...",
    "........edccb...",
    ".......edccb....",
    "......edccba....",
    ".....edccba.....",
    "....edccba......",
    "....dccba.......",
    "...dccba........",
    "...ccbba........",
    "..cbbaa.........",
    "..baa...........",
    "................",
    "................",
]


def build():
    icon("ether_shard", SHARD, PURPLE)
    icon("charged_shard", SHARD, BLUE)
    icon("wanderer_shard", SHARD, MAGENTA)

    icon("ether_staff", [
        "...........eew..",
        "..........edde..",
        ".........GcddG..",
        "..........Gcc...",
        ".........nG.....",
        "........nN......",
        ".......nN.......",
        "......nN........",
        ".....nN.........",
        "....nN..........",
        "...GG...........",
        "..nN............",
        ".nN.............",
        ".m..............",
        "................",
        "................",
    ], PURPLE, WOOD, GOLD)

    icon("ether_compass", [
        "................",
        ".....oooooo.....",
        "....oPppppPo....",
        "...oPsseessPo...",
        "..oPsseddessPo..",
        "..opsssedssspo..",
        "..opsssdcssspo..",
        "..opssscwsssPo..",
        "..opsssTtssspo..",
        "..opssstTssspo..",
        "..oPsssTsssPpo..",
        "...oPssssssPo...",
        "....oPppppPo....",
        ".....oooooo.....",
        "................",
        "................",
    ], PURPLE, COPPER, GREY)

    icon("anchor_core", [
        "................",
        ".......GG.......",
        "......GhhG......",
        "...GGGGGGGGGG...",
        "...GhcccccchG...",
        "..GGcdeeeddcGG..",
        "..GhcdewdcbcGh..",
        "..GGcdedcbbcGG..",
        "..GhcddcbbacGh..",
        "..GGccbbbaacGG..",
        "...GhcbbaaccG...",
        "...GGGGGGGGGG...",
        "......GhhG......",
        ".......GG.......",
        "................",
        "................",
    ], PURPLE, GOLD)

    SALTP = {"1": hexc("#b8b2a4"), "2": hexc("#d6d1c6"), "3": hexc("#efece6"), "4": (255, 255, 255)}
    icon("salt", [
        "................",
        "................",
        "......43........",
        ".....4332.......",
        "....433221......",
        "..43..3221.43...",
        ".4332..21.4332..",
        ".4332221..43321.",
        "..33222143332221",
        "...2211433322211",
        "....1.4333222211",
        "......333222211.",
        ".......2222211..",
        "........11111...",
        "................",
        "................",
    ], SALTP)

    RES = {"1": hexc("#5c0a0a"), "2": hexc("#8f1414"), "3": hexc("#c42a1a"), "4": hexc("#ff7a3a"), "5": hexc("#ffd0a0")}
    icon("resin", [
        "................",
        "................",
        ".......33.......",
        "......3443......",
        ".....334453.....",
        ".....3345432....",
        "....33344332....",
        "....23333322....",
        "...2233332221...",
        "...2223322211...",
        "...1222222111...",
        "....11222111....",
        ".....111111.....",
        "................",
        "................",
        "................",
    ], RES)

    STAR = {"1": hexc("#3a4680"), "2": hexc("#5a6cb8"), "3": hexc("#8a9ee8"), "4": hexc("#c8d4ff"), "5": hexc("#fffbe0")}
    icon("star_iron_ingot", [
        "................",
        "................",
        "................",
        "................",
        "......4444444...",
        ".....45444443...",
        "....4444444432..",
        "...44444444322..",
        "..3333333332221.",
        "..3335333322211.",
        "..33333333221...",
        "..2222222221....",
        "...111111111....",
        "................",
        "................",
        "................",
    ], STAR)

    SOUL = {"1": hexc("#0c5c70"), "2": hexc("#1a8aa0"), "3": hexc("#3ac4d8"), "4": hexc("#90f0ff"), "5": (255, 255, 255)}
    icon("soul_essence", [
        "................",
        ".......4........",
        "......43........",
        "......343.......",
        ".....4433.......",
        ".....34432......",
        "....3445432.....",
        "...234555432....",
        "...234554432....",
        "...23445432.....",
        "...12344332.....",
        "....1233321.....",
        ".....11221......",
        "................",
        "................",
        "................",
    ], SOUL)

    BOOK = {"1": hexc("#2a0f3a"), "2": hexc("#3d1752"), "3": hexc("#5a2678"), "4": hexc("#7a3a9a"), "p": hexc("#ebe2c8"), "q": hexc("#c8bea0")}
    icon("journal", [
        "................",
        "...222222222....",
        "..23333333332...",
        "..2343333333q...",
        "..2343hhh333pq..",
        "..234h3e3h33pq..",
        "..2343heh333pq..",
        "..23433h3333pq..",
        "..2343h3h333pq..",
        "..234h333h33pq..",
        "..23433333333q..",
        "..234333333332..",
        "..1222222222221.",
        "...111111111111.",
        "................",
        "................",
    ], BOOK, GOLD, PURPLE)

    icon("splint", [
        "................",
        "....nN....nN....",
        "....nN....nN....",
        "...TTTTTTTTTT...",
        "...tTTTTTTTTt...",
        "....nN....nN....",
        "....nN....nN....",
        "....nN....nN....",
        "....nN....nN....",
        "...TTTTTTTTTT...",
        "...tTTTTTTTTt...",
        "....nN....nN....",
        "....nN....nN....",
        "....mm....mm....",
        "................",
        "................",
    ], WOOD, GREY)

    BAND = {"1": hexc("#a8a090"), "2": hexc("#d8d0c0"), "3": hexc("#f4f0e6"), "r": hexc("#c02020"), "R": hexc("#ff5050")}
    icon("bandage", [
        "................",
        "................",
        "................",
        ".....222222.....",
        "...2233333322...",
        "..233333333332..",
        "..2333rRr33332..",
        "..233rRRRr3332..",
        "..2333rRr33332..",
        "..233333333332..",
        "..122333333221..",
        "...1122222211...",
        ".....111111.....",
        "................",
        "................",
        "................",
    ], BAND)

    SALVE = {"1": hexc("#2c6626"), "2": hexc("#3d8a32"), "3": hexc("#7ac860"), "4": hexc("#c0f0a0")}
    icon("burn_salve", [
        "................",
        "................",
        "................",
        "................",
        ".....GGGGGG.....",
        "...GhhhhhhhhG...",
        "...G33443332G...",
        "...nG2333322Gm..",
        "...nNGGGGGGGNm..",
        "...nNNNNNNNNNm..",
        "...nNNNNNNNNNm..",
        "...nnNNNNNNNmm..",
        "....mmmmmmmmm...",
        "................",
        "................",
        "................",
    ], SALVE, WOOD, GOLD)

    GL = {"x": hexc("#9ec4d0"), "X": hexc("#d8f0f8")}
    EL = {"1": hexc("#1a6a3a"), "2": hexc("#2ea060"), "3": hexc("#6ae0a0"), "4": hexc("#d0ffe8")}
    icon("elixir", [
        "................",
        "......mmmm......",
        "......nNNn......",
        ".......xX.......",
        ".......xX.......",
        "......xxXX......",
        ".....x3443X.....",
        "....x232343X....",
        "....x223332X....",
        "....x122232X....",
        "....x112222X....",
        ".....x1111X.....",
        "......xxxx......",
        "................",
        "................",
        "................",
    ], GL, EL, WOOD)

    STEW = {"1": hexc("#8a3a14"), "2": hexc("#c06020"), "3": hexc("#e89040"), "4": hexc("#ffc070"), "v": hexc("#4a9a30"), "c": hexc("#e8a030")}
    icon("hearty_stew", [
        "................",
        "................",
        "................",
        "................",
        "................",
        "..mmmmmmmmmmmm..",
        ".m3342v33c4323m.",
        ".mn2c3332v432Nm.",
        "..mnNNNNNNNNNm..",
        "..mnNNNNNNNNNm..",
        "...mnNNNNNNNm...",
        "....mmnNNNmm....",
        "......mmmm......",
        "................",
        "................",
        "................",
    ], STEW, WOOD)

    icon("underside_key", [
        "................",
        "...cccc.........",
        "..cdeedc........",
        ".cde..edc.......",
        ".ce....ec.......",
        ".cd....dc.......",
        "..cd..dcG.......",
        "...cccGGhG......",
        "........GhG.....",
        ".........GhG....",
        "..........GhG...",
        ".........GGGhG..",
        "........G.GGGhG.",
        "............G.G.",
        "................",
        "................",
    ], PURPLE, GOLD)

    SEAL = {"1": hexc("#5a0a0a"), "2": hexc("#8a1414"), "3": hexc("#c02a2a"), "4": hexc("#e85a5a")}
    icon("second_age_seal", [
        "................",
        ".....GGGGGG.....",
        "...GGhhhhhhGG...",
        "..GhH333333hhG..",
        "..Gh32222223hG..",
        ".Gh3221hh1223hG.",
        ".Gh321h44h123hG.",
        ".Gh32h4334h23hG.",
        ".Gh32h4334h23hG.",
        ".Gh321h44h123hG.",
        ".Gh3221hh1223hG.",
        "..Gh32222223hG..",
        "..GhH333333hhG..",
        "...GGhhhhhhGG...",
        ".....GGGGGG.....",
        "................",
    ], SEAL, GOLD)

    TROPHY = [
        "................",
        "...GGGGGGGGGG...",
        "..GhHHhhhhhhhG..",
        "..GhhhhhhhhhgG..",
        ".GGGhhh{e}hhgGGG",
        ".G.GhhhhhhhhgG.G",
        ".GG.GhhhhhhgG.GG",
        "..GG.GhhhhgG.GG.",
        "....GGGhhgGGG...",
        "......GhhG......",
        "......GhgG......",
        ".....GGhgGG.....",
        "....GhhhhhgG....",
        "....GGGGGGGG....",
        "................",
        "................",
    ]
    icon("hunter_trophy", [r.replace("{e}", "HH") for r in TROPHY], GOLD)
    icon("nemesis_trophy", [r.replace("{e}", "33") for r in TROPHY], GOLD, SEAL)
    icon("nemesis_echo", [
        "................",
        ".......4........",
        "......43........",
        "......343.......",
        ".....4433.......",
        ".....34432......",
        "....3445432.....",
        "...23w44w432....",
        "...234444432....",
        "...23411432.....",
        "...12344332.....",
        "....1233321.....",
        ".....11221......",
        "................",
        "................",
        "................",
    ], SEAL, {"w": (255, 230, 230)})

    # --- Глифы: каменная пластина + символ ---
    plate_rows = [
        "................",
        "...SSSSSSSSSS...",
        "..STTTTTTTTTTs..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..STttttttttts..",
        "..Ssssssssssss..",
        "................",
        "................",
    ]
    PLATE = {"S": hexc("#5a5a64"), "T": hexc("#a8a8b2"), "t": hexc("#82828c"), "s": hexc("#3e3e46")}
    RARE_PLATE = dict(PLATE, S=hexc("#8a5e14"), T=hexc("#e8b84a"), s=hexc("#5c3d0a"))
    icon("glyph_blank", plate_rows, PLATE)
    SYMS = {
        "touch": ["..xx.", ".x..x", ".x..x", "..xx.", "..x..", "..x..", ".xx.."],
        "projectile": ["....xx", "...x.x", "..x...", ".x....", "x.....", ".......", "......."],
        "self": ["..x..", ".xxx.", "..x..", ".x.x.", "x...x", ".x.x.", "..x.."],
        "zone": [".xxx.", "x...x", "x.x.x", "x...x", ".xxx.", ".....", "....."],
        "wave": ["x...x", ".x.x.", "..x..", "x...x", ".x.x.", "..x..", "....."],
        "rune": ["xxxxx", "x.x.x", "xxxxx", "..x..", ".x.x.", "x...x", "....."],
        "summon": ["..x..", ".xxx.", "xxxxx", "..x..", ".x.x.", "x...x", "....."],
        "forge": ["..x..", ".xx..", ".xxx.", "xxxxx", "xx.xx", ".xxx.", "....."],
        "flow": ["x..x.", ".xx.x", ".....", "x..x.", ".xx.x", ".....", "....."],
        "root": ["..x..", "..x..", ".xxx.", "x.x.x", "..x..", ".x.x.", "x...x"],
        "silence": ["x...x", ".x.x.", "..x..", ".x.x.", "x...x", ".....", "....."],
        "stone": [".xxx.", "xx.xx", "x...x", "xx.xx", ".xxx.", ".....", "....."],
        "ash": [".xxx.", "x.x.x", "xxxxx", ".x.x.", ".xxx.", ".....", "....."],
        "star": ["..x..", "..x..", "xxxxx", ".xxx.", ".x.x.", "x...x", "....."],
        "amplify": ["..x..", ".xxx.", "x.x.x", "..x..", "..x..", "..x..", "....."],
        "expand": ["x...x", ".x.x.", ".....", ".x.x.", "x...x", ".....", "....."],
        "delay": ["xxxxx", ".x.x.", "..x..", ".x.x.", "xxxxx", ".....", "....."],
        "chain": ["xx...", "x.x..", ".xxx.", "..x.x", "...xx", ".....", "....."],
        "quiet": [".....", "x...x", ".xxx.", ".....", "xxxxx", ".....", "....."],
        "echo": ["x.x.x", "x.x.x", "x.x.x", ".....", "x.x.x", ".....", "....."],
    }
    COLORS = {"form": (hexc("#6ee3f0"), hexc("#d0fbff")), "mod": (hexc("#c060ff"), hexc("#f0d0ff"))}
    ESS = {"forge": "#ff8a2a", "flow": "#4aa0ff", "root": "#5ad048", "silence": "#202028", "stone": "#d0d0d8",
           "ash": "#c02a2a", "star": "#fff4a0"}
    FORMS = ["touch", "projectile", "self", "zone", "wave", "rune", "summon"]
    RARE = {"wave", "rune", "summon", "delay", "chain", "quiet", "echo"}
    for g, sym in SYMS.items():
        if g in ESS:
            col = hexc(ESS[g]); hi = mix(col, (255, 255, 255), 0.5)
        elif g in FORMS:
            col, hi = COLORS["form"]
        else:
            col, hi = COLORS["mod"]
        rows = [list(r) for r in plate_rows]
        sw = max(len(r) for r in sym)
        ox = 8 - (sw + 1) // 2
        oy = 4
        for j, r in enumerate(sym):
            for i, ch in enumerate(r):
                if ch == "x":
                    rows[oy + j][ox + i] = "x"
                    if oy + j + 1 < 13 and rows[oy + j + 1][ox + i] == "t" and (j + 1 >= len(sym) or i >= len(sym[j + 1]) or sym[j + 1][i] != "x"):
                        rows[oy + j + 1][ox + i] = "y"
        pal = dict(RARE_PLATE if g in RARE else PLATE)
        pal["x"] = hi
        pal["y"] = shade(col, 0.55)
        icon(f"glyph_{g}", ["".join(r) for r in rows], pal)


def main():
    build()
    os.makedirs(OUT, exist_ok=True)
    for n, img in ICONS.items():
        img.save(f"{OUT}/{n}.png")
    names = list(ICONS)
    cols = 12
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 72, rows * 72), (139, 139, 139, 255))
    for k, n in enumerate(names):
        sheet.alpha_composite(ICONS[n].resize((64, 64), Image.NEAREST), ((k % cols) * 72 + 4, (k // cols) * 72 + 4))
    sheet.save(os.path.join(HERE, "items_preview.png"))
    print(len(names), "icons")


if __name__ == "__main__":
    main()
