package com.robert.courtcompanion.cli

import com.robert.courtcompanion.grouping.Goal
import com.robert.courtcompanion.grouping.Grouping
import com.robert.courtcompanion.grouping.averageSkill
import com.robert.courtcompanion.model.MAX_SKILL
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface

/** Draws a grouping as cards, like the app's result screen, to a PNG. Uses Skia directly, no window. */
object GroupImage {
    // The app's group colours (ui/theme/Theme.kt), as ARGB.
    private val colors = intArrayOf(
        0xFF2E6FD8.toInt(), 0xFFD9482B.toInt(), 0xFF2E9E5B.toInt(), 0xFFE8A11E.toInt(),
        0xFF7B4FC9.toInt(), 0xFF1C9AA6.toInt(), 0xFFD64F95.toInt(), 0xFF5D6B7A.toInt(),
    )
    private const val CARD_W = 240f
    private const val HEADER = 34f
    private const val ROW = 24f
    private const val GAP = 12f
    private const val PAD = 10f

    private val typeface: Typeface? by lazy {
        FontMgr.default.matchFamilyStyle("sans-serif", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle("DejaVu Sans", FontStyle.NORMAL)
            ?: FontMgr.default.legacyMakeTypeface("", FontStyle.NORMAL)
    }
    private val bold: Typeface? by lazy {
        FontMgr.default.matchFamilyStyle("sans-serif", FontStyle.BOLD) ?: typeface
    }

    fun render(grouping: Grouping, goal: Goal): ByteArray {
        val groups = grouping.groups
        val columns = groups.size.coerceIn(1, 4)
        val rows = (groups.size + columns - 1) / columns
        val tallest = groups.maxOfOrNull { it.size } ?: 0
        val cardH = HEADER + PAD * 2 + ROW * tallest
        val width = (GAP + columns * (CARD_W + GAP)).toInt()
        val height = (GAP + rows.coerceAtLeast(1) * (cardH + GAP)).toInt()

        val surface = Surface.makeRasterN32Premul(width, height)
        val canvas = surface.canvas
        canvas.clear(0xFFF3F4F7.toInt())
        val text = Font(typeface, 15f)
        val small = Font(typeface, 12f)
        val title = Font(bold, 15f)
        val paint = Paint().apply { isAntiAlias = true }
        val label = if (goal == Goal.BALANCED) "Team" else "Group"

        groups.forEachIndexed { i, group ->
            val x = GAP + (i % columns) * (CARD_W + GAP)
            val y = GAP + (i / columns) * (cardH + GAP)
            val color = colors[i % colors.size]
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawRRect(RRect.makeXYWH(x, y, CARD_W, cardH, 10f), paint)
            paint.color = color
            canvas.save()
            canvas.clipRRect(RRect.makeXYWH(x, y, CARD_W, cardH, 10f), true)
            canvas.drawRect(Rect.makeXYWH(x, y, CARD_W, HEADER), paint)
            canvas.restore()
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawString("$label ${i + 1}", x + PAD, y + 23f, title, paint)
            val avg = "avg ${"%.1f".format(group.averageSkill())}"
            canvas.drawString(avg, x + CARD_W - PAD - small.measureTextWidth(avg), y + 22f, small, paint)

            group.forEachIndexed { r, p ->
                val baseline = y + HEADER + PAD + ROW * r + 17f
                paint.color = 0xFF1B1F24.toInt()
                canvas.drawString(p.name, x + PAD, baseline, text, paint)
                val initials = p.tags.sorted().joinToString(" ") { it.take(1).uppercase() }
                val dotsX = x + CARD_W - PAD - MAX_SKILL * 11f
                if (initials.isNotEmpty()) {
                    paint.color = 0xFF6B7280.toInt()
                    canvas.drawString(initials, dotsX - 8f - small.measureTextWidth(initials), baseline - 1f, small, paint)
                }
                for (d in 0 until MAX_SKILL) {
                    paint.color = if (d < p.skill) color else 0xFFD5D9E0.toInt()
                    canvas.drawCircle(dotsX + d * 11f + 4f, baseline - 5f, 4f, paint)
                }
            }
        }
        return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: error("Could not encode the image")
    }
}
