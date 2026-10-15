package dev.omalauncher.ui

import dev.omalauncher.theme.Toml
import dev.omalauncher.wayland.Keys

/** One key on the extra-keys bar; see [ExtraKeysSpec.parse]. */
sealed interface ExtraKey

/**
 * A key that sends [code], with [held] pressed around it. Drawn as [label], or
 * as a chevron turned [arrow] degrees from up. [repeats] while it's held.
 */
class Press(
    val label: String,
    val code: Int,
    val repeats: Boolean = false,
    val arrow: Float? = null,
    val held: Int? = null,
) : ExtraKey

/** A modifier that latches: a tap holds it for the next key, a long press locks it. [modifier] names it. */
class LatchKey(
    val label: String,
    val modifier: String,
) : ExtraKey

/**
 * keys.toml, the terminal's extra-keys bar: one top-level key per row, in file
 * order, whose text is that row's keys left to right. A row is shared by the
 * bar on the home screen and the one in a Linux window.
 *
 * Plain Kotlin, no Android, so the format is unit-tested; [ExtraKeysStore] reads
 * it for the app.
 */
object ExtraKeysSpec {
    /** The bar's shape: one row's height in dp, and the label size in sp. */
    data class Sizes(
        val row: Float = 40f,
        val text: Float = 14f,
    )

    /** What [parse] made of a file: its rows, its shape, and what it couldn't read. */
    class Parsed(
        val rows: List<List<ExtraKey>>,
        val problems: List<String>,
        val sizes: Sizes = Sizes(),
    )

    /** The modifiers a key can hold or latch, by the name the file uses. */
    private val MODIFIERS =
        mapOf(
            "ctrl" to Keys.KEY_LEFTCTRL,
            "alt" to Keys.KEY_LEFTALT,
            "shift" to Keys.KEY_LEFTSHIFT,
        )

    /** A key by name: the letters and digits, then the ones a terminal asks for. */
    private val NAMES: Map<String, Int> =
        buildMap {
            for ((row, start) in listOf("qwertyuiop" to 16, "asdfghjkl" to 30, "zxcvbnm" to 44)) {
                row.forEachIndexed { i, c -> put(c.toString(), start + i) }
            }
            "1234567890".forEachIndexed { i, c -> put(c.toString(), 2 + i) }
            put("esc", Keys.KEY_ESC)
            put("backspace", Keys.KEY_BACKSPACE)
            put("tab", Keys.KEY_TAB)
            put("enter", 28)
            put("space", 57)
            put("minus", Keys.KEY_MINUS)
            put("equal", Keys.KEY_EQUAL)
            put("slash", Keys.KEY_SLASH)
            put("backslash", 43)
            put("grave", 41)
            put("semicolon", 39)
            put("apostrophe", 40)
            put("comma", 51)
            put("period", 52)
            put("leftbrace", 26)
            put("rightbrace", 27)
            put("insert", Keys.KEY_INSERT)
            put("delete", 111)
            put("home", Keys.KEY_HOME)
            put("end", Keys.KEY_END)
            put("pgup", Keys.KEY_PAGEUP)
            put("pgdn", Keys.KEY_PAGEDOWN)
            put("up", Keys.KEY_UP)
            put("down", Keys.KEY_DOWN)
            put("left", Keys.KEY_LEFT)
            put("right", Keys.KEY_RIGHT)
            put("capslock", 58)
            put("ctrl", Keys.KEY_LEFTCTRL)
            put("alt", Keys.KEY_LEFTALT)
            put("shift", Keys.KEY_LEFTSHIFT)
            put("meta", 125)
            for (f in 1..10) put("f$f", 58 + f)
            put("f11", 87)
            put("f12", 88)
        }

    /** Punctuation by the character itself, which is shorter to write than its name. */
    private val SYMBOLS =
        mapOf(
            "/" to Keys.KEY_SLASH,
            "-" to Keys.KEY_MINUS,
            "\\" to 43,
            "`" to 41,
            ";" to 39,
            "'" to 40,
            "," to 51,
            "." to 52,
            "[" to 26,
            "]" to 27,
        )

    /** How the four arrows are drawn, in degrees from up. */
    private val ARROWS = mapOf("up" to 0f, "right" to 90f, "down" to 180f, "left" to -90f)

    /** A row's height, and a label's size, have to stay usable on a phone. */
    private val SHAPE = mapOf("height" to (20f to 80f), "text" to (8f to 24f))

