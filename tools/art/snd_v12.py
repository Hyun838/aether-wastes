"""Звуки 1.2: Призрачный шаг, возвращение, гул подземелья, удар босса, слом печати."""
import numpy as np
from soundart import t, env, lowpass, reverb, save, SR

rng = np.random.default_rng(12)


def phantom_phase(v):
    tt = t(1.3)
    f = 300 + 900 * (1 - np.exp(-tt * 3)) + v * 80
    ph = np.cumsum(2 * np.pi * f / SR)
    tone = np.sin(ph) * 0.4 + np.sin(ph * 1.5 + np.sin(tt * 30) * 0.5) * 0.25
    air = lowpass(rng.standard_normal(len(tt)), 0.08 + 0.2 * tt / tt[-1]) * 1.2
    x = (tone + air) * env(len(tt), 0.05, 0.9)
    return reverb(x, 0.55)


def phantom_return(v):
    tt = t(0.9)
    f = 1100 * np.exp(-tt * 3) + 140 + v * 40
    ph = np.cumsum(2 * np.pi * f / SR)
    x = np.sin(ph) * 0.5 + lowpass(rng.standard_normal(len(tt)), 0.1) * 0.8
    thump = np.sin(2 * np.pi * 60 * tt) * np.exp(-tt * 12) * 1.2
    return reverb((x * env(len(tt), 0.01, 0.6) + thump), 0.45)


def dungeon_ambient(v):
    tt = t(5.0)
    drone = sum(np.sin(2 * np.pi * f * tt + np.sin(2 * np.pi * 0.13 * tt * (k + 1)) * 2) / (k + 1)
                for k, f in enumerate((41 + v * 3, 61.5, 82.4, 123)))
    wind = lowpass(rng.standard_normal(len(tt)), 0.02 + 0.015 * np.sin(2 * np.pi * 0.3 * tt)) * 3
    drip = np.zeros(len(tt))
    for k in range(3):
        at = int(rng.uniform(0.5, 4.5) * SR)
        n = int(0.25 * SR)
        drip[at:at + n] += np.sin(2 * np.pi * (1800 - 800 * np.linspace(0, 1, n)) * np.linspace(0, 0.25, n)) * np.exp(-np.linspace(0, 10, n)) * 0.5
    return reverb((drone * 0.5 + wind + drip) * env(len(tt), 1.0, 1.5), 0.6)


def boss_slam(v):
    tt = t(1.6)
    boom = np.sin(2 * np.pi * (45 + 30 * np.exp(-tt * 6)) * tt) * np.exp(-tt * 3.5) * 1.6
    crack = lowpass(rng.standard_normal(len(tt)), 0.35) * np.exp(-tt * 10) * 1.5
    rubble = lowpass(rng.standard_normal(len(tt)), 0.06) * np.exp(-tt * 2) * (0.8 + v * 0.2)
    return reverb(boom + crack + rubble, 0.5)


def seal_break(v):
    tt = t(2.4)
    out = np.zeros(len(tt))
    for k, f in enumerate((523, 659, 784, 1046, 1318)):
        out += np.sin(2 * np.pi * f * tt * (1 - 0.15 * tt / tt[-1])) * np.exp(-tt * (1.5 + k * 0.4)) / (k + 1)
    shatter = lowpass(rng.standard_normal(len(tt)), 0.5) * np.exp(-tt * 6)
    low = np.sin(2 * np.pi * 50 * tt) * np.exp(-tt * 1.5)
    return reverb(out + shatter + low * 0.8, 0.6)


for name, fn, n in (("phantom_phase", phantom_phase, 2), ("phantom_return", phantom_return, 2),
                    ("dungeon_ambient", dungeon_ambient, 2), ("boss_slam", boss_slam, 2), ("seal_break", seal_break, 1)):
    for v in range(n):
        save(f"{name}{v + 1}", fn(v))
print("sounds v1.2 ok")
