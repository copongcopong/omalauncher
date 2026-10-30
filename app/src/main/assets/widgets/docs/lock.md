# Screen lock

Part of the widget docs: README.md, one folder up, comes first, and paths here are from the widgets folder.

A widget that wants a drag has to fight the pager for it: one page turn is a sideways drag
anywhere on home. `lock(true)` holds home still until it is released (owl 18, this fork's):

```owl
widget "sketch" {
  meta { owl: 18 }
  state editing = false
  Column {
    Row {
      gap: 8
      Text { text: editing ? "locked" : "pad"; weight: 1 }
      Button { text: editing ? "done" : "draw"; onTap: { editing = !editing; lock(editing) } }
    }
    Canvas { weight: 1 }   // a drag in here now belongs to the widget, not the pager
  }
}
```

- A page that has the tile holds the lock by itself; tapping the prompt bar or another tile is a
  tap outside it, so it lets go and the lock with it. A `lock(true)` a widget asked for is not tied
  to focus, so it keeps holding while the bar and the strip ignore taps.
- `lock(true)` stops what home answers with a swipe or a long press: the page turning, the swipe up
  to the drawer, a tile lifting, and taps on the prompt bar and the recent strip. The widget's own
  taps keep working; the notification shade and Home still belong to Android, as before.
- A Browser tile holds it by itself once it has been tapped: a page's drags are its own, so home waits
  for it to let go (a tap outside the tile, Back, or the chip). Editing home, or arranging a page,
  lets go, so a page can still be moved.
- It applies to the whole screen. Home's gestures are one surface, so a locked screen holds for
  every tile on it. Two widgets that lock are both honoured, and one letting go leaves the other's
  hold.
- While it is held, the launcher draws a border around the screen and a chip at top centre, and
  the chip's button releases the lock. The launcher draws both over everything, so no widget can
  cover the way out.
- It ends with `lock(false)`, when the widget stops or its file is removed, through the chip, and
  with Back when nothing else on the tile wants it (a page's history, a terminal's control, an open
  menu). A restart, a reboot or a crash comes up unlocked: the lock is never kept. A tap outside the
  tile does not end it by itself, so while the widget is on screen the chip is the way out.
- Nothing tells a widget whether it still holds the lock, so let the mode say what it is. The chip
  frees the screen without changing the widget's own state.
- Don't lock on page load. A lock belongs to a mode the user turned on and can see, as `editing`
  is above: the chip is the fallback, not the way out. In owl-check's preview there is no home screen
  to hold, so `lock(...)` reports instead of locking.

`examples/47-lock.owl` is a pad that locks while it is drawn on, ready to move up into `widgets/`
and use.
