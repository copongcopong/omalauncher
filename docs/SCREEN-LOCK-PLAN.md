# Screen lock

Status: implemented, merged in #8. The lock is owl 13, since the 0.10.0 sync took 11 and 12.
Target: `copongcopong/omalauncher`.

Goal: let a widget hold the home screen still while it is being used, so a tap or a swipe
does not turn the page or open the drawer.

## Why

One page turn is a single sideways drag anywhere on home. A widget that wants a drag has to
fight the pager for it. A Browser and a Terminal get their touches only because the WebView
and the terminal consume the touch stream, and a face full of buttons or a widget that draws
gets nothing. The lock is the explicit form of what a focused page already did by accident.

## Decisions

The lock is an action, `lock(true)` and `lock(false)`, called from a handler. A property on a
node would tie it to one tile. An action lets a face decide when its own UI is in a mode that
wants the screen held.

A file that asks for the lock declares `meta { owl: 13 }`. Launchers older than that reject
the file rather than ignoring the call and turning pages anyway.

Locking holds the pager, the swipe up and the long press for the whole screen, because they
are one gesture surface. A tile cannot lock only itself.

The lock is a set of owners, not a boolean. Two widgets that both lock are both honoured, and
one stopping cannot free a screen the other holds.

Nothing is persisted. A restart, a reboot, a crash or a rebuild comes up unlocked, and there
is no way to boot into a locked home screen.

The chip is drawn by the launcher over everything. A widget cannot cover it or disable it, so
it is always the way out.

## Public API

```owl
widget "notes" {
  meta { owl: 13 }
  state editing = false
  Column {
    Button { text: editing ? "done" : "draw"; onTap: { editing = !editing; lock(editing) } }
    Canvas { weight: 1 }   // a drag that now belongs to the widget, not the pager
  }
}
```

| call | does |
|---|---|
| `lock(true)` | Holds home still until released. Asking twice is the same as asking once. |
| `lock(false)` | Releases this widget's hold. Other widgets' holds stay. |
| `lock(value)` | Any expression: a state, a comparison, a value from a source. |

A widget that never releases is not a trap. The lock ends when the widget does, and the chip
goes with it.

A focused `Browser` holds the lock by itself. It is the one tile whose focus home can see,
since tapping it sets `TerminalTiles.browser` and a tap outside or Back clears it. It is an
owner in the same set as a widget's `lock(true)`, so the two coexist. Editing home or
arranging a page releases it, or a page could never be moved. The chip and Back let go of the
page too, because releasing the hold alone would let the focused page take it straight back.

## What "locked" turns off

| gesture | unlocked | locked |
|---|---|---|
| swipe left/right | turns the page | does nothing |
| swipe up | drawer, per gesture settings | does nothing |
| long-press a tile | lifts it, opens its menu | does nothing |
| tap the prompt bar, the recent strip | opens apps, switches | does nothing |
| tap the locked widget | the widget | the widget |
| system back gesture, notifications, status bar | Android's | Android's, unchanged |

The widget's own taps keep working. The lock is about home's gestures, not a shield over
touches.

## Release matrix

A hold ends when:

| event | why |
|---|---|
| `lock(false)` from the widget that asked | the normal path |
| the widget stops, is deleted or renamed | its process is gone, and a hold with no owner is a bug |
| the launcher restarts, the phone reboots, the app crashes | nothing is persisted |
| the unlock button on the chip | the escape hatch |

Two releases the plan first promised are not in this version, because neither has a signal to
hang on. One was "the node that asked leaves composition"; the lock belongs to a widget while
composition is per node, so a node going away cannot say whether the widget still works. The
other was "the locked tile loses focus"; on home, focus exists only for a terminal, a widget's
text field and a Browser page, so a plain widget that locks, which is the case this feature is
for, never loses focus in a way the launcher can see. The chip covers both gaps, and both are
worth doing once home has a real notion of which tile is active.

Opening another app does release the lock, because a widget that is not on screen is stopped
and a stopped widget's hold goes with it. Coming back after a detour finds the screen
unlocked. That is the stop rule at work, and it is what the device showed.

Android's own gestures (notification shade, recents, home) are never intercepted, so a user
can always leave.

