# Extra keys

The bar of keys above the soft keyboard in a terminal: what it draws is a file
in the guest, `~/.config/omalauncher/keys/keys.toml`. Edit it and the bar
follows as soon as the file is saved — no rebuild, no restart.

It's the same bar in both places a terminal shows up: an active terminal tile
on the home screen, and a Linux window in its own activity. The ⌨ button at a
window's bottom left toggles the soft keyboard, and the bar is docked above it.
With a hardware keyboard — DeX, a Bluetooth keyboard — the bar stays away: those
keys already exist, and Gboard's floating toolbar counts as visible but has no
height of its own.

## The file

`keys.toml` lives outside the rootfs, in the launcher's own files
(`LauncherData.keysDir`: `files/keys`), and proot binds it into the guest as
`~/.config/omalauncher/keys`. Either side edits the same bytes: from inside the
guest (`$EDITOR ~/.config/omalauncher/keys/keys.toml`), or from Android's shell,
where the app's files aren't writable by the shell user directly — push it to
`/data/local/tmp` and copy it in:

```sh
adb push keys.toml /data/local/tmp/
adb shell run-as dev.omalauncher.fold cp /data/local/tmp/keys.toml files/keys/keys.toml
```

The launcher writes the file only when it doesn't exist, with the preset below.
It never writes it otherwise, so an edit of yours is only ever lost by another
edit. Deleting it brings the preset back.

## Rows

One line per row, top row first, in the order the lines appear; a row is its
keys left to right, separated by spaces. The bar's rows are as long as you like
and needn't be the same length — every key in a row shares that row's width, so
a short row draws wider caps.

```toml
row1 = "esc / - home up end pgup* copy=ctrl+insert"
row2 = "tab ctrl alt left down right pgdn* paste=shift+insert ^b=ctrl+b ^a=ctrl+a ^p=ctrl+p"
```

A row with no keys, or a file with no rows, leaves the bar away entirely (and
home stops lifting a typing tile for it). That's how to get rid of the bar
without deleting the file.

## Keys

A key's spec is a name, a name with one modifier held around it, or a raw evdev
code as a number:

| Written | Sends |
|---|---|
| `a` … `z`, `0` … `9` | that letter or digit |
| `/` `-` `\` `` ` `` `;` `'` `,` `.` `[` `]` | that punctuation, named by the character itself |
| `esc` `backspace` `tab` `enter` `space` | the usual |
| `minus` `equal` `slash` `backslash` `grave` `semicolon` `apostrophe` `comma` `period` `leftbrace` `rightbrace` | punctuation by name — `=` is written `equal`, since `=` separates a label from a spec |
| `insert` `delete` `home` `end` `pgup` `pgdn` | navigation |
| `up` `down` `left` `right` | the four arrows |
| `capslock` `meta` | Caps Lock, Super |
| `f1` … `f12` | function keys |
| `ctrl` `alt` `shift` alone | a modifier that latches, see below |
| `46`, `103` | a raw evdev code, if a key is missing from the list above |

Three kinds of decoration, on any key:

- `label=spec` draws the label instead. Terse ones read better on a narrow cap:
  `^b=ctrl+b`, `copy=ctrl+insert`. A label can be anything without a space or
  `=` — including an emoji (`😀=up`), which Android draws from the system emoji
  font. The launcher's own icon font isn't used here, so nerd-font glyphs won't
  render.
- `*` repeats the key while it's held: `pgup*`. The four arrows repeat without
  it, and keep repeating even when they have a label.
- one modifier in front of a key is held around it: `ctrl+b`, `shift+insert`,
  `alt+tab`. One at a time; `ctrl+shift+a` isn't expressible.

`up`, `down`, `left` and `right` draw as chevrons unless they have a label of
their own.

### Latches

`ctrl`, `alt` and `shift` on their own are keys that latch, since a soft
keyboard has no modifier of its own: a tap holds the modifier for the next key
only (`ctrl` then `c` sends Ctrl+C), and a long press locks it until it's tapped
again. A latched modifier is drawn outlined, a locked one filled.

## The bar's shape

Two optional settings, anywhere in the file:

```toml
height = 34   # one row's height in dp, 20..80; 40 without it
text = 12     # the label size in sp, 8..24; 14 without it
```

`height` is what makes the bar take less of the screen: three rows at 28dp are
about a fifth shorter than three at 40. The chevrons are drawn at half the row
height, and the home screen lifts a typing tile by the bar's full height, so
nothing hides behind it. Values outside the ranges are reported and ignored
rather than clamped, so a typo is visible instead of half-applied.

## A file that doesn't read

Problems are reported, not swallowed. A row that doesn't parse is left out of
the bar and named, and the bar keeps the rows that last read — as does a file
that isn't TOML at all, which changes nothing but what the launcher shows. The
problems appear over the home screen in the same panel `style.toml`'s use, one
line each:

```
keys.toml: 1 problem
row1: "bogus": "bogus" isn't a key
tap to hide
```

Nothing is written to disk for them: unlike the style, the launcher doesn't own
this file, so it doesn't keep an `errors.txt` next to it.

## How it reaches the bar

`ExtraKeysStore` reads the file before the first frame and then watches it
(`watchFile`, the same inotify watch the widget files use), so an edit arrives
as soon as it's saved — including a row added, a label changed, or the height.
Both bars read the same rows, so an edit shows in the tile and in the window.

## The preset

What a launcher with no `keys.toml` writes, and what the bar has always had
plus the ctrl keys the fork added:

```toml
row1 = "esc / - home up end pgup* copy=ctrl+insert"
row2 = "tab ctrl alt left down right pgdn* paste=shift+insert ^b=ctrl+b ^a=ctrl+a ^p=ctrl+p"
```

## Where it's implemented

- `ui/ExtraKeysSpec.kt`: the format, its key names, and the preset — plain
  Kotlin, no Android, so it's unit-tested (`ExtraKeysSpecTest`)
- `ui/ExtraKeysStore.kt`: reads and watches the file, and holds the rows, the
  sizes and the problems
- `ui/ExtraKeys.kt`: the bar itself, and the height home lifts a tile by
- `ui/Keyboard.kt`: `KeyboardBridge` sends the keys and holds the latches;
  `LatchKey` names one
- `LauncherData.keysDir` and `linux/Proot.kt`: where the file lives, and the
  bind that shows it to the guest at `~/.config/omalauncher/keys`
- `ui/Launcher.kt`: the panel that shows the problems
