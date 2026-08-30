package com.claudetest.matchtiles.model

/**
 * Something that just happened and is worth hearing.
 *
 * The view model posts these instead of playing sounds itself: rules stay testable and
 * free of any Android [android.content.Context], and the UI decides what each one sounds
 * like (and whether sound is switched on at all).
 */
enum class GameEvent {
    Flip,
    Match,
    Miss,
    Shuffle,
    LevelClear,
    GameOver,
    GameWon,
}
