# The fork

Omalauncher Z is [copongcopong/omalauncher](https://github.com/copongcopong/omalauncher), a
fork of [SimonSchubert/omalauncher](https://github.com/SimonSchubert/omalauncher). Upstream
stays the source of everything the launcher does. This fork carries a small set of additions,
and this file lists them so a sync or a release does not have to rediscover them.

It has a second use. When one of these additions is worth sending upstream, this file is the
starting point: each entry says which level it uses, which files it touches, and what an
upstream PR would still need.

Demos of the fork on the Fold: [one](https://youtube.com/shorts/D3fvVJxXpGE),
[another](https://youtube.com/shorts/rUvdpGPZgIY).

Keep this file current. When a fork change lands, add or edit its entry here in the same
change. When a sync lands, re-read it against the new upstream and fix whatever moved.

## Identity

| | |
|---|---|
| package | `dev.omalauncher.fold` |
| version name | `<upstream>-z`, e.g. `0.18.0-z` |
| version code | one above upstream's, so it installs over an upstream build, e.g. `30` |
| signing | release falls back to the debug key, which is what the phone's install has |
| releases | tag `v<versionName>`, built by `.github/workflows/fork-release.yml` |

## Keeping up with upstream

Fetch, branch as `sync/upstream-<version>`, merge, resolve, build, then open a PR.

Conflicts so far land in the same places every time: `CHANGELOG.md`, `README.md`,
`app/build.gradle.kts`, `app/src/main/assets/widgets/README.md` and `docs/versions.md`, the
widget sizes in `10-today.owl` and `20-fetch.owl` and the short-text threshold in `60-owl.owl`,
our rows in `keybar.owl`, `fastlane`'s changelog for the versionCode both sides used, and the owl
language tables (`Schema.kt`, `Builtins.kt`, `Compiler.kt`, `LangTest.kt`), because both sides
add to them. `Home.kt` and `Launcher.kt` conflict when upstream rewrites home, which it does
regularly, and the settings hub and style screen conflict when upstream moves their text into
string resources.

Since 0.15.0 upstream keeps every piece of UI text in `res/values/strings_*.xml` and runs
`HardcodedTextTest`, which fails the build on a literal passed to a text call. Fork text follows
that: a label we add goes in `strings_settings.xml`, `strings_style.xml` or `strings_home.xml`
and then in the files for the other five languages beside it (`values-in`, `values-es`,
`values-pt`, and since 0.17.0 `values-de` and `values-zh`). Widget-side text goes in the file's
own `strings { }` the same way.

Every file this document used to call the fork's own is shared ground now. `TerminalTiles.kt`
conflicts because upstream gave terminals a translucent window for widgets' glass and the keys
their hand-back; `HomeGrid.kt` conflicts because upstream moved a tile into `HomeTile` and makes
the pages either side of the one showing. `BrowserView.kt`, `LockCue.kt`, `docs/browser.md`,
`ScreenLockTest.kt`, `TileLiftTest.kt` and `WidgetInstanceBrowserTest.kt` came back as upstream's
after #254 and #255, so a sync takes upstream's copy of them rather than merging.

Check the language level first when a sync starts. Upstream and the fork have collided four
times: see [Owl levels](#owl-levels).

After the merge: bump `versionName` to `<upstream>-z` and `versionCode` to upstream's plus one,
add a `CHANGELOG.md` section at the top, and update this file.

Syncs so far, each its own branch and PR: `sync/upstream-011`, `sync/upstream-012`,
`sync/upstream-016` (0.14.0 to 0.16.0 together), `sync/upstream-018` (0.17.0 and 0.18.0) and
`sync/upstream-019` (0.19.0, the sync that gave the fork's two features back to upstream).

One check on 0.18.0-z is red for a reason outside the merge: upstream's `main` fails lint with
`Previews.kt:142: Error: Statement must run from @WorkerThread ... [ThreadConstraint]`, from its
own crash fix that put the preview's dismissal in `finally`, and the same run fails on
SimonSchubert/omalauncher. The fork's build and tests pass on pop-480; drop this note once
upstream's lint is green again.

`fastlane`'s Play notes are upstream's and stay upstream's since the 0.18.0-z sync. The fork
does not publish to Play and its GitHub release notes come from `fork-release.yml`, so the
version codes the two sides share (25, 28) stop costing a conflict and upstream's notes stop
being dropped.

## Owl levels

The level in `meta { owl: N }` is a promise to older launchers: a file that asks for level N
either works or is rejected cleanly. Numbers are global, so two features cannot share one.

| level | whose | what |
|---|---|---|
| 11 | upstream | panels, `android.drawer`, `appAction`, `workApps`, `shortcuts`, lazy Columns and Grids, `Image.label`, `Text.minLines`, `chunk`, `rank`, `strongMatch`, app `statusBar` |
| 12 | upstream | `screenTop`, how much of an app's top a camera cutout covers; `keybar "…" { }` files, `pressKey`, `typeText`, `latch`, `keybar.gaps-out` and `keybar.border-width` |
| 13 | upstream | the phone's own controls: `settings`, `dnd`, `torch(level)`, `volume`'s stream, `notification("clear")`/`"action"`/`"reply"`/`"snooze"`/`"snoozeAll"`/`"unsnooze"`/`"unsnoozeAll"`, `drag` and `onDragEnd`, `android.network`, `android.sim`, a panel's `opacity`, `animate offset`, `tileOpacity` and `tileFrost` |
| 14 | upstream | a face's `edge: true`, with `screenTop`, `screenLeft` and `screenRight` in a face; `ripple: false` |
| 15 | upstream | a panel's `keyboard: false`, for a drawer that leaves the soft keyboard down; `pickImages`, `android.speech` and `speech`, `android.power`, `launcher("page:N")` |
| 16 | upstream | `strings { }` and `str`, and `lang()` |
| 17 | upstream | `onPull`, `pin:` on `http`, `post` and `ws`, `ws`, `send` and `on … each` |
| 18 | upstream | `Input`'s `lines`, a widget's or a face's `lock:`, `Browser`, `web(...)` and the page's `onNavigate`, `onTitle` and `onProgress`, `android.location` and `location`, `android.airpods` and `airpods`, `android.calendar` and `calendar` |

The fork has no level of its own since the 0.19.0-z sync. The two it had were sent upstream and
came back as upstream's own: `Browser` was the fork's at 11, then at 18 when the lock needed the
same gating, and the lock was 13, then 14, 16 and 18 as upstream filled the levels under it.
Both landed at owl 18 on 2026-11-04, the lock as a `lock:` on a widget or a face rather than the
fork's `lock(...)` action, so a widget file written for the fork's lock says `lock: true` (or
whatever it held as true) instead of calling it.

A file written for the fork's 13 or 14 keeps working on an old fork build and says `update the
launcher` on this one. That is the cost of the fork not owning a number, and the reason to take
the next free one only for something that cannot be expressed in what upstream has.

Give any new fork feature the next free number, and check upstream's `LEVEL` before choosing
one.

## Fork additions

### Home grid

The inner screen of an unfolded foldable is 12 columns wide, so a full-width tile spans it. This
is upstream's own setting now, not a fork hack: `home.columns` defaults to 12 and its range is
widened to 4..12, and `home.single-page` is on by default, so home shows one page rather than
the two upstream auto-detects on a wide screen. A layout saved by an older fork build keeps its
cells.

### Terminal

`terminal.opacity` (0.3 to 1.0, default 1) lets a terminal show the wallpaper through it, under
the opaque Surface rows: its row reads the `style_opacity` string, shared with the widget
opacity rows' label, and its hint is `style_terminal_opacity_hint`. Since
the 0.12.2 sync the terminal's window is always translucent, as upstream made it for widgets'
glass, so the setting only has to reach foot: at 1 the window is cleared and the tile's glass
shows through at `widgets.opacity`, below 1 foot draws its own background at that alpha through
a `colors-dark.alpha` on the command line as well as in its `foot.ini`. The ⌨ button at a
window's bottom left came through the 0.9.0 sync unchanged.

~~The `keys.toml` extra-keys bar, which carried its own parser, store, file watch and unit test,
with the rows in the guest and the bar redrawn on save.~~ **Retired in the 0.11.0 sync.** Upstream
0.11.0 made the key bar an owl file of its own, `keybar.owl`, with `pressKey`, `typeText` and
`latch` at owl 12 and `keybar.gaps-out`/`keybar.border-width` in Style, which covers what
`keys.toml` did and more. The fork adopted it: `ui/ExtraKeysSpec.kt`, `ui/ExtraKeysStore.kt`,
`ui/ExtraKeysSpecTest.kt`, `LauncherData.keysDir` and the guest bind for `~/.config/omalauncher/keys`
are gone, and the rows `keys.toml` had as its preset are the bar this fork ships as
`assets/widgets/keybar.owl` (36 dp rows, 12 sp labels). The 0.16.0 sync took upstream's photos
key, its `|` key, its icon labels and their spoken names, and kept this fork's rows around them:
`;`, `pgdn` next to `pgup`, readline's `^B`, `^A` and `^P`.

This is the one fork feature upstream has taken over the same ground as, so it is also the one to
drop from any upstream conversation: a PR for `terminal.opacity` or the ⌨ button stays as it was,
there is nothing to send for the bar.

### Sent upstream: the Browser node and the screen lock

Both were the fork's and are upstream's since 0.19.0, as
[#254](https://github.com/SimonSchubert/omalauncher/pull/254) and
[#255](https://github.com/SimonSchubert/omalauncher/pull/255). Nothing of them is kept here: the
files this document used to list as the fork's (`ui/BrowserView.kt`, `ui/LockCue.kt`,
`docs/browser.md`, `ScreenLockTest.kt`) are upstream's now, and the sync took upstream's copies.

Upstream's shape differs from the fork's in one place worth knowing. The lock is a property of the
widget or the face, `lock: mode == "play"`, read as it changes, rather than a `lock(...)` action a
handler calls, so a widget can't forget to let go or believe it still holds a screen the chip
freed. `docs/browser.md` and the widget README have both; `docs/BROWSER-NODE-PLAN.md` is the plan
that became #255 and `docs/UPSTREAM-PLAN.md` is the record of the two pull requests.

### Examples and docs

The fork adds no widget examples since upstream took `46-browser.owl` and `47-lock.owl`; its own
`44-fav-sites`, `45-browser` and `46-browser-tabs` are gone with them. What stays is
`docs/BROWSER-DEV.md`, `docs/UPSTREAM-PLAN.md` and the plan documents upstream now carries too.

## Sending a feature upstream

The two that were worth sending are in: the screen lock and the Browser node, merged on 2026-11-04
as #254 and #255 and released in upstream 0.19.0. `docs/UPSTREAM-PLAN.md` is the record of how
they went, and the shape to copy for the next one.

What upstream still has none of: `terminal.opacity`, BrowserDev and the ⌨ button's bottom-left
place. The 12-column grid ships there already as a setting, so only the default differs.

| feature | owl level | files it touches | how hard to port |
|---|---|---|---|
| `terminal.opacity` | none | `theme/Style.kt`, `linux/TerminalFont.kt`, `ui/TerminalTiles.kt`, `Runtime.kt` | small: upstream's window is already translucent, so it arrives as one command-line option |
| ⌨ button | none | `ui/TerminalTiles.kt`, `LinuxWindowActivity.kt` | one line each, but it is a placement no PR needs |
| BrowserDev | none | `BrowserDevActivity.kt`, the manifest, `linux/OpenBridge.kt`, `linux/DefaultApps.kt` | not for upstream: it answers a developer's need, not a launcher's, and a Play release has no business shipping a DevTools port |

What the two that landed cost, as a measure for the next one:

- The lock moved level four times here (13, 14, 16, 18) before it was upstream's, and it changed
  shape on the way: upstream's is a `lock:` property, not an action, so the fork had to adopt that
  and drop its own.
- The Browser waited for the lock's gating to be upstream's, and then arrived at 18 rather than
  a new 19, because 18 was unreleased when it went in.
- Both came back with upstream's own edits: a Browser can't go in a `for` or a component, a page's
  script stops while the launcher is behind another app, and a widget let go stays let go until its
  `lock:` turns false. The sync took those, not the fork's.

## Building

Android builds run on pop480, not on the phone, which has no SDK:

```
ssh pop480
. ~/android/env.sh
export PATH="$HOME/bin:$PATH"
cd ~/projects/omalauncher
git fetch && git checkout <branch>
./gradlew :owl:test :app:testDebugUnitTest :app:assembleDebug
```

Install on the phone from pop480 with `adb connect <phone>:<wireless port>` and
`adb install -r app/build/outputs/apk/debug/app-debug.apk`. The wireless port rotates, and
`~/adbdiscover.py` on the phone prints the current one. Screenshots need the inner display:
`adb exec-out screencap -p -d 4630946474866474867211650`.

## Known gaps

- The lock and a Browser tile's gating are upstream's now and tested there; what the fork has not
  checked on a device since 0.19.0-z is `terminal.opacity` over upstream's glass.
- A Browser file gets no `owl-check` preview.
- The store treats embedded web content as untrusted, and no warning is shown for it yet.

## Writing in this repository

Prose in docs, the widget README and commit messages goes through the humanizer pass before it
lands: no em dashes as connectors, no "not X but Y" phrasing, no bold labels on every bullet,
no closers that restate the paragraph. Code, commands, paths and upstream's own text stay as
they are.
