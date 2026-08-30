# Tile Match

A child-friendly memory match game for Android, built with Kotlin and Jetpack Compose.

Boards start at 4 × 4 and grow every level up to 8 × 8. Every tile is a big, colourful
sticker; every correct pair hands hearts back, so a good memory keeps the run alive instead
of ending it. Six levels, then the game says thank you.

| Menu | Settings | Playing | Shuffle look | Win |
| --- | --- | --- | --- | --- |
| ![Menu](docs/screenshots/menu.png) | ![Settings](docs/screenshots/settings.png) | ![Playing](docs/screenshots/game.png) | ![Shuffle](docs/screenshots/shuffle.png) | ![Win](docs/screenshots/win.png) |

## How it plays

1. A splash screen pops four sticker tiles in, then hands off to the menu. Tap to skip it.
2. **Play** deals the board face up for a short memorise window, then flips everything down.
3. Tap two tiles. A match stays up, pays hearts back, and builds a streak. A miss flips both
   back and costs one heart.
4. Clearing every pair finishes the level. Running out of hearts ends the run.
5. **🔀 Shuffle** rearranges the tiles still in play and shows them for a ten-second look
   before they go face down again. It is free and unlimited — it costs no hearts and has no
   cooldown, so a stuck player is never stuck for long.
6. **🏠** quits to the home screen at any time. The system back button does the same.
7. Clearing the last level shows a **Thank you for playing!** card with confetti.

## Progression

Boards grow one dimension at a time until they hit the 8 × 8 cap. Odd tile counts gain a
column so every board can always be split into pairs.

| Level | Board | Pairs | Opening hearts | Hearts per match |
| --- | --- | --- | --- | --- |
| 1 | 4 × 4 | 8 | 12 | 5 |
| 2 | 5 × 6 | 15 | 23 | 5 |
| 3 | 6 × 6 | 18 | 28 | 4 |
| 4 | 7 × 8 | 28 | 39 | 4 |
| 5 | 8 × 8 | 32 | 44 | 3 |
| 6 | 8 × 8 | 32 | 44 | 3 |

- Opening hearts are `3 × columns`, plus 5 for every level the board has grown.
- The per-match reward starts at 5 and drops by 1 every two levels, with a floor of 1, so
  later levels are less forgiving without ever becoming unwinnable.
- Level 6 is the finale on purpose: it is one level past the point where the board stops
  growing, so the run ends on a full-size board at a tightened reward rate rather than on
  the first board that merely stopped growing. `LevelPlan.LAST_LEVEL` is the single knob.
- Difficulty never loosens once the board is capped — the unit tests assert it.

## Scoring

- **100** per matched pair.
- **+25 × streak** for each consecutive match, reset by a miss.
- **Clear bonus** of up to **500**, scaled by the share of the level's total possible hearts
  still in hand, plus **25 × level**. It measures accuracy, so it does not inflate as the
  reward rate changes.

## Sound

Two independent switches behind the ⚙ in the menu, persisted across launches:

- **Background music** — a looping 19.2 s music-box phrase.
- **Tap sounds** — the interface and gameplay effects (tap, button, shuffle, match, miss,
  level clear, game over, win fanfare).

Every sound in `app/src/main/res/raw` is generated, not licensed: `tools/make_audio.py`
synthesizes all nine WAVs from scratch with Python's standard library (`wave`, `math`,
`struct`) using additive sine partials, exponential decay envelopes and phase-accumulated
sweeps, with edge fades so the music loop does not click. Regenerate them with:

```bash
python tools/make_audio.py
```

## Architecture

MVVM with a deliberately Android-free core, so the game rules are unit-testable.

```
model/      LevelPlan (the curve), GameUiState + Phase, Tile, GameEvent
game/       GameViewModel (all rules), BoardFactory, Symbols
ui/         MatchTilesApp (navigation), SplashScreen, MenuScreen, GameScreen,
            Board, TileCard, Hud, Overlays, Confetti, theme/
audio/      GameAudio (SoundPool + MediaPlayer) behind an AudioController interface
data/       SettingsStore (SharedPreferences mirrored into StateFlows)
```

Notable decisions:

- **The ViewModel never touches a `Context`.** It reports what happened through a
  `MutableSharedFlow<GameEvent>`; the UI turns those events into sound via a `LocalAudio`
  composition local. `SilentAudio` is the default, which keeps previews and tests quiet.
- **No extra dependencies.** Navigation is an `enum class Screen` plus `remember` and a
  `BackHandler`; the splash is hand-rolled rather than `core-splashscreen`. The project
  builds `--offline` against exactly the libraries in `gradle/libs.versions.toml`.
- **Timing is driven by the animation, not by a clock.** Every deal and shuffle bumps a
  `dealId`, the tiles are keyed on it, and the entrance animation's own completion callback
  arms the look window. A board is therefore never memorised before it is actually on
  screen, however slow a cold start is.
- Compose targets `minSdk 26` / `compileSdk 37`, Java and Kotlin both on JVM 17.

## Build and run

Needs the Android SDK and a JDK 17+ (Android Studio's bundled JBR works).

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:installDebug       # install on a connected device or emulator
./gradlew :app:testDebugUnitTest  # 32 JUnit tests
```

Or open the project in Android Studio and press Run.

## Tests

32 JUnit 4 tests, all green, covering the level curve, the board dealer and the whole
ViewModel state machine — including that a shuffle preserves progress and the exact symbol
multiset, that the ten-second look only starts once the board is drawn, that spending every
heart ends the run, and that clearing the last level wins the game for good. Coroutine
timing is driven with `kotlinx-coroutines-test`, so the suite runs in seconds with no
real delays.

```bash
./gradlew :app:testDebugUnitTest
```

