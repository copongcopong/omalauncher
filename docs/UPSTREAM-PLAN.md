# Sending the fork's additions upstream: two pull requests

Status: not started. Numbers below are against upstream 0.18.0, versionCode 29, owl 17, which is
where `sync/upstream-018` left the fork's own main. The fork is at owl 18, because its lock took
that level.

The fork has four additions upstream has none of: the `Browser` node, the screen lock, the
terminal's opacity and BrowserDev. This document is the plan for the first two, sent as two pull
requests rather than one, and for the order between them.

## Why two, and why the lock first

A WebView inside an `AndroidView` does not negotiate Compose's nested scroll, so a sideways drag
inside a page reaches the pager and turns home, and a swipe up opens the drawer. A Browser tile
therefore needs home's own gestures gated while a page has the tile. The code that does that is
the same code the lock needs: a set of owners, the pager gate, the swipe and tap gates and a Back
handler.

Sending that gating inside the Browser pull request means asking a maintainer to change how home's
gestures work for a capability no widget file can reach, in the three files upstream rewrites most
often. Sending it inside the lock's own pull request means the same change arrives as the feature
it belongs to, with an example and a chip a person can try.

Asking a separate model about the choice pointed the same way: lock first 0.98, a Browser-only
pull request merged without pushback on the gate 0.21, and the gate questioned or blocking inside
a Browser pull request about 0.92 of the time.

## Pull request 1: the screen lock, owl 18

What a person gets: `lock(true)` holds the home screen still (no page turn, no swipe up, no long
press, and the prompt bar and recent strip open nothing), the launcher draws a border and a chip
whose button always works, and `lock(false)` lets go. The hold is a set of owners, so two widgets
locking are both honoured and one letting go cannot free the screen the other holds. Nothing is
persisted, and editing a page lets go, since a locked screen cannot be rearranged.

| file | what arrives |
|---|---|
| `ui/TerminalTiles.kt` | `HomeLock`, the owner set, and the tile state's `lock`, `locked`, `syncFocusLock`, `releaseFocusLock` and `unlockAll` |
| `ui/LockCue.kt` | new: the border and the chip, and the `lockedTaps` modifier |
| `ui/Home.kt` | the cue, the effect that syncs the hold, and the Back handler that lets go only when nothing else on home wants Back |
| `ui/HomeGrid.kt` | the pager's `userScrollEnabled` gate |
| `ui/Launcher.kt` | the swipe and tap gates on the side rail, the recent strip and the prompt bar |
| `widgets/runtime/WidgetInstance.kt`, `widgets/WidgetEngine.kt` | `onLock`, and saying so where a preview has no home to hold |
| owl's `Builtins.kt`, `Compiler.kt`, `Schema.kt` | the action's arity, its refusal of a value that only reads as true, and `LOCK` at 18 in `SINCE`; `Compiler.LEVEL` becomes 18 |
| `assets/widgets/examples/47-lock.owl`, the widget README, `docs/versions.md` | the example and the node's documentation |
| `ui/ScreenLockTest.kt`, owl's `LangTest.kt` | the tests: owner accounting, the level, arity, a computed value passing |
| `docs/SCREEN-LOCK-PLAN.md` | the design record, so a reviewer sees what the alternatives were |

Two things to get right in this pull request. The chip's two words go in `strings_home.xml` in all
six languages upstream ships (English, Indonesian, Spanish, Portuguese, German, Chinese), since
`HardcodedTextTest` fails the build on a literal. And the owl level is a new number, so
`Compiler.LEVEL`, `SINCE`, the README, `docs/versions.md` and `LangTest` all say 18 together.

If the lock is accepted at 18, the fork and upstream agree on that number again, which costs the
fork nothing: its lock is already at 18.

## Pull request 2: the Browser node, owl 19

What a person gets: a page from the web in a tile of its own, drawn by Android's System WebView, so
it needs no Linux, with `url`, `js` and `zoom`, `onNavigate`, `onTitle` and `onProgress`, and `web`
to steer it. `https` only, at a host written in the file, JavaScript off unless asked for, no
JavaScript bridge, no file or content access, two pages per file at most.