Back reaches the locked widget first, as it does today: a page's history, a terminal's
control, an open menu. If nothing consumes it, Back releases the lock and undoes nothing else,
so a focused tile never swallows Back twice.

## The cue

While locked, the launcher draws two things over everything. A 2 dp border runs around the
screen edge in `Palette.accent`, with `corners` applied, so it follows the theme and the
corner style instead of being the only hard-cornered thing on screen. A chip sits at top
centre with a lock glyph, the words `screen locked`, and an unlock button. Tapping the button
releases the lock. Tapping the text does nothing, so a swipe passing over the chip cannot
unlock by accident.

On a face, which fills the whole page, the border is drawn at 60% alpha so it reads as a layer
over the artwork instead of a frame around it. The chip is unchanged.

The cue is composed above the pages, so a widget that covers the tile area cannot cover the
way out.

## Implementation

Language, in `owl/src/main/kotlin/dev/omalauncher/widgets/lang/`:

- `Schema.kt` has `SINCE["lock"] = 13`.
- `Builtins.kt` has `"lock" to 1..1`.
- `Compiler.kt` has `LEVEL = 13`, refuses a constant argument that is not a boolean, and calls
  `since("lock", call.pos)` at the call.
- The level check and the arity check come from the existing action path.

Runtime, in `app/src/main/kotlin/dev/omalauncher/widgets/runtime/WidgetInstance.kt`: an
`onLock: ((Boolean) -> Unit)?` next to `onScreen`, null where there is no home to hold.
`act()` intercepts `lock` before `services.act`, coerces the argument the way the other
builtins do, calls `onLock`, and reports a message in a preview instead of locking. `stop()`
calls `onLock(false)`, so every stop path drops the hold.

Home, in `app/src/main/kotlin/dev/omalauncher/ui/`:

- `TerminalTiles.kt` owns the holds in `HomeLock`, a `mutableStateMapOf<Any, Unit>` keyed by
  owner, plus `locked`, `lock(owner, on)` and `unlockAll()`. It also keeps `focusLock`, the
  page's own entry, which `syncFocusLock(editing)` adds or drops.
- `Home.kt` reads `terminals.locked` in the `homeSwipe` condition, in a `LaunchedEffect` that
  syncs the page's hold, and in a `BackHandler` that deactivates the page and unlocks.
- `HomeGrid.kt` takes `locked`, sets the pager's `userScrollEnabled` from it, and returns early
  from the long-press gesture before consuming anything.
- `LockCue.kt` draws the border and the chip, and `lockedTaps` on the prompt bar, the recent
  strip and the side rail consumes only the press, so they still draw and scroll.
- `WidgetServices.kt` and `WidgetEngine.kt` pass `onLock` where `onScreen` is passed, wired to
  `terminals.lock(instance, on)`.

## Tests

`LangTest` covers the level gate (`lock(true)` without owl 13 fails with the level message),
the arity errors for no argument and for two, the refusal of a constant string or number, and
a computed value reaching the host as `lock true` then `lock false`.

`ScreenLockTest` covers the owner accounting through `WidgetInstance`: two owners, one
releasing, still locked; asking twice counting once; `stop()` dropping only its own hold;
`unlockAll()` clearing both; a preview reporting the message.

## Acceptance criteria

- A widget that calls `lock(true)` freezes page turns, the swipe up and the long press, and the
  chip and border appear.
- `lock(false)`, the chip, Back with nothing else to consume it, the widget stopping, its file
  being removed and a launcher restart each release the lock.
- Two widgets locking: releasing one leaves the other held.
- Nothing changes for widgets that never call `lock`.
- `./gradlew :owl:test :app:testDebugUnitTest :app:assembleDebug` passes on pop480, and the
  release matrix is checked on the device.

## Risks

A user stuck behind a widget is the main one. The chip is drawn by the launcher and cannot be
covered, and the lock never survives a restart. The chip is the safety property, not a nicety.

A widget that locks on load and forgets is legal, and the chip is the answer. The README says
to tie a lock to a mode the user can see rather than to a page load.

Lock and a terminal's own Back both want Back. The order is defined above: typing first, then
lock.

The border over a face's artwork is a taste call. The alternative is the chip alone, which
reads as "frozen" rather than "locked".

`TerminalTiles` now owns focus, the lift, the keyboard and the lock. Ripping it apart into
`HomeFocus` is a separate change.
