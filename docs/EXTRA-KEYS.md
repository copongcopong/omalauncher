# Extra keys

> **~~The terminal's extra-keys bar is a file in the guest, `keys.toml`: its rows, keys, height
> and label size are edited there and the bar follows on save. It's the same bar on the home
> screen and in a Linux window, and the launcher writes the preset below when the file is
> missing.~~**

**Retired in `0.11.0-z`, in favour of upstream's own.** Upstream 0.11.0 turned the key bar into an
owl file, `keybar.owl`: a `keybar "…" { }` file that you or an agent edit, with the rows as nodes,
`pressKey`, `typeText` and `latch` for what a key sends, and `keybar.gaps-out` plus
`keybar.border-width` in Style for its frame. That covers what `keys.toml` did, so the fork
adopted it and stopped reading `keys.toml`. Upstream documents the file in
[app/src/main/assets/widgets/docs/keybar.md](../app/src/main/assets/widgets/docs/keybar.md), and
`keybar.owl` with `.stock-keybar.owl` beside it is in `~/.config/omalauncher/widgets/`.

This fork ships the rows `keys.toml` had as its preset, so a bar nobody edited looks as it did:
`esc / - ; home end pgup pgdn copy paste` over `tab ctrl alt` and the arrows, `^b ^a ^p`, at 36 dp
a row and 12 sp labels.

## Where each line of a `keys.toml` went

| `keys.toml` | in `keybar.owl` |
|---|---|
| `row1 = "esc / - home end"` | one entry per key in a row's list: `{label: "esc", key: "esc"}` |
| `pgup*` | `{label: "pgup", key: "pgup", repeats: true}` |
| `up`, `down`, `left`, `right` | the same, with `arrow: 0`, `180`, `-90` and `90` to draw the chevron |
| `^b=ctrl+b` | `{label: "^b", key: "ctrl+b"}`, and any label works, emoji included |
| `ctrl` `alt` `shift` alone | `{label: "ctrl", latch: "ctrl"}`, with `latch(...)` doing the tap and long press |
| `ctrl+b`, `shift+insert` | `key: "ctrl+b"`, `key: "shift+insert"`, now with any combination: `ctrl+shift+a` works |
| `height = 36` | `Row { height: 36 }` in the bar's frame |
| `text = 12` | `fontSize: 12` on a key's `Text` |
| a raw evdev code, like `103` | no equivalent: `pressKey` takes a key's name or a character |
| `row2 = "..."` with fewer keys | the keys share the row by `weight: 1` boxes, as before |

Anything a `keys.toml` couldn't do is there now: a key that types a whole command with
`typeText("git status\n")`, layers that swap rows, keys drawn with images and the theme's own
colors. `examples/keybar-layers.owl`, `keybar-vim.owl`, `keybar-commands.owl` and
`keybar-gamepad.owl` show each.

## If you edited yours

Your rows go into the `let rows = [ ... ]` list in `keybar.owl`, one entry per key. That file is
yours: the launcher seeds it once and never writes over your version, and `.stock-keybar.owl` next
to it is the bar as this launcher ships it, which it does keep up to date. Style › terminal › key
bar reads whether the file is still the stock one, has errors, or was removed, and can put the
stock bar back.
