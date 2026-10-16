package dev.omalauncher.linux

import java.io.File

/**
 * The section of foot.ini that foot takes its background `alpha` in.
 *
 * It isn't one name. Up to foot 1.24 the colours live in `[colors]`; from 1.26 they split into
 * `[colors-dark]` and `[colors-light]`, and `alpha` moved with them. foot refuses a section it
 * doesn't know ("invalid section name: colors") and says so inside the terminal, so the section is
 * read from the guest rather than assumed. Plain Kotlin, so the choice is unit-tested.
 */
object TerminalColors {
    /**
     * The section [rootfs] says foot accepts, or null when nothing does: the theme's own foot.ini
     * names it, and the installed package's version decides when that file isn't there. Null means
     * no alpha is written at all, which leaves a terminal opaque rather than breaking it.
     */
    fun section(rootfs: File): String? {
        themeSection(rootfs)?.let { return it }
        val version = installedFootVersion(rootfs) ?: return null
        return if (version >= 1.26) ColorsDark else Colors
    }

    fun themeSection(rootfs: File): String? =
        runCatching {
            File(rootfs, THEME)
                .takeIf { it.isFile }
                ?.useLines { lines -> lines.map(String::trim).firstOrNull(::isColorsHeader) }
        }.getOrNull()?.trim('[', ']')

    /** The foot package's version as major.minor, from pacman's database: no guest command, and it works before Linux runs. */
    fun installedFootVersion(rootfs: File): Double? {
        val entry = File(rootfs, PACMAN).listFiles()?.firstOrNull { it.isDirectory && it.name.startsWith("foot-") } ?: return null
        val desc = File(entry, "desc").takeIf { it.isFile }?.readText() ?: return null
        val version = desc.lines().dropWhile { it.trim() != "%VERSION%" }.getOrNull(1)?.trim() ?: return null
        val parts = version.substringBefore('-').split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        return major + (parts.getOrNull(1)?.toIntOrNull() ?: 0) / 100.0
    }

    private fun isColorsHeader(line: String) = line.startsWith("[colors") && line.endsWith("]")

    /** foot 1.26 and later: the colours split by theme, and alpha went with them. */
    const val ColorsDark = "colors-dark"

    /** foot 1.24 and earlier. */
    const val Colors = "colors"

    private const val THEME = "root/.local/state/omarchy/current/theme/foot.ini"
    private const val PACMAN = "var/lib/pacman/local"
}
