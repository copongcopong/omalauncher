package dev.omalauncher.linux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TerminalFontTest {
    @get:Rule val tmp = TemporaryFolder()

    /** A typical phone: density 2.625, so foot's scale is 2. */
    @Test fun phoneTextIsBiggerThanTheOldFixedSize() {
        val size = TerminalFont.pointSize(2.625f, 1f, 2)
        assertEquals(12.0, size, 0.0)
        assertTrue(size > 8.0)
    }

    /** A tablet at 1.875 gets scale 1: the old size 8 drew a 6dp em there. */
    @Test fun tabletMakesUpForTheRoundedDownScale() {
        assertEquals(17.0, TerminalFont.pointSize(1.875f, 1f, 1), 0.0)
    }

    @Test fun followsAndroidsFontSize() {
        assertEquals(15.5, TerminalFont.pointSize(2.625f, 1.3f, 2), 0.0)
    }

    /** 1080 px wide at scale 2 is the phone foot.ini's old size 8 was tuned for. */
    @Test fun btopFitsEightyColumnsInPortrait() {
        val size = TerminalFont.fitting(80, 1080, 2, 12.0)
        assertEquals(8.0, size, 0.0)
        val cell = 0.6 * size * 96 / 72 * 2
        assertTrue(80 * Math.ceil(cell) + 2 * 8 * 2 <= 1080)
    }

    @Test fun fittingNeverGrowsTheSize() {
        assertEquals(12.0, TerminalFont.fitting(80, 2400, 2, 12.0), 0.0)
    }

    /** Measured with tput on a 1080 px phone: 65 columns at 10pt, 55 at 12. */
    @Test fun countsColumnsLikeFoot() {
        assertEquals(65, TerminalFont.columns(1080, 2, 10.0))
        assertEquals(55, TerminalFont.columns(1080, 2, 12.0))
    }

    @Test fun textSizesStepTheEm() {
        assertEquals(10.0, TerminalFont.pointSize(2.625f, 1f, 2, TerminalText.Smaller.em), 0.0)
        assertEquals(14.0, TerminalFont.pointSize(2.625f, 1f, 2, TerminalText.Larger.em), 0.0)
    }

    /** What fc-list finds in a fresh guest: the icon-only Nerd Font is foot's fallback, not a choice. */
    @Test fun listsTextFontsOnly() {
        val found = listOf("DejaVu Sans Mono", "JetBrains Mono", "JetBrains Mono NL", "Symbols Nerd Font Mono", "Noto Color Emoji", "")
        assertEquals(listOf("DejaVu Sans Mono", "JetBrains Mono", "JetBrains Mono NL"), found.filter(TerminalFont::isTextFont))
    }

    /** foot's background alpha is what makes a terminal see-through without fading its text. */
    @Test fun theIncludeCarriesFootAlphaOnlyWhenTheBackgroundIsTranslucent() {
        assertEquals("[main]\nfont=JetBrains Mono:size=12.0\n", TerminalFont.includeText("JetBrains Mono", 12.0, 1f, "colors-dark"))
        assertEquals(
            "[main]\nfont=JetBrains Mono:size=12.0\n\n[colors-dark]\nalpha=0.85\n",
            TerminalFont.includeText("JetBrains Mono", 12.0, 0.85f, "colors-dark"),
        )
        // Up to foot 1.24, and anything that says so, keeps the plain section.
        assertEquals(
            "[main]\nfont=JetBrains Mono:size=12.0\n\n[colors]\nalpha=0.50\n",
            TerminalFont.includeText("JetBrains Mono", 12.0, 0.5f, "colors"),
        )
        // Nothing to write it into: better opaque than a section foot would refuse.
        assertEquals("[main]\nfont=JetBrains Mono:size=12.0\n", TerminalFont.includeText("JetBrains Mono", 12.0, 0.5f, null))
        // Two decimals, not the font's one: 0.75 as 0.8 would be a different value.
        assertEquals("0.75", TerminalFont.formatAlpha(0.75f))
        assertEquals("0.30", TerminalFont.formatAlpha(0.3f))
    }

    /**
     * foot rejects a section it doesn't know ("invalid section name: colors"), so the section comes
     * from the guest: its theme's own foot.ini, and pacman's version when that file isn't there.
     */
    @Test fun theColorsSectionComesFromTheGuest() {
        val rootfs = File(tmp.root, "arch")
        // foot 1.28's omarchy theme, as a Fold has it: the section is colors-dark, not colors.
        File(rootfs, "root/.local/state/omarchy/current/theme").mkdirs()
        File(rootfs, "root/.local/state/omarchy/current/theme/foot.ini").writeText("[colors-dark]\nbackground=2c2525\n")
        assertEquals("colors-dark", TerminalColors.section(rootfs))
        // Nothing to read: pacman's database, where 1.28 split the colours and 1.24 had not.
        File(rootfs, "root/.local/state/omarchy/current/theme/foot.ini").delete()
        assertEquals(null, TerminalColors.section(rootfs))
        val db = File(rootfs, "var/lib/pacman/local/foot-1.28.0-2")
        db.mkdirs()
        File(db, "desc").writeText("%NAME%\nfoot\n\n%VERSION%\n1.28.0-2\n")
        assertEquals("colors-dark", TerminalColors.section(rootfs))
        File(rootfs, "var/lib/pacman/local/foot-1.28.0-2").renameTo(File(rootfs, "var/lib/pacman/local/foot-1.24.0-1"))
        File(rootfs, "var/lib/pacman/local/foot-1.24.0-1/desc").writeText("%NAME%\nfoot\n\n%VERSION%\n1.24.0-1\n")
        assertEquals("colors", TerminalColors.section(rootfs))
    }
}
