# BrowserDev

A page for working on, apart from home. This is the fork's development browser: it opens a site served from
the Arch guest, or any page on the web, and an agent can attach to it with the Chrome DevTools Protocol to
look at it, drive it and screenshot it.

It is not the widgets' `Browser` node, and the two share nothing but the WebView's own hardening. A tile is
https only, with the host written out in the widget file, and nothing outside the launcher can steer it. This
window does the opposite of each of those, on purpose, which is why it is a window of its own rather than an
option on the node.

## Opening it

From the guest:

```sh
browser-dev                 # http://127.0.0.1:5173/
browser-dev 3000            # a bare port is http://127.0.0.1:<port>/
browser-dev http://127.0.0.1:8080/docs
browser-dev https://example.com/     # any https page too
```

`browser-dev` writes to the same FIFO as `xdg-open`, so it works from anywhere in the guest: an agent in a
terminal, a `package.json` script, a Makefile target, a hook. The command is installed with the other shims
in `/usr/local/bin`.

Run it again and the same window loads the new URL instead of opening a second one.

## What it loads

| | |
|---|---|
| `https://` | anywhere, as a tile's page does |
| `http://localhost`, `http://127.0.0.1`, `http://[::1]`, `http://127.*` | allowed, any port and path. The guest shares the phone's network, so a dev server in Arch is right there |
| `http://` anywhere else | refused: the page would be in the clear off the device |
| `file:`, `content:`, `javascript:`, `data:` | refused, in the address bar and in links |

A link to another app's scheme (`mailto:`, `tel:`, `market:`) goes to that app, as `url(...)` does. An
`https` page still cannot pull in `http` subresources (`MIXED_CONTENT_NEVER_ALLOW`), so a page that is
itself `http://127.0.0.1` is the easier case for a dev server.

Script and DOM storage are on here, where a tile needs `js: true`. File and content access, geolocation,
popups and self-starting media are off, exactly as in a tile.

## Attaching with DevTools

The debug socket is a local abstract socket in the dev process, so it needs adb (wireless debugging on):

```sh
pid=$(adb shell pidof dev.omalauncher.fold:dev)
adb forward tcp:9222 localabstract:webview_devtools_remote_$pid
```

The line is printed at the bottom of the window too, with its own pid filled in, so it can be copied from
there. Then, from the machine with adb:

```js
// Playwright, over CDP
const browser = await chromium.connectOverCDP("http://127.0.0.1:9222");
const page = browser.contexts()[0].pages()[0];
await page.screenshot({ path: "tile.png" });
```

`chrome://inspect` in a desktop Chrome, with the same forward in place, lists the page by hand. Browser
tiles are skipped by `owl-check` previews ("Browser tiles aren't previewed"), so a screenshot taken this way
is also the only picture of a page that a widget can have.

## Why it is safe to leave in a release

- It runs in `android:process=":dev"` with `WebView.setDataDirectorySuffix("dev")`, so its cookies and
  storage are not a tile's, and its WebView does not fight home's for the WebView data directory.
- `setWebContentsDebuggingEnabled(true)` is process-wide, so it reaches this process only. Home's WebViews
  never get a debug socket, in a release or a debug build.
- The socket exists while this window does. Close it and it is gone.
- The activity is not exported, so only the launcher starts it; there is no `am start` and no other app.

## Files

- `app/src/main/kotlin/dev/omalauncher/BrowserDevActivity.kt`: the window, the URL rule (`normalize`), the
  WebView hardening.
- `app/src/main/kotlin/dev/omalauncher/linux/OpenBridge.kt`: the `browser-dev` shim and its `browser-dev:`
  prefix on the FIFO.
- `AndroidManifest.xml`: the activity, its `:dev` process and `singleTask`.