| file | what arrives |
|---|---|
| `ui/BrowserView.kt` | new, about 356 lines: the view, its holder, the WebView's settings and the focus flag |
| `ui/TerminalTiles.kt` | the `browser` field, `typing` counting a page, and a focused page joining `HomeLock` through `syncFocusLock`, about ten lines now that the mechanism is upstream's |
| `ui/Home.kt` | the tile's draw arm |
| `widgets/render/Render.kt` | the node in the render arm |
| `widgets/runtime/WidgetInstance.kt` | `browser(...)`, the two live pages and their eviction |
| `widgets/WidgetServices.kt` | `Services.browser(...)` |
| `widgets/WidgetEngine.kt` | the preview that is skipped, with its message |
| owl's `Schema`, `Compiler`, `Builtins`, `Program` | node, properties, handlers, the `web` action, the two-page count, `url`'s fixed scheme and host, all at 19; `Compiler.LEVEL` becomes 19 |
| `widgets/market/Catalog.kt` | `browser GET` in the reads that need no Linux |
| `assets/widgets/{README.md,docs/*}`, `docs/ARCHITECTURE.md` | the node's documentation |
| `assets/widgets/examples/{44-fav-sites,45-browser,46-browser-tabs}.owl` | three examples |
| `docs/BROWSER-NODE-PLAN.md` | the design record |
| `ui/TileLiftTest.kt`, `widgets/runtime/WidgetInstanceBrowserTest.kt`, owl's `LangTest.kt` | the tests |

The pull request body carries the security section itself rather than leaving it to the reviewer:
the settings above, the two-page cap enforced twice (compiler and `WidgetInstance`), a store that
treats the page as untrusted content, and no preview. It also says that a Browser file written for
this fork declares owl 11 and has to be raised.

## What stays in the fork either way

The package name and application id, the `-z` suffix, `docs/FORK.md`, this file, BrowserDev
(`BrowserDevActivity`, `linux/OpenBridge.kt`, `docs/BROWSER-DEV.md`), `terminal.opacity`, the
fork's key bar rows, and the widget sizes its twelve-column grid gives today and fetch.

BrowserDev stays out on its own grounds: it opens a Chrome DevTools port, which a Play release has
no business shipping. That also keeps `linux/DefaultApps.kt` and `linux/OpenBridge.kt` out of both
pull requests, which is lucky, since Voice put its own guest command in the same two files.

## Where upstream has moved under both

Commits per file since the Browser node landed in the fork (`2ed618c`), counted against upstream
0.18.0: `WidgetInstance.kt` 37, the owl tables 29, `WidgetServices.kt` 26, `Home.kt` 23,
`Render.kt` 20, `TerminalTiles.kt` 13, `HomeGrid.kt` 9, `Catalog.kt` 3.

- Home is a panel host now, and a tile's focus and lift run through the key bar's hand-back and the
  frost backdrop. The lock's gates and the page's inert-until-tapped flag are re-applied to that
  path, not copied over it.
- Home composes two pages either side of the one showing rather than one. That changes what "its
  page shows" means for a Browser tile, so its load, its focus and the two-page cap get re-checked
  against it, and against a store preview that runs with no page around.
- `Services` gained `pin:`, `webSocket(...)`, `speech`, `pickImages` and the rest, so the browser arm
  sits beside more neighbours than it did.
- `Render.kt` gained nodes rather than losing them, so the arm is re-added rather than patched.
- Voice sits in `ui/voice`, `voice/` and `strings_voice.xml`, and it is where a mic tap goes.
  Nothing of it touches either pull request.

## If the lock is turned down

Then the Browser pull request carries the gating itself, as plumbing, at whatever level is free,
and this document becomes the record of why that was the second choice. Two reviews still, just in
the other order.

## Steps

1. Branch from `upstream/main` for the lock: cherry-pick `ebd08ff` (the action and the gates) and
   `09ad5ab` (a page holding the tile, which belongs with the Browser instead), keeping the second
   for later. Resolve `Home.kt`, `HomeGrid.kt`, `Launcher.kt` and `TerminalTiles.kt` against what
   upstream has now, and set the level to 18.
2. Build and test on pop-480, install, and take the device evidence: a lock widget holding the page
   turn, the swipe up and the long press, the chip unlocking, Back unlocking, and editing a page
   while locked.
3. Open the lock's pull request with `docs/SCREEN-LOCK-PLAN.md` and the level note.
4. Once it is merged, branch again for the Browser at 19: cherry-pick `7f5d827`, `6539a59`,
   `79e3e97`, `cdbda31`, and the examples' commits `2101afe` and `f7224ae`, resolving against the
   `Home.kt`, `TerminalTiles.kt`, `WidgetInstance.kt`, `WidgetServices.kt` and `Render.kt` upstream
   has now.
5. Build, install, and take the Browser evidence: a tile on home, a face with a terminal and a page
   in tabs, a tap to focus and type, a drag inside the page that does not turn it, and back and
   forward from the file.
6. Open the Browser's pull request with the security section, the level note, and a line saying the
   gating it relies on is upstream's own since the first one.

## What the device checks still owe

From the fork's known gaps, all still true: the lock's editing and arrange gate has not been
checked on a device; a page evicted by the two-page limit draws blank until its node asks for it
again; a Browser file gets no `owl-check` preview; and the store treats the page as untrusted
without saying so anywhere in the UI. A widget's own `lock(true)` ignoring prompt-bar taps is
covered by code and not by a device run.
