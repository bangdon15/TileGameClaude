"""Synthesize every sound the game ships with, into app/src/main/res/raw.

No downloaded assets: the background loop and the UI blips are built from sine partials
here so they can be retuned and regenerated. Run: python tools/make_audio.py
"""
import math
import os
import struct
import wave

RATE = 22050
RAW = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..",
    "app", "src", "main", "res", "raw",
)
BPM = 100
BEAT = 60.0 / BPM

STEPS = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5,
         "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}

# A music box: a strong fundamental with two quiet partials above it.
BELL = ((1, 1.0), (2, 0.30), (4, 0.10))
ROUND = ((1, 1.0), (2, 0.12))
SOFT = ((1, 1.0), (3, 0.12))


def hz(note):
    """'A4' -> 440.0."""
    midi = 12 * (int(note[-1]) + 1) + STEPS[note[:-1]]
    return 440.0 * 2 ** ((midi - 69) / 12.0)


def tone(note, seconds, amp=0.5, tau=0.3, attack=0.004, partials=BELL):
    """One plucked note: instant attack, exponential decay over [tau] seconds."""
    freq = hz(note) if isinstance(note, str) else float(note)
    count = int(seconds * RATE)
    out = [0.0] * count
    for i in range(count):
        t = i / RATE
        env = min(1.0, t / attack) * math.exp(-t / tau)
        value = 0.0
        for mult, weight in partials:
            value += weight * math.sin(2 * math.pi * freq * mult * t)
        out[i] = amp * env * value
    return out


def sweep(start, end, seconds, amp=0.5, tau=0.06, partials=ROUND):
    """A blip that slides from [start] Hz to [end] Hz, phase-accumulated so it stays smooth."""
    count = int(seconds * RATE)
    out = [0.0] * count
    phase = 0.0
    for i in range(count):
        t = i / RATE
        freq = start + (end - start) * (t / seconds)
        phase += 2 * math.pi * freq / RATE
        env = min(1.0, t / 0.004) * math.exp(-t / tau)
        value = 0.0
        for mult, weight in partials:
            value += weight * math.sin(phase * mult)
        out[i] = amp * env * value
    return out


def mix(track, at, samples):
    """Add [samples] into [track] at [at] seconds, growing the track as needed."""
    start = int(at * RATE)
    short = start + len(samples) - len(track)
    if short > 0:
        track.extend([0.0] * short)
    for i, value in enumerate(samples):
        track[start + i] += value


def write(name, track, peak=0.82, edge=0.006):
    """Normalise, fade the very edges so a loop point cannot click, and save as 16-bit mono."""
    loudest = max((abs(s) for s in track), default=1.0) or 1.0
    gain = peak / loudest
    fade = max(1, int(edge * RATE))
    frames = bytearray()
    for i, sample in enumerate(track):
        value = sample * gain
        if i < fade:
            value *= i / fade
        tail = len(track) - i
        if tail < fade:
            value *= tail / fade
        frames += struct.pack("<h", int(max(-1.0, min(1.0, value)) * 32767))
    path = os.path.join(RAW, name + ".wav")
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(bytes(frames))
    print("%-12s %5.2fs %5d KiB" % (name + ".wav", len(track) / RATE, len(frames) // 1024))

# A four-bar phrase in C major: I - vi - IV - V, the friendliest progression there is.
# (beat, note, length in beats)
MELODY = [
    (0.0, "E5", 0.5), (0.5, "G5", 0.5), (1.0, "C6", 1.0),
    (2.0, "A5", 0.5), (2.5, "G5", 0.5), (3.0, "E5", 1.0),
    (4.0, "A5", 0.5), (4.5, "C6", 0.5), (5.0, "A5", 1.0),
    (6.0, "G5", 0.5), (6.5, "E5", 0.5), (7.0, "D5", 1.0),
    (8.0, "F5", 0.5), (8.5, "A5", 0.5), (9.0, "C6", 1.0),
    (10.0, "A5", 0.5), (10.5, "F5", 0.5), (11.0, "G5", 1.0),
    (12.0, "D5", 0.5), (12.5, "G5", 0.5), (13.0, "B5", 1.0),
    (14.0, "A5", 0.5), (14.5, "G5", 0.5),
]
BASS = [(0.0, "C3"), (2.0, "C3"), (4.0, "A2"), (6.0, "A2"),
        (8.0, "F2"), (10.0, "F2"), (12.0, "G2"), (14.0, "G2")]
PADS = [(0.0, ("C4", "E4", "G4")), (4.0, ("A3", "C4", "E4")),
        (8.0, ("F3", "A3", "C4")), (12.0, ("G3", "B3", "D4"))]
BARS = 16.0


def background(passes=2):
    """The looping menu/game music: music-box melody over a soft bass and pad."""
    track = []
    for turn in range(passes):
        head = turn * BARS * BEAT
        for beat, note, length in MELODY:
            mix(track, head + beat * BEAT,
                tone(note, length * BEAT * 1.7, amp=0.50, tau=0.34))
        for beat, note in BASS:
            mix(track, head + beat * BEAT,
                tone(note, BEAT * 1.5, amp=0.40, tau=0.30, partials=ROUND))
            if turn % 2 == 1:
                # Second time round, double the bass an octave up for a little lift.
                mix(track, head + beat * BEAT,
                    tone(hz(note) * 2, BEAT * 1.2, amp=0.13, tau=0.22, partials=ROUND))
        for beat, chord in PADS:
            for note in chord:
                mix(track, head + beat * BEAT,
                    tone(note, 4 * BEAT, amp=0.12, tau=1.1, attack=0.09, partials=SOFT))
    return track


def arpeggio(notes, spacing, seconds, amp=0.55, tau=0.30):
    track = []
    for i, note in enumerate(notes):
        mix(track, i * spacing, tone(note, seconds, amp=amp, tau=tau))
    return track

def win():
    """A short fanfare, then the tonic chord ringing out."""
    track = arpeggio(["C5", "E5", "G5", "C6"], 0.12, 0.7, amp=0.55, tau=0.35)
    for note in ("C5", "E5", "G5", "C6"):
        mix(track, 0.60, tone(note, 1.3, amp=0.30, tau=0.55))
    return track


def main():
    os.makedirs(RAW, exist_ok=True)
    write("bgm_loop", background(), peak=0.70)
    # Tapping a tile: a tiny rising blip, quiet enough to hear forty times a level.
    write("sfx_tap", sweep(880, 1320, 0.07, amp=0.60, tau=0.05), peak=0.55)
    write("sfx_button", sweep(520, 780, 0.08, amp=0.55, tau=0.05), peak=0.55)
    write("sfx_shuffle", sweep(420, 1500, 0.28, amp=0.45, tau=0.14, partials=SOFT), peak=0.6)
    write("sfx_match", arpeggio(["E5", "A5"], 0.085, 0.45, amp=0.55, tau=0.28), peak=0.72)
    # A miss sags instead of scolding.
    write("sfx_miss", sweep(320, 170, 0.24, amp=0.50, tau=0.13,
                            partials=((1, 1.0), (2, 0.35), (3, 0.15))), peak=0.6)
    write("sfx_clear", arpeggio(["C5", "E5", "G5", "C6"], 0.09, 0.8), peak=0.78)
    write("sfx_over", arpeggio(["G4", "E4", "C4"], 0.17, 0.75, amp=0.5, tau=0.35), peak=0.65)
    write("sfx_win", win(), peak=0.82)


if __name__ == "__main__":
    main()
