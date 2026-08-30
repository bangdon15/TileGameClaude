package com.claudetest.matchtiles.game

/**
 * The face of a tile: a glyph plus the accent colour drawn behind it.
 *
 * [accent] is a plain ARGB long rather than a Compose `Color` so this stays usable
 * from pure JVM unit tests.
 */
data class TileFace(val glyph: String, val accent: Long)

object Symbols {

    /**
     * Needs to be at least `MAX_DIM * MAX_DIM / 2` entries so the largest board can be
     * filled without repeating a glyph.
     *
     * Every accent is saturated enough to read as a thick ring on a white tile, so nothing
     * here may be pastel - the tiles sit on a light background.
     */
    val faces: List<TileFace> = listOf(
        TileFace("🍒", 0xFFE63946),
        TileFace("🍋", 0xFFE8AE00),
        TileFace("🍇", 0xFF8E5AE8),
        TileFace("🍉", 0xFF23BE63),
        TileFace("🍊", 0xFFF57C00),
        TileFace("🫐", 0xFF3B82F6),
        TileFace("🥑", 0xFF6FA82F),
        TileFace("🍍", 0xFFD79B18),
        TileFace("🌰", 0xFFA0632F),
        TileFace("🌶", 0xFFE11D48),
        TileFace("⭐", 0xFFF0A800),
        TileFace("🌙", 0xFF5B8DEF),
        TileFace("☀", 0xFFF59E0B),
        TileFace("⚡", 0xFFDD9A00),
        TileFace("❄", 0xFF22A7E0),
        TileFace("🔥", 0xFFF4560A),
        TileFace("💧", 0xFF0EA5E9),
        TileFace("🌈", 0xFFA855F7),
        TileFace("🎈", 0xFFF43F5E),
        TileFace("🎁", 0xFFEC4899),
        TileFace("🎯", 0xFFDC2626),
        TileFace("🎲", 0xFF64748B),
        TileFace("🎸", 0xFFC2701A),
        TileFace("🎧", 0xFF6366F1),
        TileFace("🚀", 0xFF0EA5B7),
        TileFace("🛸", 0xFF14B8A6),
        TileFace("🧩", 0xFF10B981),
        TileFace("🔔", 0xFFD9A006),
        TileFace("💎", 0xFF06AFC9),
        TileFace("🔑", 0xFFB08206),
        TileFace("🧭", 0xFF0D9488),
        TileFace("🦋", 0xFF7C3AED),
        TileFace("🐙", 0xFFDB2777),
        TileFace("🐝", 0xFFCC9500),
        TileFace("🐬", 0xFF0284C7),
        TileFace("🦊", 0xFFEA7317),
    )

    val count: Int get() = faces.size

    operator fun get(symbol: Int): TileFace = faces[symbol.mod(faces.size)]
}
