package dev.omalauncher.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import dev.omalauncher.LauncherData
import dev.omalauncher.upgrade.writeAtomically
import dev.omalauncher.widgets.runtime.watchFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * keys.toml: the extra-keys bar's rows, read at startup and watched after, so
 * an edit made in the terminal reaches the bar as soon as the file is saved.
 * Linux sees the file as ~/.config/omalauncher/keys/keys.toml.
 *
 * The launcher doesn't write the bar's rows, only the preset when there's no
 * file at all; a file that doesn't read keeps the rows that last did.
 */
class ExtraKeysStore(
    private val context: Context,
) {
    private val file = File(LauncherData.keysDir(context), FILE)

    /** The bar's rows, empty when the file asks for no bar. */
    var rows by mutableStateOf(preset.rows)
        private set

    /** The bar's shape: one row's height and the label size. */
    var sizes by mutableStateOf(preset.sizes)
        private set

    /** What the file got wrong, for the launcher to show; the bar keeps the last rows that read. */
    var errors by mutableStateOf(emptyList<String>())
        private set

    /** What home lifts a typing tile by: the bar's height for its rows, nothing when there are none. */
    val height: Dp get() = extraKeysHeight(sizes, rows.size)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Small, and read before the first frame, so the bar is right when a window opens.
        val text = runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()
        if (text == null) restorePreset() else take(text)
    }

    fun start() {
        scope.launch { watchFile(file).collect { take(it) } }
    }

    /** Takes in the file's [text]; "" is a file emptied or deleted by hand. */
    private fun take(text: String) {
        if (text.isBlank()) {
            // Deleted: the preset comes back, so the file is there to edit. Emptied: no bar.
            if (file.isFile) {
                rows = emptyList()
                errors = emptyList()
            } else {
                restorePreset()
            }
            return
        }
        val parsed = ExtraKeysSpec.parse(text)
        // A file that doesn't read changes nothing but what the launcher shows.
        if (parsed.problems.isEmpty()) {
            rows = parsed.rows
            sizes = parsed.sizes
            errors = emptyList()
        } else {
            errors = parsed.problems
        }
    }

    private fun restorePreset() {
        runCatching {
            file.parentFile?.mkdirs()
            writeAtomically(file, ExtraKeysSpec.PRESET.toByteArray())
        }.onFailure { Log.w(TAG, "can't write $FILE", it) }
        rows = preset.rows
        sizes = preset.sizes
        errors = emptyList()
    }

    companion object {
        const val FILE = "keys.toml"

        private const val TAG = "ExtraKeys"

        private val preset = ExtraKeysSpec.parse(ExtraKeysSpec.PRESET)
    }
}
