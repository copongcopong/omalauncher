package dev.omalauncher.ui

import androidx.compose.ui.unit.dp
import dev.omalauncher.wayland.Keys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtraKeysSpecTest {
    private fun rows(text: String) = ExtraKeysSpec.parse(text).rows

    private fun one(text: String) = rows(text).single().single()

    private fun problems(text: String) = ExtraKeysSpec.parse(text).problems

    @Test fun presetIsTheBarsOldLayout() {
        val parsed = ExtraKeysSpec.parse(ExtraKeysSpec.PRESET)
        assertEquals(emptyList<String>(), parsed.problems)
        val (first, second) = parsed.rows
        assertEquals(
            listOf("esc", "/", "-", "home", "up", "end", "pgup", "copy"),
            first.map { (it as Press).label },
        )
        assertEquals(
            listOf("tab", "ctrl", "alt", "left", "down", "right", "pgdn", "paste", "^b", "^a", "^p"),
            second.map { if (it is Press) it.label else (it as LatchKey).label },
        )
    }

    @Test fun presetKeepsTheKeysBehaviour() {
        val first = ExtraKeysSpec.parse(ExtraKeysSpec.PRESET).rows.first()
        val up = first[4] as Press
        // Arrows draw as chevrons and repeat while held, without a * in the file.
        assertEquals(Keys.KEY_UP, up.code)
        assertTrue(up.arrow == 0f)
        assertTrue(up.repeats)
        // copy is ctrl+insert, held around it, as the clipboard keys foot, GTK and Qt know.
        val copy = first[7] as Press
        assertEquals(Keys.KEY_INSERT, copy.code)
        assertEquals(Keys.KEY_LEFTCTRL, copy.held)
        assertEquals(Keys.KEY_PAGEUP, (first[6] as Press).code)
        assertTrue((first[6] as Press).repeats)
    }

    @Test fun stickyModifiersLatch() {
        val second = ExtraKeysSpec.parse(ExtraKeysSpec.PRESET).rows[1]
        assertEquals("ctrl", (second[1] as LatchKey).modifier)
        assertEquals("alt", (second[2] as LatchKey).modifier)
    }

    @Test fun ctrlKeysNameTheirLabelAndHoldCtrl() {
        val key = one("""row1 = "^b=ctrl+b"""") as Press
        assertEquals("^b", key.label)
        assertEquals(48, key.code)
        assertEquals(Keys.KEY_LEFTCTRL, key.held)
    }

    @Test fun aModifiedKeyIsDrawnAsItsSpecUnnamed() {
        // ctrl names itself ^x, so a config with no labels still reads like a terminal's.
        assertEquals("^b", (one("""row1 = "ctrl+b"""") as Press).label)
        assertEquals("shift+insert", (one("""row1 = "shift+insert"""") as Press).label)
    }

    @Test fun plainKeysNameThemselves() {
        val keys = rows("""row1 = "esc space 1 f12 46"""")
        assertEquals(listOf("esc", "space", "1", "f12", "46"), keys.single().map { (it as Press).label })
        assertEquals(listOf(1, 57, 2, 88, 46), keys.single().map { (it as Press).code })
    }

    @Test fun starRepeatsAndRowsKeepFileOrder() {
        val parsed = ExtraKeysSpec.parse("""row2 = "pgdn*"${"\n"}row1 = "tab"""")
        assertEquals(emptyList<String>(), parsed.problems)
        assertTrue((parsed.rows[0].single() as Press).repeats)
        assertEquals(listOf(Keys.KEY_PAGEDOWN, Keys.KEY_TAB), parsed.rows.flatten().map { (it as Press).code })
    }

    @Test fun anEmptyFileIsABarWithNoKeys() {
        assertEquals(emptyList<List<ExtraKey>>(), rows(""))
        assertEquals(emptyList<String>(), problems(""))
        // A row of nothing is the same: the bar stays away.
        assertEquals(emptyList<List<ExtraKey>>(), rows("""row1 = """""))
    }

    @Test fun badKeysAreNamedAndTheRestOfTheRowStands() {
        val parsed = ExtraKeysSpec.parse("""row1 = "esc bogus tab"""" + "\n" + """row2 = "ctrl+ctrl+a"""")
        assertEquals(listOf(Keys.KEY_ESC, Keys.KEY_TAB), parsed.rows.single().map { (it as Press).code })
        assertEquals(2, parsed.problems.size)
        assertTrue(parsed.problems[0].startsWith("row1: \"bogus\": "))
        assertTrue(parsed.problems[1].startsWith("row2: \"ctrl+ctrl+a\": "))
    }

    @Test fun aFileThatIsntTomlChangesNothing() {
        assertEquals(emptyList<List<ExtraKey>>(), rows("row1 = esc"))
        assertTrue(problems("row1 = esc").single().startsWith("keys.toml line 1: "))
    }

    @Test fun punctuationIsNamedByItsCharacter() {
        val labels = rows("""row1 = "/ - equal [ ] ; ' , ."""").single().map { (it as Press).label }
        assertEquals(listOf("/", "-", "equal", "[", "]", ";", "'", ",", "."), labels)
        // TOML escapes, so a backslash is written twice.
        assertEquals(43, (rows("""row1 = "\\"""").single().single() as Press).code)
    }

    @Test fun anEmojiIsALabelLikeAnyOther() {
        val keys = rows("""row1 = "😀=up 🎉=f5 ⏎=enter"""").single().map { (it as Press).label }
        assertEquals(listOf("😀", "🎉", "⏎"), keys)
        // A label draws over the chevron an arrow key would have, and the key still repeats.
        val smile = (rows("""row1 = "😀=up"""").single().single() as Press)
        assertTrue(smile.arrow == null)
        assertTrue(smile.repeats)
        assertEquals(Keys.KEY_UP, smile.code)
    }

    @Test fun theBarIsFortyAndFourteenUnlessTheFileSaysOtherwise() {
        assertEquals(ExtraKeysSpec.Sizes(row = 40f, text = 14f), ExtraKeysSpec.parse(ExtraKeysSpec.PRESET).sizes)
        val sizes = ExtraKeysSpec.parse("height = 30" + "\n" + "text = 11" + "\n" + """row1 = "esc"""").sizes
        assertEquals(ExtraKeysSpec.Sizes(row = 30f, text = 11f), sizes)
        // One of them alone leaves the other at its default.
        assertEquals(ExtraKeysSpec.Sizes(row = 24f, text = 14f), ExtraKeysSpec.parse("height = 24").sizes)
        assertEquals(ExtraKeysSpec.Sizes(row = 40f, text = 9f), ExtraKeysSpec.parse("text = 9").sizes)
    }

    @Test fun aShapeOutsideTheRangeIsAProblemAndTheDefaultStands() {
        val parsed = ExtraKeysSpec.parse("height = 200" + "\n" + "text = 2")
        assertEquals(ExtraKeysSpec.Sizes(), parsed.sizes)
        assertEquals(listOf("height: 200.0 is outside 20..80", "text: 2.0 is outside 8..24"), parsed.problems)
    }

    @Test fun aShapeThatIsntANumberIsAProblem() {
        val parsed = ExtraKeysSpec.parse("""height = "34"""")
        assertEquals(ExtraKeysSpec.Sizes(), parsed.sizes)
        assertEquals(listOf("height: a number, like height = 34"), parsed.problems)
    }

    @Test fun theBarIsThatTallWithItsFrameAndGap() {
        // Three rows at 30dp: 90 + the frame and key gaps (2+3)*2 + the bar's gap of 6.
        assertEquals(106.dp, extraKeysHeight(ExtraKeysSpec.Sizes(row = 30f, text = 11f), 3))
        assertEquals(96.dp, extraKeysHeight(ExtraKeysSpec.Sizes(), 2))
        // No rows, no bar: home's lift adds nothing.
        assertEquals(0.dp, extraKeysHeight(ExtraKeysSpec.Sizes(), 0))
    }

    @Test fun aRowThatIsntTextIsAProblem() {
        val parsed = ExtraKeysSpec.parse("row1 = 3")
        assertEquals(emptyList<List<ExtraKey>>(), parsed.rows)
        assertTrue(parsed.problems.single().contains("a row is text in quotes"))
    }
}
