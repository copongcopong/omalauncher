# Changelog

The most important changes in each release. The Play Store notes for a release
are in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
Dates are when the build was uploaded to Play.

## 0.16.0-z (versionCode 28) – 2026-10-30

Omalauncher Z on upstream 0.16.0 (versionCode 27) and its main: settings has a language, and
the launcher's text comes in Indonesian, Spanish and Brazilian Portuguese, carried in string
resources, with widgets' `strings { }` at owl 16 and `lang()` for the language picked. Owl 17
adds a WebSocket client (`ws()`, `send()`, `on x each { }`), a `pin:` SHA-256 that trusts a LAN
server's own certificate, and `onPull` for pulling a scrolling list down to refresh it. A Linux
job's log keeps up with progress bars, holds 200 lines and opens whole when tapped, pacman
installs bubblewrap again, the recent apps and the prompt bar frost over the wallpaper with the
tiles, and home's long-press menu wraps its buttons instead of cutting the last one off.

This release folds in 0.14.0, 0.15.0 and 0.16.0, which upstream published between the fork's
0.13.0-z and now.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- The screen lock moved from owl 16 to owl 18, because upstream took 16 for `strings { }` and
  `lang()`, and 17 for `ws()`, `pin:` and `onPull`. A file that locks the screen says
  `meta { owl: 18 }` now; nothing else about it changed. This is the third move: 13 went first,
  then 14, then 16.
- The fork's own text is in string resources like the rest of the launcher: the settings row
  that opens BrowserDev, and the terminal's opacity hint, in English with Indonesian, Spanish
  and Portuguese alongside. Upstream's new test for text written straight into the UI catches
  what isn't, so nothing here fails it.
- The key bar keeps this fork's rows, the ones its `keys.toml` carried: `;`, `pgdn` that
  repeats while held, and readline's `^B`, `^A`, `^P`. From upstream it gains the photos key,
  the `|` key, icon labels and their spoken names in the phone's language.
- The `Browser` node at owl 11, the 12-column grid, the terminal's ⌨ button,
  `terminal.opacity` and BrowserDev come through the sync unchanged. The today and fetch tiles
  keep the 6x3 size the wider grid gives them, and the owl widget keeps its shorter text for a
  tile narrower than half the screen.

## 0.16.0 (versionCode 27) – 2026-10-29

- Settings has a language: the phone's, or one the launcher has its text in. A pick brings
  every open screen back in it, and widgets' `strings { }` and `lang()` follow it too.
