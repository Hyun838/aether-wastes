"""Синтез звуков мода (numpy → wav → ogg через ffmpeg) и sounds.json."""
import json, os, subprocess, tempfile
import numpy as np

SR = 44100
OUT = "/home/claude/aetherwastes/src/main/resources/assets/aetherwastes/sounds"
rng = np.random.default_rng(7)


def t(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(n, a=0.01, r=0.3, curve=2.0):
    x = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    if na:
        x[:na] = np.linspace(0, 1, na)
    if nr:
        x[-nr:] *= np.linspace(1, 0, nr) ** curve
    return x


def lowpass(x, cutoff):
    """Однополюсный фильтр; cutoff может быть массивом."""
    cutoff = np.broadcast_to(cutoff, x.shape)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        a = 1 - np.exp(-2 * np.pi * cutoff[i] / SR)
        acc += a * (x[i] - acc)
        y[i] = acc
    return y


def reverb(x, decay=0.4, taps=(0.031, 0.047, 0.071, 0.113, 0.157)):
    y = x.copy()
    for k, d in enumerate(taps):
        n = int(d * SR)
        g = decay ** (k + 1)
        y[n:] += x[:-n] * g
    return y


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(norm(x), -1, 1)
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".raw", delete=False) as f:
        f.write(pcm.tobytes())
        raw = f.name
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(SR), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "5", f"{OUT}/{name}.ogg"], check=True)
    os.remove(raw)


def spell_cast(seed):
    tt = t(0.7)
    noise = rng.standard_normal(len(tt))
    sweep = lowpass(noise, 300 + 5000 * (tt / 0.7) ** 1.5) * env(len(tt), 0.02, 0.35)
    sparkle = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * 6) for f in (880 * (1 + seed * 0.12), 1320, 1760 * (1 + seed * 0.05)))
    return reverb(sweep * 1.2 + sparkle * 0.25 * env(len(tt), 0.1, 0.4))


def whisper(seed):
    tt = t(1.6)
    noise = rng.standard_normal(len(tt))
    band = lowpass(noise, 2500) - lowpass(noise, 600)
    am = 0.5 + 0.5 * np.sin(2 * np.pi * (3 + seed) * tt + np.sin(2 * np.pi * 0.7 * tt) * 2)
    return reverb(band * am * env(len(tt), 0.3, 0.6), 0.5)


def boss_roar(seed):
    tt = t(2.2)
    f = 70 + 25 * np.sin(2 * np.pi * 5 * tt) + 40 * np.exp(-tt * 2)
    phase = 2 * np.pi * np.cumsum(f) / SR
    saw = (phase / np.pi % 2) - 1
    growl = np.tanh(3 * (saw + 0.4 * lowpass(rng.standard_normal(len(tt)), 400)))
    sub = np.sin(phase / 2) * 0.6
    return reverb(lowpass(growl, 1800) + sub, 0.45) * env(len(tt), 0.08, 1.0)


def rift_open(seed):
    tt = t(2.6)
    f = 900 * np.exp(-tt * 1.4) + 60
    phase = 2 * np.pi * np.cumsum(f) / SR
    tone = np.sin(phase) + 0.4 * np.sin(phase * 1.5)
    rumble = lowpass(rng.standard_normal(len(tt)), 180) * 3
    return reverb(tone * 0.6 + rumble, 0.6) * env(len(tt), 0.05, 1.4)


def ritual(seed):
    tt = t(1.8)
    out = np.zeros(len(tt))
    for k, f in enumerate((523.25, 659.25, 783.99, 1046.5)):
        start = int(k * 0.12 * SR)
        seg = tt[: len(tt) - start]
        out[start:] += np.sin(2 * np.pi * f * seg) * np.exp(-seg * 3) + 0.3 * np.sin(4 * np.pi * f * seg) * np.exp(-seg * 5)
    return reverb(out, 0.55)


def tide(seed):
    tt = t(4.0)
    drone = sum(np.sin(2 * np.pi * f * tt + np.sin(2 * np.pi * 0.2 * tt) * k) for k, f in enumerate((55, 82.5, 110, 164.8)))
    shimmer = lowpass(rng.standard_normal(len(tt)), 3000) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.5 * tt)) * 0.3
    return reverb(drone * 0.5 + shimmer, 0.5) * env(len(tt), 1.2, 1.5)


def anchor_hum(seed):
    tt = t(2.0)
    hum = np.sin(2 * np.pi * 110 * tt) + 0.5 * np.sin(2 * np.pi * 220.5 * tt) + 0.25 * np.sin(2 * np.pi * 331 * tt)
    return hum * (0.6 + 0.4 * np.sin(2 * np.pi * 2 * tt)) * env(len(tt), 0.4, 0.8)


SOUNDS = {
    "spell_cast": (spell_cast, 3, "Заклинание"),
    "whisper": (whisper, 3, "Шёпот"),
    "boss_roar": (boss_roar, 2, "Рёв Скитальца"),
    "rift_open": (rift_open, 1, "Разлом открылся"),
    "ritual": (ritual, 1, "Ритуал"),
    "tide": (tide, 1, "Прилив Эфира"),
    "anchor_hum": (anchor_hum, 1, "Гул Якоря"),
}


def main():
    sj = {}
    for name, (fn, variants, sub) in SOUNDS.items():
        files = []
        for v in range(variants):
            fname = f"{name}{v + 1}" if variants > 1 else name
            save(fname, fn(v))
            files.append(f"aetherwastes:{fname}")
        sj[name] = {"sounds": files, "subtitle": f"subtitles.aetherwastes.{name}"}
    with open(os.path.join(os.path.dirname(OUT), "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(sj, f, indent=2)
    print("sounds ok")


if __name__ == "__main__":
    main()
