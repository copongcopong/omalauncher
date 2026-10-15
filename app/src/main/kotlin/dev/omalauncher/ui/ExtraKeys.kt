package dev.omalauncher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One row's height, keycap and the gap around it; the bar's rows come from [ExtraKeysStore]. */
private val RowHeight = 40.dp
private val KeyGap = 3.dp
private val FrameWidth = 2.dp

/** The gap between the bar and the keyboard, and to the screen's sides: the tiles' gap. */
private val Gap = 6.dp

/**
 * The bar's full height for [rowCount] rows, gap included, and nothing when it has
 * no rows; the home screen lifts a typing tile by this much more.
 */
fun extraKeysHeight(rowCount: Int): Dp =
    if (rowCount <= 0) 0.dp else RowHeight * rowCount + (FrameWidth + KeyGap) * 2 + Gap

/**
 * Whether the extra-keys row belongs on screen: only over a docked soft
 * keyboard. With a hardware keyboard, Gboard shows just a floating toolbar
 * that counts as visible but takes no height, and the hardware keyboard has
 * these keys already.
 */
@Composable
fun extraKeysWanted(): Boolean {
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    // Derived, so the keyboard's slide-in animation doesn't recompose every frame.
    val docked by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    return docked
}

/**
 * The keys a phone keyboard lacks, docked above it for terminals: a framed
 * panel of keycaps, like the tiles above it. Keys never take focus, so the
 * soft keyboard stays up; ctrl, alt and shift latch for the next key on a tap
 * and lock on a long press. The rows are [ExtraKeysStore]'s, so editing
 * keys.toml redraws them.
 */
@Composable
fun ExtraKeys(
    bridge: KeyboardBridge,
    rows: List<List<ExtraKey>>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = Gap, end = Gap, bottom = Gap)
            .frame(FrameWidth, Palette.border, Palette.background, radius = 12.dp)
            .padding(KeyGap),
    ) {
        for (row in rows) {
            Row(Modifier.fillMaxWidth().height(RowHeight)) {
                for (key in row) {
                    val cap = Modifier.weight(1f).fillMaxHeight().padding(KeyGap)
                    when (key) {
                        is Press -> PlainKey(key, bridge, cap)
                        is LatchKey -> bridge.latch(key.modifier)?.let { StickyKey(key, it, cap) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlainKey(
    key: Press,
    bridge: KeyboardBridge,
    modifier: Modifier,
) {
    val view = LocalView.current
    var pressed by remember { mutableStateOf(false) }
    KeyCap(
        modifier.pointerInput(key) {
            presses { scope ->
                pressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val press =
                    scope.launch {
                        bridge.tap(key.code, key.held)
                        if (!key.repeats) return@launch
                        delay(RepeatDelay)
                        while (true) {
                            bridge.tap(key.code, key.held)
                            delay(RepeatInterval)
                        }
                    }
                waitForUpOrCancellation()
                press.cancel()
                pressed = false
            }
        },
        fill = if (pressed) Palette.accent else Palette.surface,
    ) {
        val color = if (pressed) Palette.background else Palette.foreground
        if (key.arrow != null) {
            Image(Icons.Chevron, key.label, Modifier.size(20.dp).rotate(key.arrow), colorFilter = ColorFilter.tint(color))
        } else {
            BasicText(key.label, style = Mono.copy(color = color))
        }
    }
}

@Composable
private fun StickyKey(
    key: LatchKey,
    sticky: StickyModifier,
    modifier: Modifier,
) {
    val view = LocalView.current
    val latch = sticky.latch
    KeyCap(
        modifier.pointerInput(sticky) {
            presses { scope ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                var locked = false
                val hold =
                    scope.launch {
                        delay(RepeatDelay)
                        sticky.latch = Latch.Locked
                        locked = true
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                val up = waitForUpOrCancellation() != null
                hold.cancel()
                if (up && !locked) sticky.latch = if (sticky.latch == Latch.Off) Latch.Once else Latch.Off
            }
        },
        // Once: outlined in the accent, for the next key. Locked: filled, until tapped off.
        fill = if (latch == Latch.Locked) Palette.accent else Palette.surface,
        outline = if (latch == Latch.Once) Palette.accent else null,
    ) {
        BasicText(
            key.label,
            style =
                Mono.copy(
                    color =
                        when (latch) {
                            Latch.Off -> Palette.foreground
                            Latch.Once -> Palette.accent
                            Latch.Locked -> Palette.background
                        },
                ),
        )
    }
}

@Composable
private fun KeyCap(
    modifier: Modifier,
    fill: Color,
    outline: Color? = null,
    content: @Composable () -> Unit,
) {
    val shape = LauncherStyle.shape(8.dp)
    Box(
        modifier
            .clip(shape)
            .background(fill, shape)
            .then(if (outline != null) Modifier.border(FrameWidth, outline, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Runs [press] for each touch on the key, with a scope to start key repeat or long-press timers in. */
private suspend fun PointerInputScope.presses(press: suspend AwaitPointerEventScope.(CoroutineScope) -> Unit) =
    coroutineScope {
        val scope = this
        awaitEachGesture {
            awaitFirstDown()
            press(scope)
        }
    }

private const val RepeatDelay = 400L
private const val RepeatInterval = 50L