- The launcher comes in Spanish and Brazilian Portuguese (also for phones set to Portugal's),
  with the drawer, today, fetch, owl and key bar widgets translated. Play installs carry every
  language, not just the phone's. Indonesian uses the words Android itself does.
- A Linux job's log keeps up with progress bars instead of piling them up, holds 200 lines,
  and opens whole when tapped, red only where something went wrong, with [ copy ] for a bug
  report (#240).
- pacman installs bubblewrap again, so glycin and the GTK apps needing it upgrade (#243).
- With frosted glass, the recent apps and the prompt bar frost too, on home, in the landscape
  rail and over the open drawer.
- Owl 17: `ws(url)` listens to a WebSocket server, `send()` talks back and `on x each { }`
  runs for every message; `pin:` on `http`, `post` and `ws` trusts a LAN server's own
  certificate by its SHA-256; a scrolling Column or Grid takes `onPull` for pull to refresh.
- Fixes: picking a language no longer opens a terminal; home's long-press menu wraps its
  buttons instead of cutting the last one off; rendering a theme no longer empties a folder
  Omarchy's `current/theme` links to.

## 0.15.0 (versionCode 26) – 2026-10-28

- The launcher speaks Indonesian: with the phone set to Indonesian, all of its text, the
  drawer, the today, fetch and owl widgets and the terminal key bar's spoken key names are
  in Indonesian.
- Widgets can carry their text in more than one language: owl 16's
  `strings { en { ... } id { ... } }`, read as `str.name`, show the phone's language's with
  English where it has none, and texts can take values. `lang()` is the phone's language for
  APIs. Widgets recompile when the phone's language changes, and agents publishing a
  one-language widget are asked whether to add English.
- Coding agents take the theme as on Omarchy: Claude Code has an Omarchy theme to pick in
  /theme, Pi starts in it, and OpenCode follows the terminal's colors; running sessions
  recolor when the theme changes.
- A widget, face or app added from the store can be removed from its page in the store, and
  each store always has its installed tab.
- The empty page right of home's last invites a widget, with create with the agent and browse
  store, instead of a one-line hint.
- The store's built-in widgets, a terminal and a tile per coding agent, are one row that
  scrolls sideways.
- At a large Android font size, buttons that would squeeze a label to a letter a line go under
  it, in settings, the style screen and Files' selection bar, and settings' updates scroll with
  its rows.

## 0.14.0 (versionCode 25) – 2026-10-27

- Tapping a now-playing widget brings up the player's own screen, like a web app's
  window when one installed from a browser plays, rather than a new browser tab.
- A drawer can leave the keyboard down as it opens, for scrolling the apps rather
  than searching them: owl 15's `keyboard: false` in `drawer.owl`. Tapping the
  bar brings it up.
- Images go into a terminal as the path they're saved to in Linux's /tmp, which
  opencode and Claude Code attach: tap a screenshot or copied image in Gboard's
  clipboard suggestions, or paste an image copied on Android (Chrome's Copy image).
  Copied text pastes as before. The terminal key bar's new photos key, beside paste, opens Android's
  photo picker and pastes the photos picked the same way, several at once; above it, a `|` key. Owl 15's
  `pickImages()` does it in a key bar of your own.
- The today widget's weather screen switches between °C and °F when you tap the
  temperature's °C or °F, and keeps the scale picked; until then it follows the country.
- A widget can turn home to another page: `launcher("page:0")` shows the face left of
  page 1, as Super+1…9 does for pages 1 to 9, so a tile can lead to the face that shows more of it.
- Widgets can take dictation: owl 15's `speech("toggle")` turns the microphone on and off from a
  tap, and `source said = android.speech` has what's heard, as you speak and once you're done, so a
  widget can show it, run a command with it, or type it into a terminal with `on said.final`. The
  phone recognizes the speech, as for the prompt bar, and only the text reaches the widget (#84).
- Widgets can show how fast the phone charges: owl 15's `source p = android.power every 2s` has the
  battery's current in mA, its watts, the mAh it holds, Android's time to full and what the charger
  offers, the same units and sign on every phone (#226).
- Home's empty pages at either end can be turned off in style.toml or the Style screen:
  `home.empty-page-left = false` drops the one left of the leftmost page, and
  `home.empty-page-right = false` the one after the last page of tiles (#202, #205).
- GTK3 and wxWidgets apps (PrusaSlicer) leave out their own title bar and window buttons,
  which only took space on a phone; apps with their own header bar keep it (#215).
- A Linux window too big for the screen is drawn shrunk to fit, keeping its shape, instead of
  running off the right and bottom edges, and touches map back to it (#218).
- Fixes: a widget node with `clip: true` and a negative radius crashed home; a widget home
  placed on another page, or one moved there, stopped updating two pages away until the
  launcher restarted; a GTK app with a header bar kept asking how to decorate its window.

## 0.13.0-z (versionCode 25) – 2026-10-25

Omalauncher Z on upstream 0.13.0 (versionCode 24) and its main: faces can draw to the screen's
edge, store faces can come with several pages installed side by side, owl apps and panels ripple
where they're pressed, widgets' glass is a setting (`widgets.opacity`, `widgets.frost`), the
launcher works with a keyboard or gamepad, and a drawer can leave the keyboard down (`keyboard:
false`, owl 15). Owl 13 filled in the phone's own controls (`settings`, `dnd`, `torch(level)`,
`android.sim`, `android.network`, notification actions, replies and snoozes). Upstream also fixed
a crash measuring a tile on a page the pager had set aside, stopped a store update from
overwriting an edited widget, taught GTK apps to take their own title bar, shrank a Linux window
too big for the screen, and made animated canvases draw about twice as fast.

This release folds in 0.12.0 to 0.13.0: the fork published none of the 0.12.x releases, so their
changes arrive here.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- The screen lock moved from owl 14 to owl 16, because upstream took 13, 14 and 15 for its own
  features. A file that locks the screen says `meta { owl: 16 }` now; nothing else about it
  changed. This is the second move: 13 went first, then 14.
- A development browser, apart from home: `browser-dev [url|port]` in Arch opens a window that
  takes `http://` on this phone's loopback as well as https, in its own process with its own
  WebView data directory, so an agent can attach to the page with the Chrome DevTools Protocol
  without home's `Browser` tiles gaining a debug socket or an http hole. Settings has a row for
  it, and it comes back to the page it was last on. `docs/BROWSER-DEV.md`.
- The `Browser` node at owl 11, the 12-column grid, the terminal's ⌨ button and
  `terminal.opacity` come through the sync unchanged.

## 0.13.0 (versionCode 24) – 2026-10-25

- Faces can draw to the screen's edge: owl 14's `edge: true` runs a face under the
  status bar, and in landscape under a camera cutout or navigation bar at the side;
  `screenTop`, `screenLeft` and `screenRight` say how much those cover. Other faces
  now keep clear of a navigation bar at the side, which they ran under.
- A store face can have several pages, installed side by side. Each page stays
  yours to remove; an update replaces only the pages still on home, and the face's
  store page adds a removed one back. Publishing a face lets your agent add your
  other faces as its pages.
- Owl 14's `ripple: false` leaves out the ripple on a node that shows its press
  itself.
- Fixes: a face's bottom jumped after every reload or data update; the store's
  widgets/faces/apps toggle stayed round with square corners.

## 0.12.2 (versionCode 23) – 2026-10-25

- A store update no longer throws away changes made to a widget, app or face:
  settings' update leaves an edited one out and lists it as edited, and its store
  page says updating replaces the changes, as "update anyway" (a Play review).
- Home no longer crashes measuring or updating a tile on a page the pager had set
  aside ("measure/Apply is called on deactivated node", Play, 0.12.1).
- Animated Canvases draw about twice as fast.
- AGENTS.md and CLAUDE.md in the Linux home folder link the launcher's source.

## 0.12.1 (versionCode 22) – 2026-10-24

- Owl apps, panels, faces and screens ripple where they're pressed; a widget's tile
  on home and the key bar don't.
- Home's bar on a pinned owl app offers edit and share, as the drawer does (#197),
  and an app in a folder gets home's bar on a long press.
- A tap on a terminal's prompt line brings the soft keyboard back (#100). Pressing
  Home on home with nothing open goes back to page 1 (#201).
- Faster and lighter: home has its widgets in about half the time after start and
  shows complete rather than filling in; pages either side are made ahead; animated
  widgets, terminal tiles and Linux app windows draw at most 60 frames a second, and
  a terminal tile stops drawing while home is out of sight; the drawer's app list,
  owl apps' icons and the media source work off the main thread.
- Linux setup stops with an error naming the kernel fix on custom ROM kernels where
  it hung at "Initializing pacman keyring" (#105), and setup failing at once (too
  little space) no longer crashes the launcher.
- Fixes: Shift on a hardware keyboard with FlorisBoard in terminals and Linux windows;
  the home terminal tile stuck on its red file name after a relaunch; the first home
  after onboarding opened on page 2 on short screens; the today tile labelled cached
  forecast days wrong after midnight; android.battery said "on battery" when plugged
  in but not charging; system updates printed "Directory not empty" removing an old
  agent version (#196).

## 0.12.0 (versionCode 21) – 2026-10-23

- controls.owl, a control center for swiping down on home: the phone's switches,
  brightness, volume and where it plays, Do Not Disturb and the torch with their
  levels, what's playing, the next alarm and the notifications (reply, act, snooze,
  swipe away). Pick it in settings › gestures. Owl 13 adds what it uses:
  `settings(...)`, `dnd(...)` with `dndAllows`, `torch(level)`, the radios and more
  in `android.modes`, `android.sim`, `android.network`, `android.volume`'s output,
  notification actions, replies and snoozing, `drag` and `onDragEnd`.
- Widgets' glass is a setting: an opacity slider and frosted glass in Style
  (widgets.opacity, widgets.frost), terminal widgets included; the drawer and panels
  set how much they cover home. Owl 13's `tileOpacity` and `tileFrost` let a face or
  a widget's own cards match.
- Keyboards and gamepads reach the whole launcher: arrows or the D-pad move between
  tiles, recent apps, the bar, the drawer, panels, settings and what takes taps in a
  widget, with an accent ring; Enter opens, holding it or Menu long-presses, Esc or B
  goes back. Super+D opens the drawer, Super+T a terminal, Super+Down the swipe-down
  panel.
- Codex runs commands again (its sandbox failed on every command under proot).
  Agent updates delete the old versions and npm's cache, which left every upgraded
  agent installed twice.
- The store takes widgets, faces and apps up to 1 MB; an installed app opens from
  its store page; store cards decode their previews at the size they're drawn.
- Fixes: the mic crashed with some voice assistants (ChatGPT's) as the phone's
  speech recognizer; store previews were blank on Android 10 and 11; very large
  widget images ran out of memory; a widget too big for a share link crashed the
  share screen (it's shared by QR code now); a frost that comes out NaN crashed the
  launcher; the first key on setup could change the theme.

## 0.11.0-z (versionCode 21) – 2026-10-22

Omalauncher Z on upstream 0.11.0 (versionCode 20) and its main: the terminal key bar is an owl
file now, `keybar.owl`, and owl 12 adds `secret` state kept in the Android keystore, Input
`password`, post's `method:`, `toBase64`/`fromBase64`, `screenTop`, `pressKey`, `typeText`, `latch`,
`onPress`/`onRelease`, `repeats` and `android.modifiers`. Also the 0.10.1 fixes: the store's kind
toggle no longer crashes, going home during a backup doesn't, and backups leave out Linux so they
fit Android's quota.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- **The `keys.toml` extra-keys bar is retired**, superseded by upstream's own `keybar.owl`, which
does the same job as a `keybar "…" { }` file in the guest with more to it: any rows, `pressKey`,
`typeText`, latched modifiers, keys that run commands, and its gaps and border in Style. The fork's
rows are what it ships as its bar, 36 dp a row with 12 sp labels, so a bar nobody edited looks as
it did; `keys.toml` is no longer read, and [docs/EXTRA-KEYS.md](docs/EXTRA-KEYS.md) maps each line
of one onto the file that replaced it.
- The `Browser` node at owl 11 and the screen lock at owl 13 come through the sync unchanged, and the
widget docs now carry them as `docs/browser.md` and `docs/lock.md`.

## 0.10.0-z (versionCode 19) – 2026-10-22

Omalauncher Z on upstream 0.10.0 (versionCode 18) and its main: the app drawer is an owl
panel now, home's layout is `layout.toml`, and owl 11 gained panels, lazy columns, app
`statusBar`/`title`, `chunk`, `minLines` and the drawer's search ranking.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- A `Browser` node at owl 11: a web page in a tile, drawn by Android's own WebView, https
  only, script off unless `js: true`, two per widget. Upstream's owl 11 is panels, so 11 is
  shared: this launcher supports both, and an upstream launcher reading such a file says
  `Browser` isn't a node rather than a version error.
- `lock(true)` at owl 13: a widget holds home still (no page turn, swipe up, long press or
  prompt-bar tap) and the launcher draws a border and a chip whose button lets go. A page
  that has the tile holds it by itself, until a tap outside, Back or the chip. 13 because
  upstream took 11 and 12 while the fork was on 0.9.0.
- The home grid is 12 columns across a foldable's inner screen, from the fork's `home.columns`
  default, widened to 4..12.
- The terminal's ⌨ button, `terminal.opacity` and the `keys.toml` extra-keys bar.
- A Browser example beside `45-browser.owl`: `45-fav-sites.owl` keeps Hacker News,
  old.reddit.com, GitHub trending and this repo behind one button's menu, with back, forward
  and reload above the page.

## 0.11.0 (versionCode 20) – 2026-10-21

- The terminal key bar is an owl file: keybar.owl, which you or an agent can edit
  like drawer.owl, with .stock-keybar.owl as the fallback. Settings › style shows
  whether it's stock, edited, has errors or removed, and sets its gaps and border
  width. Owl 12 adds `keybar "…" { }` files, `pressKey`, `typeText`, `latch`,
  `android.modifiers`, `onPress`/`onRelease` and `repeats`.
- Owl 12 also: `secret` state, kept encrypted with Android's keystore and sent only
  to the server written out in its request's URL; Input `password`; post's
  `method:` PUT, PATCH and DELETE; `toBase64`/`fromBase64`; `screenTop` for apps
  without a status bar. `requires { pacman: [] }` no longer makes a widget need Linux.
- Widget requests follow redirects themselves and send headers and bodies only to
  the server they name; a response over 1 MB fails rather than being cut; a failed
  post says why in err. A runaway regex or a huge value can't freeze or take down
  the launcher, and a stream's command stops when its widget lets go of it.
- Installing Linux checks for free space before downloading (it needs about 3 GB)
  and can be cancelled. Without Linux, a widget's edit and other agent buttons offer
  to install it instead of failing.
- Fixes: the status bar's icon colour after a fold or rotation; settings' subtitles
  in the White theme; a store widget that doesn't fit shows the page it went to; the
  store's previews use far less memory; Files handles links into /proc or out of
  Linux and opens .owl files in its editor; symbols the launcher's font lacks; the
  edit bar of a widget in home's top row.

## 0.10.1 (versionCode 19) – 2026-10-20

- The store's kind toggle showed a "panels" tab with no store behind it, and tapping
  it crashed the launcher; it lists widgets, faces and apps again.
- Going home while a backup or device transfer ran crashed (the process runs with a
  plain Application then); home now closes until the process restarts.
- Backups and device transfers leave out Linux (the rootfs and its setup records)
  and Omarchy's downloaded release: a restored phone shows Linux as not installed
  instead of a broken rootfs, and the backup fits Android's 25 MB quota. Before,
  a phone with Linux got no backup at all ("Size quota exceeded").


## 0.10.0 (versionCode 18) – 2026-10-20

- The app drawer is an owl file: drawer.owl, which you or an agent can edit, with
  .stock-drawer.owl as the fallback when it's removed or doesn't compile. Owl 11
  adds `panel "…" { }` files that a swipe on home opens (up: the drawer, down: from
  the top), picked in settings › gestures or style.toml, with the prompt bar's
  `query`, `enter`, `onSubmit` and `onKey`; `android.drawer`, `appAction`,
  `workApps`, `shortcuts()`, `rank` and `strongMatch` for the drawer's apps, search
  and menus. The Kotlin drawer is gone.
- Home's layout is a file: ~/.config/omalauncher/home/layout.toml lists home's
  pages in order and where every widget, app and folder sits. Moving things on the
  phone rewrites it, and an agent (or you) can edit it to move, resize, pin or
  reorder; problems show on home and in errors.txt next to it. A widget's `page:`,
  `order:` and `size:` only place it the first time.
- Owl 11 also: Column and Grid `lazy: true` and `reverse`, Image `label` for screen
  readers, Text `minLines`, `chunk(list, n)`, apps without a status bar or title
  row, and a Column in a Row or Flow is as wide as its content.
- owl review warns about widgets that poll more often than they need to.
- Faster: a Shader animates only when it reads time; Canvas text layouts are cached.

## 0.9.0-z (versionCode 18) – 2026-10-19

Omalauncher Z on upstream 0.9.0 (versionCode 17) and the rest of its main: 51 commits since
0.8.0 — faces, owl 9 through 11, the store's previews and faces tab, and upstream's fixes,
among them Linux setup running out of memory at "Installing cliamp", `cmd("/")`, a damaged
font file and `round()` of very large numbers.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- The home grid is 12 columns across a foldable's inner screen, so a widget tile is full width,
  and it is upstream's own setting now: `home.columns` (Style › home) at 12 with
  `home.single-page` on, which the fork ships as its defaults and widens from upstream's 4..9 to
  4..12. The fork's hard-coded grid is gone, so the grid is configurable at last. A layout saved
  by an earlier fork build says 12 columns and keeps its cells where they are; an older one is
  moved onto 12 the way upstream moves a layout onto any change.
- The terminal's ⌨ button at a window's bottom left, `terminal.opacity` and the `keys.toml`
  extra-keys bar come through the sync unchanged.

## 0.9.0 (versionCode 17) – 2026-10-18

- Faces: an owl file can be a whole home screen (`face "name" { }`, owl 10), left of
  page 1 like a stock launcher's feed, as many as you like, with an empty one
  inviting the next. The store has a faces tab (minimal, pixel, terminal); settings ›
  pages lists faces and tile pages to drag into order, edit or remove. A face can
  hide the status bar, runs under the navigation bar with `screenInset`, and takes
  both halves of an unfolded foldable.
- Owl 10: text `spans`, `fontWeight`, `letterSpacing`, linear and radial gradient
  backgrounds, `frost` (frosted glass over the wallpaper), fonts of a widget's own
  (asset, URL or Linux file), styled Canvas text, `Popup`, Input `underline: false`,
  `onLongPress`, `android.agents`, `android.recent`, `open(key)`, `ask(text)` and
  Image `icon:<key>`. A Text in a Row or Flow is as wide as its text. `android.apps`
  updates as apps change instead of being polled.
- Example widgets: the start page is redesigned; the ai bar rebuilds the prompt bar
  and recent strip in owl.
- Fixes: sharing a face or an app to the store; dragging pages in settings; a NaN
  font size, a damaged font file or `cmd("/")` no longer crash the launcher;
  `round()` of very large numbers.

## 0.8.1 (versionCode 16) – 2026-10-17

- Home's grid has 4 to 9 columns (6 by default); changing it moves every tile onto
  the new grid. A pages row keeps one page where a foldable would show two (#141).
- Style settings preview home and widgets as they change, and the terminal preview
  is drawn in place instead of starting a foot. The terminal font is a dropdown.
- A font for the launcher's text and widgets: Android's monospace, or one of Linux's
  monospace fonts.
- Local network: on Android 17+ a settings row asks for ACCESS_LOCAL_NETWORK, so
  widgets and Linux can reach devices like a printer or Home Assistant.
- Store updates for widgets and apps show in settings' system updates box, with
  pending packages and agents listed one per line.
- The home menu has the settings gear at its top right, like the drawer.
- Owl 9: `parseTime`, `fmtAgo`, `fmtNum`, `fmtTime` with a time zone and language,
  `random`, `shuffle` and `hash`; `set`, `remove`, `merge`, `reduce`, `scan`,
  `groupBy`, `toJson`, `capitalize`, `asin`, `acos`, `flatten` to a depth and
  `on x first`; handlers have `if`/`else` and `let`. Numbers print as JavaScript
  writes them (1792231445.5, not 1.7922314455E9).
- Fixes: a widget's text field lifts its tile above the keyboard like a terminal;
  home no longer stays sideways in portrait after a landscape app (before Android 14);
  a widget filling its page shows its edit bar inside it; Linux setup no longer
  hangs at "Installing cliamp" on phones with a small app heap.

## 0.8.0 (versionCode 15) – 2026-10-17

- Apps: an owl file can be a full-screen app (`app "name" { }`, owl 8) that opens
  from the app drawer, with its own Recents card. Back goes back a level inside it.
  The store lists apps like Braincup's brain games, Yoga and Linux commands; an app's
  page swipes through its screens, drawn in your theme.
- Widgets and apps can show images from the web (SVG drawn as vectors), play sounds,
  send HTTP POSTs and read the installed apps (`android.apps`). Values in URLs go
  through `enc()`, and the store's reviewer sees each URL a widget can reach.
- GPU acceleration for Linux apps on Adreno 6xx and newer (Turnip). The patched Mesa
  now ships in the app for Adreno and Mali, so OpenGL apps run on the GPU through
  zink, and windows with MSAA (PrusaSlicer's G-code viewer) no longer crash.
- Folders can have a name, set in place in their header.
- Home can hide the status bar.
- App search matches names with their accents stripped.
- The store marks widgets added in the last three days with a "new" badge and tab,
  and has more categories (sport, finance, media, games, pets). Its pages and
  settings pages opened from another have a back chevron.
- Themes installed from the catalog show its screenshot in the theme list.
- OpenCode 2 installs and updates again: agent installs and system updates update
  mise first and add node for OpenCode.
- Prompts start Antigravity and GitHub Copilot in interactive mode, and Antigravity
  in plan mode for untrusted prompts.
- Fixes: submitting to the store no longer crashes without a browser, links from
  Linux keep opening after an error, and turning the GPU on waits for package installs.

## 0.7.3 (versionCode 14) – 2026-10-16

Omalauncher Z, this fork's first release with the terminal work in it, on upstream 0.7.2 plus its
next 22 commits. Signed with the debug key, so it installs over an earlier fork build.

Fork changes ([copongcopong/omalauncher](https://github.com/copongcopong/omalauncher)):

- The terminal's ⌨ button moved from the bottom right to the bottom left, in the home
  screen's terminal tiles and in a Linux window.
- A terminal's background can be see-through: `terminal.opacity` in `style.toml` (0.3 to 1), or
  Style › terminal › opacity, becomes foot's own background alpha, so the text stays solid over
  the wallpaper. It applies to terminals started from then on, and a standalone Linux window
  keeps its own background.
- The extra-keys bar is configurable. `~/.config/omalauncher/keys/keys.toml` holds its rows
  and keys — a key by name or evdev code, `*` to repeat while held, `ctrl`/`alt`/`shift`
  alone to latch a modifier, `label=ctrl+x` to hold one around a key — and the bar follows
  the file as it's saved. Its shape is settings too: `height` in dp for a row (20..80) and
  `text` in sp for the labels (8..24), both optional. The preset is the fork's own rows at
  `height = 36`, `text = 12`, and its header says how to update the file and links
  [docs/EXTRA-KEYS.md](docs/EXTRA-KEYS.md), the format's reference.
- The home screen's grid is 12 columns across the whole inner screen of a foldable, instead
  of the two 6-column pages side by side, so a widget tile can be full width.

From upstream, merged into this release:

- Widgets can draw images and play sounds from the web, and SVGs as vectors (owl 8).
- The widget store lists apps, with a preview of each one's own window in every theme.
- Widgets can read your installed Android apps as `android.apps`.
- Folders can be named in their own header.
- Turnip GPU acceleration on Adreno phones, with the patched Mesa bundle shipped in the APK.
- The status bar on the home screen can be hidden.

## 0.7.2 (versionCode 14) – 2026-10-14

- Text files open in the launcher's own touch-first editor, in the Omarchy theme,
  which is also Linux's `$EDITOR`: Android's selection and keyboard, line numbers,
  find, word wrap and a key row above the keyboard. Each file gets its own Recents
  card, and `git commit` waits until you close the message.
- Files is an Omarchy-style panel with its own icon, and can create a new file.
- Settings open in their own window with a Recents card, and come back as you left
  them after going home or to a terminal mid-install. The launcher's own screens
  open and close without animations.
- Terminals no longer flicker when the soft keyboard opens or closes.
- New coding agents from Omarchy's list: Oh My Pi, Ori, Muse Code and Kimi.
- Linux no longer installs lazygit, and fastfetch stays out of the drawer.
- Checking for system updates in settings records the check time again.
- The widget store's refresh button spins while it reloads.

## 0.7.1 (versionCode 13) – 2026-10-13

- Websites installed from a browser (Install app, or Add to home screen) show up
  on the home screen and in the app drawer, like other apps' shortcuts to home.
  Browsers didn't offer to install them before.
- Long-pressing an Android app in the drawer, the recent apps or on home lists its
  shortcuts, such as Maps' "Navigate home", a chat or Chrome's incognito tab. Tap one
  to open it, or its [+] to put it on the home screen.
- Apps that change their own icon, like Duolingo or X, no longer leave a blank
  tile on the home screen that won't open; it shows the new icon and opens the app.
- The now playing widget's play button works again when it seemed dead: after a
  long pause (Spotify) it opens the app so playback resumes, and for a player that
  can't play in the background (YouTube without Premium) it opens the app. Tapping
  the tile opens the app again on Android 14 and later.
- After you change the phone's language, app names, home tiles and widget dates
  follow it.
- Widgets that only animate some of the time, like a backdrop drawn while music
  plays, stop redrawing while they're still.
- The koi in the koi pond widget turn in arcs instead of flipping round.
- Keeping the theme setup suggests (Tokyo Night) no longer loses it, and its wallpaper,
  the next time the launcher starts.

## 0.7.0 (versionCode 12) – 2026-10-13

- The style lives in `~/.config/omalauncher/style/style.toml`, which you or an agent can edit
  from Linux; changes show within a second, and settings it can't read show in a banner.
  `defaults.toml` next to it lists every setting. Your earlier style carries over.
- New style settings, like Hyprland's: gaps between widgets and to the edges, border widths
  and colors, the focused border, and screen panels' gaps and borders.
- The status bar and the gesture bar can sit on the same glass as the widgets
  (style → system bars); they're bare by default.
- Widgets can show the next alarm and the phone's modes (Do Not Disturb, battery saver,
  ringer, torch), turn the torch on, share text, buzz, and draw app icons (owl 7).
- yay in a terminal no longer asks for root's password, and AUR builds no longer print
  "exec: -v: invalid option".

## 0.6.0 (versionCode 11) – 2026-10-12

- Apps from a work profile show up next to your own, marked with a briefcase,
  and dimmed while work apps are paused.
- Cloned apps carry the clone mark instead of the briefcase, from Android 15 on.
- With a work profile, the drawer splits into personal and work tabs; swipe sideways to switch.
- The work tab pauses work apps and turns them back on.
- Widget screens can raise the keyboard as they open, with `Input { focus: true }`.
- Swipe up on home to open the app drawer, like stock Android; settings → gestures
  can turn it off.
- The app list starts at the top, Linux apps first; closing the drawer clears the search.
- Widgets can follow your corner style, from square to round (owl 6).
- The widget store only takes widgets that pass values into commands safely, with `sh()` (owl 6).

## 0.5.0 (versionCode 10) – 2026-10-11

- Add your own themes, like Omarchy's, and your own picture as the background.
- Themes became the style screen, which also sets the terminal's font and text size.
- The terminal shows and types Chinese, Japanese and Korean, and characters like æ or ö.
- Tablets and wide screens get larger text and use the extra room.
- Terminal tiles are in the widget store.
- Coding agents no longer update themselves.

## 0.4.0 (versionCode 9) – 2026-10-10

- Share widgets as a QR code or a link, and open shared widgets in the launcher.
- Before adding a shared widget, see what it installs, every command it runs and
  what looks risky, or hand it to your agent to review.
- Publish your own widgets to the widget store without a GitHub account.

## 0.3.0 (versionCode 8) – 2026-10-09

- Folders on the home screen.
- Two home pages side by side on foldables.
- Linux apps and Android share the clipboard.

## 0.2.0 (versionCode 7) – 2026-10-08

- New app icon.
- GPU acceleration for Linux apps on Mali GPUs, like the Pixel 9a (Settings › linux).
- Publish your own widgets to the widget store with a long press.

## 0.1.3 (versionCode 6) – 2026-10-08

- Fix installing coding agents on some phones, like the Pixel 10.

## 0.1.2 (versionCode 5) – 2026-10-08

- Home works in landscape.
- Linux windows show up in Recents under their own name and icon.

## 0.1.1 (versionCode 4) – 2026-10-07

- The app list moved into the bar, with recently used apps above it.
- Widgets can open full screen.
- Fixes for many crashes from widgets, Linux apps and uninstalled apps.

## 0.1.0 (versionCode 1–3) – 2026-10-01

- First release: Omarchy themes and wallpapers, home screen widgets and the
  widget store, the Android app drawer, and optional Arch Linux with Linux apps,
  terminal widgets and coding agents.
