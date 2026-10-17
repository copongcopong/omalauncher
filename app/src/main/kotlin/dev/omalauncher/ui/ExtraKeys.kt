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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Keycap and gap sizes that don't come from the file: the frame, its gap, and the bar's side gap. */
internal val KeyGap = 3.dp
internal val FrameWidth = 2.dp

/**
 * One row's height where a bar isn't the terminal's: the editor's key row draws with the
 * same keycaps. The extra-keys bar takes its own from keys.toml instead ([ExtraKeysSpec.Sizes]).
 */
internal val KeyRowHeight = 40.dp

/** The gap between the bar and the keyboard, and to the screen's sides: the tiles' gap. */
private val Gap = 6.dp

/**
 * The bar's full height for [sizes] and [rowCount] rows, gap included, and nothing
 * when it has no rows; the home screen lifts a typing tile by this much more.
 */
fun extraKeysHeight(
    sizes: ExtraKeysSpec.Sizes,
    rowCount: Int,
): Dp = if (rowCount <= 0) 0.dp else sizes.row.dp * rowCount + (FrameWidth + KeyGap) * 2 + Gap

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
 * and lock on a long press. The rows and the size of them are [ExtraKeysStore]'s,
 * so editing keys.toml redraws them.
 */
@Composable
fun ExtraKeys(
    bridge: KeyboardBridge,
    rows: List<List<ExtraKey>>,
    sizes: ExtraKeysSpec.Sizes,
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
            Row(Modifier.fillMaxWidth().height(sizes.row.dp)) {
                for (key in row) {
                    val cap = Modifier.weight(1f).fillMaxHeight().padding(KeyGap)
                    when (key) {
                        is Press -> PlainKey(key, bridge, sizes, cap)
                        is LatchKey -> bridge.latch(key.modifier)?.let { StickyKey(key, it, sizes, cap) }
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
    sizes: ExtraKeysSpec.Sizes,
    modifier: Modifier,
) = RepeatingKeyCap(
    key.label,
    key.arrow?.let { Icons.Chevron },
    key.arrow ?: 0f,
    key.repeats,
    modifier,
    { bridge.tap(key.code, key.held) },
    // Half the row, so a shorter bar gets a smaller chevron with it.
    iconSize = (sizes.row * 0.5f).coerceAtMost(20f).dp,
    labelSize = sizes.text.sp,
)

/**
 * A keycap that acts on touch down, and keeps acting while held when it [repeats]:
 * the terminal's extra keys and the editor's. Shows [icon] turned by [rotation], else [label].
 */
@Composable
internal fun RepeatingKeyCap(
    label: String,
    icon: ImageVector?,
    rotation: Float,
    repeats: Boolean,
    modifier: Modifier,
    onPress: () -> Unit,
    // The terminal's bar sizes its caps from keys.toml; the editor's key row takes the defaults.
    iconSize: Dp = 20.dp,
    labelSize: TextUnit? = null,
) {
    val view = LocalView.current
    val press by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }
    KeyCap(
        modifier.pointerInput(label, repeats) {
            presses { scope ->
                pressed = true
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val job =
                    scope.launch {
                        press()
                        if (!repeats) return@launch
                        delay(RepeatDelay)
                        while (true) {
                            press()
                            delay(RepeatInterval)
                        }
                    }
                waitForUpOrCancellation()
                job.cancel()
                pressed = false
            }
        },
        fill = if (pressed) Palette.accent else Palette.surface,
    ) {
        val color = if (pressed) Palette.background else Palette.foreground
        if (icon != null) {
            Image(icon, label, Modifier.size(iconSize).rotate(rotation), colorFilter = ColorFilter.tint(color))
        } else {
            BasicText(
                label,
                style = if (labelSize != null) Mono.copy(color = color, fontSize = labelSize) else Mono.copy(color = color),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StickyKey(
    key: LatchKey,
    sticky: StickyModifier,
    sizes: ExtraKeysSpec.Sizes,
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
                    fontSize = sizes.text.sp,
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
internal fun KeyCap(
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
internal suspend fun PointerInputScope.presses(press: suspend AwaitPointerEventScope.(CoroutineScope) -> Unit) =
    coroutineScope {
        val scope = this
        awaitEachGesture {
            awaitFirstDown()
            press(scope)
        }
    }

internal const val RepeatDelay = 400L
internal const val RepeatInterval = 50L
