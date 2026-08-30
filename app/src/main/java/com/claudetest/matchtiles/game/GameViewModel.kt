package com.claudetest.matchtiles.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudetest.matchtiles.model.GameEvent
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.model.LevelConfig
import com.claudetest.matchtiles.model.LevelPlan
import com.claudetest.matchtiles.model.Phase
import com.claudetest.matchtiles.model.Tile
import com.claudetest.matchtiles.model.TileState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Owns all game rules. [random] is injectable so tests can deal deterministic boards;
 * `@JvmOverloads` keeps a no-arg constructor around for the default ViewModel factory.
 */
class GameViewModel @JvmOverloads constructor(
    private val random: Random = Random.Default,
) : ViewModel() {

    /**
     * Bumped for every board dealt or shuffled, so each look window is armed exactly once.
     * Declared ahead of the state it stamps, since the opening board is dealt right below.
     */
    private var deals = 1
    private var armedDeal = 0

    private val _state = MutableStateFlow(freshLevel(level = 1, banked = 0, bestStreak = 0))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /**
     * Fire-and-forget notifications for the UI to sonify. Buffered and emitted with
     * `tryEmit` so the rules never block on nobody listening.
     */
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var previewJob: Job? = null
    private var resolveJob: Job? = null

    /**
     * Called once per board, when the grid has actually been drawn. The look window is
     * timed from here rather than from the deal: a cold start can spend most of a second
     * laying the board out, and counting that down against an empty grid is time the player
     * never gets to use.
     */
    fun onBoardShown() {
        if (armedDeal == deals) return
        armedDeal = deals
        startPreview()
    }

    fun onTileTapped(tileId: Int) {
        val current = _state.value
        val tapped = current.tiles.firstOrNull { it.id == tileId } ?: return
        if (!current.isTappable(tapped)) return

        val flipped = current.tiles.map {
            if (it.id == tileId) it.copy(state = TileState.FaceUp) else it
        }
        val faceUp = flipped.filter { it.state == TileState.FaceUp }
        _events.tryEmit(GameEvent.Flip)

        if (faceUp.size < 2) {
            _state.value = current.copy(tiles = flipped, missIds = emptySet())
            return
        }

        val (first, second) = faceUp
        if (first.symbol == second.symbol) {
            applyMatch(current, flipped, first.id, second.id)
        } else {
            applyMiss(current, flipped, first.id, second.id)
        }
    }

    fun nextLevel() {
        val current = _state.value
        if (current.phase != Phase.LevelClear) return
        deal(
            level = current.config.level + 1,
            banked = current.totalScore,
            bestStreak = current.bestStreak,
        )
    }

    /** Replays the current level. Banked score survives; the level's own score does not. */
    fun retryLevel() {
        val current = _state.value
        deal(
            level = current.config.level,
            banked = current.bankedScore,
            bestStreak = current.bestStreak,
        )
    }

    fun newGame() {
        deal(level = 1, banked = 0, bestStreak = 0)
    }

    /**
     * The shuffle lifeline: every tile still in play swaps places, and the new arrangement
     * is shown for [SHUFFLE_LOOK_MS] before it all turns back over.
     *
     * Free to use, but not a free win — the positions the player had already learned are
     * gone, so the look has to be spent re-memorising the board.
     */
    fun shuffleBoard() {
        val current = _state.value
        if (!current.canShuffle) return

        val open = current.tiles.filter { it.state != TileState.Matched }
        if (open.size < 2) return
        val symbols = open.map { it.symbol }.shuffled(random)
        val moved = open.map { it.id }.zip(symbols).toMap()

        previewJob?.cancel()
        resolveJob?.cancel()
        deals++
        _state.value = current.copy(
            tiles = current.tiles.map { tile ->
                val symbol = moved[tile.id] ?: return@map tile
                tile.copy(symbol = symbol, state = TileState.FaceDown)
            },
            phase = Phase.Preview,
            inputLocked = true,
            missIds = emptySet(),
            dealId = deals,
            lookMillis = SHUFFLE_LOOK_MS,
        )
        _events.tryEmit(GameEvent.Shuffle)
    }

    /** Puts a new board up in its preview phase; [onBoardShown] starts its clock. */
    private fun deal(level: Int, banked: Int, bestStreak: Int) {
        previewJob?.cancel()
        resolveJob?.cancel()
        deals++
        _state.value = freshLevel(level = level, banked = banked, bestStreak = bestStreak)
    }

    private fun applyMatch(current: GameUiState, flipped: List<Tile>, aId: Int, bId: Int) {
        val streak = current.streak + 1
        val gained = MATCH_POINTS + STREAK_BONUS * (streak - 1)
        val matchedPairs = current.matchedPairs + 1
        val cleared = matchedPairs == current.config.pairCount
        val won = cleared && LevelPlan.isFinalLevel(current.config.level)

        // A correct guess pays chances back, so a good memory keeps the hunt alive. The
        // rate is per level, tapering as boards grow (see LevelPlan.rewardFor).
        val chancesLeft = current.chancesLeft + current.config.chanceReward

        val tiles = flipped.map {
            if (it.id == aId || it.id == bId) it.copy(state = TileState.Matched) else it
        }
        // Scored on the share of the level's total chances still in hand, so the bonus
        // measures accuracy instead of growing with the reward rate.
        val clearBonus = if (cleared) {
            val kept = chancesLeft.toFloat() / current.config.chanceCeiling
            (kept * CLEAR_BONUS_MAX).toInt() + current.config.level * CLEAR_BONUS_PER_LEVEL
        } else {
            0
        }

        _state.value = current.copy(
            tiles = tiles,
            chancesLeft = chancesLeft,
            chancePeak = maxOf(current.chancePeak, chancesLeft),
            matchedPairs = matchedPairs,
            moves = current.moves + 1,
            streak = streak,
            bestStreak = maxOf(current.bestStreak, streak),
            levelScore = current.levelScore + gained + clearBonus,
            phase = when {
                won -> Phase.GameWon
                cleared -> Phase.LevelClear
                else -> current.phase
            },
            inputLocked = cleared,
            missIds = emptySet(),
        )
        _events.tryEmit(
            when {
                won -> GameEvent.GameWon
                cleared -> GameEvent.LevelClear
                else -> GameEvent.Match
            }
        )
    }

    private fun applyMiss(current: GameUiState, flipped: List<Tile>, aId: Int, bId: Int) {
        _state.value = current.copy(
            tiles = flipped,
            moves = current.moves + 1,
            streak = 0,
            chancesLeft = current.chancesLeft - 1,
            inputLocked = true,
            missIds = setOf(aId, bId),
        )
        _events.tryEmit(GameEvent.Miss)

        resolveJob = viewModelScope.launch {
            delay(MISMATCH_HOLD_MS)
            _state.update { state ->
                val outOfChances = state.chancesLeft <= 0
                state.copy(
                    tiles = state.tiles.map {
                        if (it.id == aId || it.id == bId) it.copy(state = TileState.FaceDown) else it
                    },
                    phase = if (outOfChances) Phase.GameOver else Phase.Playing,
                    inputLocked = outOfChances,
                    missIds = emptySet(),
                )
            }
            if (_state.value.phase == Phase.GameOver) _events.tryEmit(GameEvent.GameOver)
        }
    }

    private fun startPreview() {
        previewJob?.cancel()
        resolveJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(_state.value.lookMillis)
            _state.update { state ->
                if (state.phase == Phase.Preview) {
                    state.copy(phase = Phase.Playing, inputLocked = false)
                } else {
                    state
                }
            }
        }
    }

    private fun freshLevel(level: Int, banked: Int, bestStreak: Int): GameUiState {
        val config = LevelPlan.configFor(level)
        return GameUiState(
            config = config,
            tiles = BoardFactory.build(config, random),
            chancesLeft = config.chances,
            chancePeak = config.chances,
            matchedPairs = 0,
            moves = 0,
            streak = 0,
            bestStreak = bestStreak,
            levelScore = 0,
            bankedScore = banked,
            phase = Phase.Preview,
            inputLocked = true,
            missIds = emptySet(),
            dealId = deals,
            lookMillis = previewMillis(config),
        )
    }

    private fun previewMillis(config: LevelConfig): Long =
        (PREVIEW_BASE_MS + PREVIEW_PER_TILE_MS * config.tileCount).coerceAtMost(PREVIEW_MAX_MS)

    companion object {
        const val MATCH_POINTS = 100
        const val STREAK_BONUS = 25
        /** Full value of the clear bonus, awarded for a level cleared without a miss. */
        const val CLEAR_BONUS_MAX = 500
        const val CLEAR_BONUS_PER_LEVEL = 25
        const val MISMATCH_HOLD_MS = 800L
        /** Memorise window: generous on purpose, and timed from the board being drawn. */
        const val PREVIEW_BASE_MS = 1200L
        const val PREVIEW_PER_TILE_MS = 50L
        const val PREVIEW_MAX_MS = 3600L
        /** A shuffle buys a long, flat look at the board: ten seconds, however big it is. */
        const val SHUFFLE_LOOK_MS = 10_000L
    }
}
