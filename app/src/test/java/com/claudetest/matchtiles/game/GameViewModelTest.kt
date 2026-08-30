package com.claudetest.matchtiles.game

import com.claudetest.matchtiles.model.GameEvent
import com.claudetest.matchtiles.model.GameUiState
import com.claudetest.matchtiles.model.LevelPlan
import com.claudetest.matchtiles.model.Phase
import com.claudetest.matchtiles.model.TileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `game opens on a locked preview of level one`() = runTest(dispatcher) {
        val vm = gameViewModel(1)

        val opening = vm.state.value
        assertEquals(Phase.Preview, opening.phase)
        assertTrue(opening.inputLocked)
        assertEquals(1, opening.config.level)
        assertEquals(opening.config.chances, opening.chancesLeft)

        advanceUntilIdle()
        assertEquals(Phase.Playing, vm.state.value.phase)
        assertTrue(!vm.state.value.inputLocked)
    }

    @Test
    fun `taps are ignored while the preview is running`() = runTest(dispatcher) {
        val vm = gameViewModel(2)
        val before = vm.state.value

        vm.onTileTapped(before.tiles.first().id)

        assertEquals(before.tiles, vm.state.value.tiles)
        advanceUntilIdle()
    }

    @Test
    fun `a match keeps both tiles up and hands chances back`() = runTest(dispatcher) {
        val vm = gameViewModel(5)
        advanceUntilIdle()
        val start = vm.state.value

        val (a, b) = start.matchingIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)

        val after = vm.state.value
        assertEquals(1, after.matchedPairs)
        assertEquals(1, after.moves)
        assertEquals(1, after.streak)
        assertEquals(start.chancesLeft + start.config.chanceReward, after.chancesLeft)
        assertEquals(GameViewModel.MATCH_POINTS, after.levelScore)
        assertTrue(after.tiles.filter { it.id == a || it.id == b }.all { it.state == TileState.Matched })
    }

    @Test
    fun `matches build a chance surplus above the opening budget`() = runTest(dispatcher) {
        val vm = gameViewModel(17)
        advanceUntilIdle()
        val config = vm.state.value.config

        repeat(3) {
            val (a, b) = vm.state.value.matchingIds()
            vm.onTileTapped(a)
            vm.onTileTapped(b)
        }

        val rich = vm.state.value
        assertEquals(config.chances + 3 * config.chanceReward, rich.chancesLeft)
        assertEquals(3 * config.chanceReward, rich.chancesEarned)
        assertEquals(rich.chancesLeft, rich.chancePeak)
        assertEquals("a surplus reads as a full meter", 1f, rich.chanceFraction, 0f)

        val (c, d) = rich.mismatchedIds()
        vm.onTileTapped(c)
        vm.onTileTapped(d)

        val missed = vm.state.value
        assertEquals(rich.chancesLeft - 1, missed.chancesLeft)
        assertEquals("the peak survives a miss", rich.chancePeak, missed.chancePeak)
        assertTrue("the meter drains against the peak", missed.chanceFraction < 1f)

        advanceUntilIdle()
        assertEquals(Phase.Playing, vm.state.value.phase)
    }

    @Test
    fun `a flawless clear keeps every chance and pays the full bonus`() = runTest(dispatcher) {
        val vm = gameViewModel(23)
        advanceUntilIdle()
        val config = vm.state.value.config

        repeat(config.pairCount) {
            val (a, b) = vm.state.value.matchingIds()
            vm.onTileTapped(a)
            vm.onTileTapped(b)
        }

        val cleared = vm.state.value
        assertEquals(config.chanceCeiling, cleared.chancesLeft)

        val matchPoints = config.pairCount * GameViewModel.MATCH_POINTS +
            GameViewModel.STREAK_BONUS * (0 until config.pairCount).sum()
        val fullBonus = GameViewModel.CLEAR_BONUS_MAX +
            config.level * GameViewModel.CLEAR_BONUS_PER_LEVEL
        assertEquals(matchPoints + fullBonus, cleared.levelScore)
    }

    @Test
    fun `a miss costs a chance and flips both tiles back`() = runTest(dispatcher) {
        val vm = gameViewModel(7)
        advanceUntilIdle()
        val start = vm.state.value

        val (a, b) = start.mismatchedIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)

        val held = vm.state.value
        assertEquals(start.chancesLeft - 1, held.chancesLeft)
        assertEquals(0, held.streak)
        assertTrue("input stays locked while the miss is shown", held.inputLocked)
        assertEquals(setOf(a, b), held.missIds)

        advanceUntilIdle()
        val resolved = vm.state.value
        assertEquals(Phase.Playing, resolved.phase)
        assertTrue(resolved.tiles.all { it.state == TileState.FaceDown })
        assertTrue(resolved.missIds.isEmpty())
    }

    @Test
    fun `spending every chance ends the game`() = runTest(dispatcher) {
        val vm = gameViewModel(3)
        advanceUntilIdle()
        val budget = vm.state.value.config.chances

        repeat(budget) {
            val (a, b) = vm.state.value.mismatchedIds()
            vm.onTileTapped(a)
            vm.onTileTapped(b)
            advanceUntilIdle()
        }

        val over = vm.state.value
        assertEquals(0, over.chancesLeft)
        assertEquals(Phase.GameOver, over.phase)
        assertTrue(over.inputLocked)
    }

    @Test
    fun `clearing every pair completes the level and banks the score`() = runTest(dispatcher) {
        val vm = gameViewModel(11)
        advanceUntilIdle()
        val pairs = vm.state.value.config.pairCount

        repeat(pairs) {
            val (a, b) = vm.state.value.matchingIds()
            vm.onTileTapped(a)
            vm.onTileTapped(b)
        }

        val cleared = vm.state.value
        assertEquals(pairs, cleared.matchedPairs)
        assertEquals(Phase.LevelClear, cleared.phase)
        assertEquals(pairs, cleared.bestStreak)
        assertTrue(
            "clear bonus and streak should beat flat per-match scoring",
            cleared.levelScore > pairs * GameViewModel.MATCH_POINTS,
        )

        val bankedTotal = cleared.totalScore
        vm.nextLevel()
        val next = vm.state.value
        assertEquals(2, next.config.level)
        assertEquals(bankedTotal, next.bankedScore)
        assertEquals(0, next.levelScore)
        assertEquals(Phase.Preview, next.phase)
        assertNotEquals(cleared.config.tileCount, next.config.tileCount)
        advanceUntilIdle()
    }

    @Test
    fun `retry redeals the same level and keeps banked score`() = runTest(dispatcher) {
        val vm = gameViewModel(13)
        advanceUntilIdle()
        val (a, b) = vm.state.value.matchingIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)

        vm.retryLevel()
        val retried = vm.state.value
        assertEquals(1, retried.config.level)
        assertEquals(0, retried.matchedPairs)
        assertEquals(0, retried.levelScore)
        assertEquals(retried.config.chances, retried.chancesLeft)
        assertEquals(retried.config.chances, retried.chancePeak)
        assertEquals(Phase.Preview, retried.phase)
        advanceUntilIdle()
    }

    @Test
    fun `the memorise window waits for the board to be drawn`() = runTest(dispatcher) {
        val vm = GameViewModel(Random(19))

        advanceUntilIdle()
        assertEquals("no clock runs before the grid is on screen", Phase.Preview, vm.state.value.phase)

        vm.onBoardShown()
        advanceUntilIdle()
        assertEquals(Phase.Playing, vm.state.value.phase)

        // Recomposition can call this again; the window is armed once per board.
        vm.onBoardShown()
        advanceUntilIdle()
        assertEquals(Phase.Playing, vm.state.value.phase)
    }

    @Test
    fun `a redealt board waits for its own draw before counting down`() = runTest(dispatcher) {
        val vm = gameViewModel(29)
        advanceUntilIdle()

        vm.retryLevel()
        advanceUntilIdle()
        assertEquals(
            "the fresh board holds its preview until it is drawn",
            Phase.Preview,
            vm.state.value.phase,
        )

        vm.onBoardShown()
        advanceUntilIdle()
        assertEquals(Phase.Playing, vm.state.value.phase)
    }

    @Test
    fun `a shuffle reopens the board for a ten second look`() = runTest(dispatcher) {
        val vm = gameViewModel(31)
        advanceUntilIdle()
        val (a, b) = vm.state.value.matchingIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)
        val before = vm.state.value

        vm.shuffleBoard()
        val shuffled = vm.state.value

        assertEquals(Phase.Preview, shuffled.phase)
        assertTrue(shuffled.inputLocked)
        assertEquals(GameViewModel.SHUFFLE_LOOK_MS, shuffled.lookMillis)
        assertNotEquals("the entrance has to replay", before.dealId, shuffled.dealId)
        assertEquals("progress is untouched", before.matchedPairs, shuffled.matchedPairs)
        assertEquals(before.chancesLeft, shuffled.chancesLeft)
        assertEquals(before.levelScore, shuffled.levelScore)
        assertEquals(
            "matched tiles are left alone",
            before.tiles.filter { it.state == TileState.Matched }.map { it.id to it.symbol },
            shuffled.tiles.filter { it.state == TileState.Matched }.map { it.id to it.symbol },
        )
        assertEquals(
            "the symbols in play are only moved, never swapped out",
            before.tiles.map { it.symbol }.sorted(),
            shuffled.tiles.map { it.symbol }.sorted(),
        )

        // The look is counted from the new arrangement being drawn, like any other deal.
        advanceUntilIdle()
        assertEquals(Phase.Preview, vm.state.value.phase)
        vm.onBoardShown()
        advanceTimeBy(GameViewModel.SHUFFLE_LOOK_MS - 100)
        assertEquals("ten seconds is the whole point", Phase.Preview, vm.state.value.phase)

        advanceUntilIdle()
        val playing = vm.state.value
        assertEquals(Phase.Playing, playing.phase)
        assertTrue(
            "everything still in play goes back face down",
            playing.tiles.filter { it.state != TileState.Matched }
                .all { it.state == TileState.FaceDown },
        )
    }

    @Test
    fun `shuffling is refused unless the level is in play`() = runTest(dispatcher) {
        val vm = gameViewModel(37)

        val opening = vm.state.value
        vm.shuffleBoard()
        assertEquals("not during the opening look", opening.dealId, vm.state.value.dealId)

        advanceUntilIdle()
        val (a, b) = vm.state.value.mismatchedIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)
        val held = vm.state.value
        vm.shuffleBoard()
        assertEquals("not while a miss is being shown", held.dealId, vm.state.value.dealId)
        advanceUntilIdle()
    }

    @Test
    fun `clearing the last level wins the game for good`() = runTest(dispatcher) {
        val vm = gameViewModel(41)
        advanceUntilIdle()

        while (vm.state.value.config.level < LevelPlan.LAST_LEVEL) {
            clearBoard(vm)
            assertEquals(Phase.LevelClear, vm.state.value.phase)
            vm.nextLevel()
            vm.onBoardShown()
            advanceUntilIdle()
        }
        clearBoard(vm)

        val won = vm.state.value
        assertEquals(Phase.GameWon, won.phase)
        assertTrue(won.inputLocked)
        assertEquals(LevelPlan.LAST_LEVEL, won.config.level)

        // Nothing comes after the finale.
        vm.nextLevel()
        assertEquals(Phase.GameWon, vm.state.value.phase)
        assertEquals(LevelPlan.LAST_LEVEL, vm.state.value.config.level)
    }

    @Test
    fun `flips matches misses and shuffles all announce themselves`() = runTest(dispatcher) {
        val vm = gameViewModel(43)
        advanceUntilIdle()

        val heard = mutableListOf<GameEvent>()
        val listener = launch { vm.events.collect { heard.add(it) } }
        runCurrent()

        val (a, b) = vm.state.value.matchingIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)
        runCurrent()
        assertEquals(listOf(GameEvent.Flip, GameEvent.Flip, GameEvent.Match), heard)

        heard.clear()
        val (c, d) = vm.state.value.mismatchedIds()
        vm.onTileTapped(c)
        vm.onTileTapped(d)
        runCurrent()
        assertEquals(listOf(GameEvent.Flip, GameEvent.Flip, GameEvent.Miss), heard)

        heard.clear()
        advanceUntilIdle()
        vm.shuffleBoard()
        runCurrent()
        assertEquals(listOf(GameEvent.Shuffle), heard)
        listener.cancel()
    }
}

/** Matches every remaining pair on the current board, in whatever order they are found. */
private fun clearBoard(vm: GameViewModel) {
    repeat(vm.state.value.config.pairCount - vm.state.value.matchedPairs) {
        val (a, b) = vm.state.value.matchingIds()
        vm.onTileTapped(a)
        vm.onTileTapped(b)
    }
}

/** A view model with its look clock armed, exactly as the screen does on first draw. */
private fun gameViewModel(seed: Int) = GameViewModel(Random(seed)).apply { onBoardShown() }

private fun GameUiState.matchingIds(): Pair<Int, Int> {
    val pair = tiles
        .filter { it.state == TileState.FaceDown }
        .groupBy { it.symbol }
        .values
        .first { it.size >= 2 }
    return pair[0].id to pair[1].id
}

private fun GameUiState.mismatchedIds(): Pair<Int, Int> {
    val down = tiles.filter { it.state == TileState.FaceDown }
    val first = down.first()
    val second = down.first { it.symbol != first.symbol }
    return first.id to second.id
}
