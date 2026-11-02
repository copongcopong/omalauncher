# Browser node: the pull request to upstream

Status: not started. Checks and numbers below are against upstream 0.16.0, versionCode 27, owl 17.
The fork is at owl 18 since its lock took that level.

This is the plan for sending the fork's `Browser` node to `SimonSchubert/omalauncher`. The design
itself is in `docs/BROWSER-NODE-PLAN.md`; this file only says what the pull request carries, what
it leaves behind, and what it costs to rebase.

## Why this one goes first

It is the fork addition with the least fork in it. A node, three properties, three handlers, one
action, drawn by Android's own WebView, so no Linux and no new dependency. It touches files
upstream already owns and products upstream does not ship, which is the bar the other two
additions (`terminal.opacity`, the screen lock) clear less easily.

## What goes in

Four commits from `feat/browser-node`, ported rather than merged: `7f5d827` (the node, https
and script off), `6539a59` (touches and keyboard once tapped, two pages at most), `79e3e97` and
`cdbda31` (the page's holder and box, and letting go of it when the tile goes), plus `2101afe`
and `f7224ae` for the examples. The pull request is one branch off `upstream/main`, not a merge
of this fork.

| file | what arrives |
|---|---|
| `ui/BrowserView.kt` | new, about 356 lines: the tile's view, its holder, the WebView's settings and its focus flag |
| `ui/TerminalTiles.kt` | the page joins the tiles' active and typing path |
| `ui/Home.kt` | the tile draw arm |
| `widgets/render/Render.kt` | the `Browser` node in the render arm |
| `widgets/runtime/WidgetInstance.kt` | `browser(...)`, the two live pages and their eviction |
| `widgets/WidgetServices.kt` | `Services.browser(...)`, the engine's side |
| `widgets/WidgetEngine.kt` | the preview that is skipped, with its message |
| `widgets/lang/{Schema,Compiler,Builtins,Program}.kt` | node, properties, handlers, the `web` action, the two-page count, `url`'s fixed scheme and host |
| `widgets/market/Catalog.kt` | `browser GET` in the list of reads that need no Linux |
| `assets/widgets/README.md`, `docs/ARCHITECTURE.md` | the node's page of documentation |
| `assets/widgets/examples/{44-fav-sites,45-browser,46-browser-tabs}.owl` | three examples |
| `docs/BROWSER-NODE-PLAN.md` | the design record, so reviewers see the decisions and the option this rejected |
| `app/src/test/.../WidgetInstanceBrowserTest.kt`, `ui/TileLiftTest.kt`, `owl/.../LangTest.kt` | the tests: two pages, eviction, the level, arity, a `url` built from a value |

## What stays here

The package name and application id, the `-z` suffix and version numbers, `docs/FORK.md`, this
file, BrowserDev (`BrowserDevActivity`, `linux/OpenBridge.kt`, `docs/BROWSER-DEV.md`), the
screen lock, `terminal.opacity`, this fork's key bar rows, and the widget sizes its wider grid
gives today and fetch. BrowserDev in particular is a developer's tool: a release that opens a
DevTools port has no business on Play.

## The level

The fork ships `Browser` at owl 11 and is stuck there, because widgets in the wild already ask
for 11 and moving it would break them. Upstream took 11 for panels in the same release, so a
`Browser` file at 11 is accepted by upstream and then fails on the node. That sharing is the one
thing the pull request has to undo.

A pull request today takes 18: upstream's `Compiler.LEVEL` is 17, so the patch bumps it to 18 and
marks `Browser`, `web`, `onNavigate`, `onTitle` and `onProgress` as 18 in `SINCE`, with the widget
README, `docs/versions.md` and the `LangTest` cases following. If the lock goes upstream as well,
it arrives at 19. Any file written for this fork's Browser has to raise its `meta { owl: N }`,
which belongs in the pull request's notes.

## Where the patch does not drop in any more

Upstream moved under the node. Commits since the node landed, per file: `Home.kt` 23,
`WidgetInstance.kt` 34, `WidgetServices.kt` 24, `Render.kt` 19, `TerminalTiles.kt` 13,
`HomeGrid.kt` 8, the owl tables 27.

- Home is a panel host now, and a tile's focus and lift run through the key bar's hand-back and
  the frost backdrop. The Browser's "inert until tapped" and its `dispatchTouchEvent` flag have
  to be re-applied to that path, not copied over it.
- `Services` gained `pin:`, `webSocket(...)`, `speech`, `pickImages` and the rest. The browser
  arm and the two-page eviction sit beside them; `browser(...)` itself keeps its signature.
- `Render.kt` gained a node or three, so the arm is re-added rather than patched.
- `Catalog`'s no-Linux list still carries `browser GET ` from the fork, which arrives as a
  one-line diff.

## What the pull request says

The security section is the part reviewers should not have to discover: `js: true` is an
explicit opt-in, `https` only at a host written in the file, no JavaScript bridge, no file or
content access, no geolocation, no second window, no mixed content, no third-party cookies, two
pages per file enforced by the compiler and again by `WidgetInstance`, and a store that treats
the page as untrusted content with no preview.

Worth raising as open questions rather than deciding quietly: what a WebView costs on a home
screen that stays up all day, what process death does to a live page, whether the store should
warn before installing a Browser widget, and whether the zoom range of 0.5 to 2 is the right one.

## Steps

1. Branch from `upstream/main`.
2. Cherry-pick the four node commits and the examples, resolving `Home.kt`, `TerminalTiles.kt`,
   `WidgetInstance.kt`, `WidgetServices.kt` and `Render.kt` against what upstream has now.
3. Move the level to a free number (18 now) in the tables, docs and tests.
4. Build and test on pop-480, install, and take the device evidence: a Browser tile on home, a
   face with a terminal and a page in tabs, a tap to focus and type, a drag inside the page that
   does not turn it, back and forward from the file.
5. Open the pull request with the security section and the level note.

## What the device checks still owe

From the fork's own known gaps, all still true: a page evicted by the two-page limit draws blank
until its node asks for it again; a Browser file gets no `owl-check` preview; and the store
treats the page as untrusted without saying so anywhere in the UI.