    /**
     * Reads [text]; a row that doesn't read is left out and named in the
     * problems, so the caller can keep the rows that last read.
     */
    fun parse(text: String): Parsed {
        val table =
            try {
                Toml.parse(text)
            } catch (e: Toml.Error) {
                return Parsed(emptyList(), listOf("keys.toml ${e.message}"))
            }
        val problems = mutableListOf<String>()
        val rows = mutableListOf<List<ExtraKey>>()
        var sizes = Sizes()
        for ((row, value) in table) {
            val shape = SHAPE[row]
            if (shape != null) {
                val number =
                    (value as? Double) ?: run {
                        problems += "$row: a number, like $row = ${if (row == "height") 34 else 12}"
                        continue
                    }
                if (number < shape.first || number > shape.second) {
                    problems += "$row: $number is outside ${shape.first.toInt()}..${shape.second.toInt()}"
                    continue
                }
                sizes = if (row == "height") sizes.copy(row = number.toFloat()) else sizes.copy(text = number.toFloat())
                continue
            }
            if (value !is String) {
                problems += "$row: a row is text in quotes, like $row = \"esc tab ctrl\""
                continue
            }
            val keys = value.split(' ', '\t').filter { it.isNotEmpty() }.mapNotNull { key(it, row, problems) }
            if (keys.isNotEmpty()) rows += keys
        }
        return Parsed(rows, problems, sizes)
    }

    /** One key of a row: `label=spec`, or just `spec` to be drawn as itself. */
    private fun key(
        token: String,
        row: String,
        problems: MutableList<String>,
    ): ExtraKey? {
        val eq = token.indexOf('=')
        val label = if (eq < 0) null else token.substring(0, eq)
        if (eq == 0) return bad(row, token, "nothing before the = to draw", problems)
        var spec = if (eq < 0) token else token.substring(eq + 1)
        val starred = spec.endsWith("*")
        if (starred) spec = spec.dropLast(1)
        val parts = spec.split('+').map { it.trim().lowercase() }
        if (parts.any { it.isEmpty() }) return bad(row, token, "a key is missing around the +", problems)
        return when {
            // A modifier on its own latches, so the soft keyboard can send ctrl and shift at all.
            parts.size == 1 && parts[0] in MODIFIERS ->
                if (starred) bad(row, token, "a modifier doesn't repeat", problems) else LatchKey(label ?: parts[0], parts[0])
            parts.size == 2 -> {
                val held = MODIFIERS[parts[0]] ?: return bad(row, token, "\"${parts[0]}\" isn't a modifier", problems)
                val code = code(parts[1]) ?: return bad(row, token, "\"${parts[1]}\" isn't a key", problems)
                Press(label ?: if (parts[0] == "ctrl") "^${parts[1]}" else "${parts[0]}+${parts[1]}", code, starred, held = held)
            }
            parts.size > 2 -> bad(row, token, "one modifier only", problems)
            else -> {
                val name = parts[0]
                val code = code(name) ?: return bad(row, token, "\"$name\" isn't a key", problems)
                // The four arrows are chevrons and repeat while held; a label draws over the chevron.
                val arrowKey = name in ARROWS
                Press(label ?: name, code, starred || arrowKey, if (arrowKey && label == null) ARROWS[name] else null)
            }
        }
    }

    /** A key name, or an evdev code. */
    private fun code(name: String): Int? = NAMES[name] ?: SYMBOLS[name] ?: name.toIntOrNull()?.takeIf { it in 1..255 }

    private fun bad(
        row: String,
        token: String,
        why: String,
        problems: MutableList<String>,
    ): Nothing? {
        problems += "$row: \"$token\": $why"
        return null
    }

    /** The rows the bar has always had, plus its ctrl keys; written to keys.toml when there is none. */
    val PRESET: String =
        """
        # The terminal's extra keys: the bar that sits above the soft keyboard in a Linux
        # window, on the home screen and in a window of its own. One line per row, top row
        # first, each row's keys left to right. Keys in a row share its width, so a row with
        # fewer keys draws wider caps, and rows needn't be the same length.
        #
        #   esc                a key, named below, drawn as its name
        #   up down left right drawn as chevrons, and they repeat while held
        #   pgup*              a trailing * repeats the key while it's held
        #   ctrl alt shift     alone: a modifier that latches, a tap holds it for the next
        #                      key, a long press locks it until tapped again
        #   ^b=ctrl+b          label=spec draws the label instead of the spec, an emoji too
        #   |=shift+backslash  a modified key holds the modifier around it, one modifier only
        #
        # Names: a-z, 0-9, esc backspace tab enter space minus equal slash backslash grave
        # semicolon apostrophe comma period leftbrace rightbrace insert delete home end pgup
        # pgdn up down left right capslock f1..f12 ctrl alt shift meta, the punctuation
        # / - [ ] ; ' , . itself, or an evdev code as a number; = is written equal, as it
        # separates a label from a spec. A row with no keys, or a file with no rows, leaves
        # the bar away.
        #
        # The bar's shape, both optional and both before the rows' text:
        #   height = 34    one row's height in dp, 20..80; 40 without it
        #   text = 12      the label size in sp, 8..24; 14 without it
        #
        # The bar shows while a soft keyboard is up: with a hardware keyboard these keys
        # already exist, so it stays off. An edit here reaches the bar as soon as it's saved.

        row1 = "esc / - home up end pgup* copy=ctrl+insert"
        row2 = "tab ctrl alt left down right pgdn* paste=shift+insert ^b=ctrl+b ^a=ctrl+a ^p=ctrl+p"
        """.trimIndent()
}
