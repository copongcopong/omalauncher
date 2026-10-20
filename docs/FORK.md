# The fork

Omalauncher Z is [copongcopong/omalauncher](https://github.com/copongcopong/omalauncher), a
fork of [SimonSchubert/omalauncher](https://github.com/SimonSchubert/omalauncher). Upstream
stays the source of everything the launcher does. This fork carries a small set of additions,
and this file lists them so a sync or a release does not have to rediscover them.

It has a second use. When one of these additions is worth sending upstream, this file is the
starting point: each entry says which level it uses, which files it touches, and what an
upstream PR would still need.

Keep this file current. When a fork change lands, add or edit its entry here in the same
change. When a sync lands, re-read it against the new upstream and fix whatever moved.

## Identity

| | |
|---|---|
| package | `dev.omalauncher.fold` |
| version name | `<upstream>-z`, e.g. `0.10.0-z` |
| version code | one above upstream's, so it installs over an upstream build, e.g. `19` |
| signing | release falls back to the debug key, which is what the phone's install has |
| releases | tag `v<versionName>`, built by `.github/workflows/fork-release.yml` |

## Keeping up with upstream

Fetch, branch as `sync/upstream-<version>`, merge, resolve, build, then open a PR.

Conflicts so far land in the same places every time: `CHANGELOG.md`, `app/build.gradle.kts`,
`app/src/main/assets/widgets/README.md`, and the owl language tables (`Schema.kt`,
`Builtins.kt`, `Compiler.kt`), because both sides add to them. `Home.kt` and `Launcher.kt`
conflict when upstream rewrites home, which it does regularly. The fork's own files
(`TerminalTiles.kt`, `HomeGrid.kt`, `BrowserView.kt`, `LockCue.kt`) have never been touched
upstream and merge cleanly.

Check the language level first when a sync starts. Upstream and the fork have collided twice:
see [Owl levels](#owl-levels).

After the merge: bump `versionName` to `<upstream>-z` and `versionCode` to upstream's plus one,
add a `CHANGELOG.md` section at the top, and update this file.

## Owl levels

The level in `meta { owl: N }` is a promise to older launchers: a file that asks for level N
either works or is rejected cleanly. Numbers are global, so two features cannot share one.

| level | whose | what |
|---|---|---|
| 11 | shared | upstream's panels, `android.drawer`, `appAction`, `workApps`, `shortcuts`, lazy Columns and Grids, `Image.label`, `Text.minLines`, `chunk`, `rank`, `strongMatch`, app `statusBar`; the fork's `Browser` node and `web(...)` |
| 12 | upstream | `screenTop`, how much of an app's top a camera cutout covers |
| 13 | fork | `lock(true)` and `lock(false)` |

Level 11 is deliberately shared. `Browser` shipped at 11 before upstream took 11 for panels, and
moving it would break widgets already written for it, so the fork keeps it there. This
launcher supports both features at 11. An upstream launcher reading a Browser file accepts the
level and then says `Browser` is not a node, which is a worse message than a version error but
not a silent failure.

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

`terminal.opacity` (0.3 to 1.0, default 1) lets a terminal show the wallpaper through it. The
⌨ button at a window's bottom left, and the `keys.toml` extra-keys bar, came through the
0.9.0 sync unchanged.

### Browser node

A web page in a tile, drawn by Android's System WebView, so it needs no Linux. Owl 11.

```owl
Browser { url: "https://lobste.rs/"; js: true; zoom: 0.9; weight: 1
  onNavigate: url = value; onTitle: title = value; onProgress: p = value }
```

`url` is https only, at a host written in the file, with values through `enc()`. `js` is off
unless set, and DOM storage follows it. There is no JavaScript bridge, no file or content
access, no geolocation, no second window, no mixed content and no third-party cookies. `web`
takes `back`, `forward`, `reload` or `stop` and steers every page in the file. A widget holds
two pages at most; the compiler counts the nodes and `WidgetInstance` evicts the page asked for
longest ago.

The store classifies a Browser widget as not needing Linux, and previews are skipped with a
message rather than a blank picture.

Files: `ui/BrowserView.kt`, the `Render.kt` arm, `WidgetInstance.browser`, the store's
`Catalog`, and owl's `Schema`, `Compiler`, `Builtins`.

Examples: `assets/widgets/examples/45-browser.owl`, `45-fav-sites.owl` and `46-browser-tabs.owl`.

Plan and decisions: `docs/BROWSER-NODE-PLAN.md`.

### Screen lock

A widget holds the home screen still while it is being used: no page turn, no swipe up, no long
press, and the prompt bar and recent strip open nothing. Owl 13.

```owl
Button { text: editing ? "done" : "draw"; onTap: { editing = !editing; lock(editing) } }
```

A focused Browser holds it by itself, since a page's drags are its own. The lock is a set of
owners, so two widgets locking are both honoured. It ends with `lock(false)`, when the widget
stops, through the chip, or with Back when nothing else on the tile wants it, and it is never
persisted. The launcher draws a border and a top-centre chip whose button always works.

A page's hold follows the view's focus, so a tap outside the page, including on the prompt bar,
lets go of both.

Files: `ui/TerminalTiles.kt` (the holds), `ui/Home.kt` and `ui/HomeGrid.kt` (the gates),
`ui/Launcher.kt` (`lockedTaps`), `ui/LockCue.kt` (the cue), `widgets/runtime/WidgetInstance.kt`
(`onLock`), and owl's `Schema` and `Builtins`.

Example: `assets/widgets/examples/47-lock.owl`.

Plan and decisions: `docs/SCREEN-LOCK-PLAN.md`.

### Examples and docs

The fork adds `45-browser.owl`, `45-fav-sites.owl`, `46-browser-tabs.owl` and `47-lock.owl` to
`assets/widgets/examples`, and two plan documents under `docs/`. `docs/ARCHITECTURE.md` has a
section each for Browser tiles and the screen lock.

## Sending a feature upstream

Upstream has none of these: `terminal.opacity`, the keys bar, `Browser` and `lock` all read as
absent from `upstream/main`, and the 12-column grid ships there already as a setting, so only
the default differs.

| feature | owl level | files it touches | how hard to port |
|---|---|---|---|
| `terminal.opacity` | none | `theme/Style.kt`, the terminal's draw path | small |
| keys bar and ⌨ button | none | `ui/ExtraKeys*`, `keys.toml`, terminal windows | medium, and its own docs |
| Browser node | 11 in the fork, taken upstream | `ui/BrowserView.kt`, owl's `Schema`, `Compiler`, `Builtins`, `Program`, `Render.kt`, `WidgetInstance`, `WidgetServices`, `WidgetEngine`, `Catalog` | medium, self-contained |
| screen lock | 13 in the fork | `TerminalTiles`, `Home`, `HomeGrid`, `Launcher`, `LockCue`, `WidgetInstance`, owl's `Schema`, `Builtins` | medium, and `Home`/`Launcher` are the files upstream rewrites most |

A picked feature goes out as one branch from `upstream/main`, not as a merge of this fork.
Cherry-pick or copy the fork commits, then:

- Take a fresh owl level. Upstream has 11 (panels) and 12 (`screenTop`), so the Browser would
  arrive at 13 or later and the lock after it, and `Compiler.LEVEL`, `SINCE`, the widget README
  and `LangTest` move with it.
- Leave out what belongs to the fork: the package name, the version suffix, this file and the
  fork plans.
- Re-check the parts upstream has since rewritten. The lock's prompt-bar and strip gate lives
  in `Launcher.kt`, which upstream turned into a panel host, and home's placement now comes
  from `layout.toml`.
- A Browser PR needs the store to treat a page as untrusted content, so say so in the pull
  request rather than leaving it to the reviewer to notice. A preview is skipped, and
  `owl-check` should keep saying why.
- Bring the tests: `LangTest` cases for the level, the arity and the constant-argument check,
  `ScreenLockTest` for the owner accounting, and the device evidence from the plan documents.

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

- The editing and arrange gate for the screen lock has not been checked on a device. A page
  cannot be moved while locked, and the code releases the hold when `state.editing` or an open
  folder is set, but the check itself is untested.
- A widget's own `lock(true)` ignoring prompt-bar taps is covered by code and not by the device
  run, because the test tile would not stay on page 1.
- A page evicted by the two-page limit draws blank until its node asks for it again.
- A Browser file gets no `owl-check` preview.
- The store treats embedded web content as untrusted, and no warning is shown for it yet.

## Writing in this repository

Prose in docs, the widget README and commit messages goes through the humanizer pass before it
lands: no em dashes as connectors, no "not X but Y" phrasing, no bold labels on every bullet,
no closers that restate the paragraph. Code, commands, paths and upstream's own text stay as
they are.
