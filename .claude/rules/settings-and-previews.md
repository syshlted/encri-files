---
paths:
  - "app/src/**/SettingsActivity*.kt"
  - "app/src/**/settings/**"
  - "app/src/**/ui/Dialogs*.kt"
  - "app/src/**/browse/Previews*.kt"
  - "app/src/**/ui/DocumentAdapter*.kt"
---

# Settings and previews — component brief

This app's settings and what they govern: `SettingsActivity` and `Settings`, which keep the two switches;
`followScreenshotSetting` and `showFollowing`, which put every window and every dialog under the screenshot
setting; and previews, which `Previews` asks of encri and holds, and `DocumentAdapter`'s rows show. The notices row at
the foot of the settings is `release-and-signing`'s (F10). This brief is level 1; the full records are in
`docs/decisions/`.

## Rules that hold here
- **Each setting changes this app alone, and says so.** encri's own screens, and what encri answers other apps, are
  set in encri (encri D112). (F3, F4)
- **Allow screenshots: off by default.** Off, every window this app draws carries `FLAG_SECURE`: screenshots and
  screen recordings are blocked, the app is kept off a screen it is cast to, and the recent apps show a blank card.
  On, the flag is cleared and all of that is allowed. The screen says what turning it on costs: a screenshot is kept
  in the photos, outside the vault, where photo backups and other apps reach it. encri's own screens always block
  screenshots (encri D8), and this app cannot change that. (F3)
- **The flag is applied when a window is made and again whenever a screen comes to the front**, so a change in the
  settings reaches the browsing screen without a restart, and the first frame, the one the app switcher keeps, is
  already right. (F3)
- **Every window follows the setting, each dialog included.** A dialog is a window of its own, and the Activity's flag
  does not reach it, so a dialog is shown with `showFollowing`, never with `show()`. The notices screen follows it too,
  where encri's are always secure: here the rule is the setting, not every window. (F3, F5, F10)
- **No toast names a document**: a toast is drawn by the system, outside this app's windows, where the setting cannot
  reach. A note that names nothing may be a toast. (F5)
- **The choices are kept in the app's private preferences**, a yes or a no each, and stay on the phone: `allowBackup`
  is false. (F3)
- **Show previews: on by default**, a toggle of this app's own (encri D112). Off, the row's slot is gone and nothing
  is asked for. (F4)
- **encri makes and encodes every preview**; this app decodes encri's encoding and never an original. (F4, encri D112)
- **A row asks for its preview when it is drawn, and stops asking when it is scrolled away** (`ListView`'s recycler
  listener, or the row drawn for another document). Cancelling a request cancels encri's work (encri-api L12), which
  matters because encri makes two previews at a time behind a queue of 256 (encri D120). (F4)
- **Held in an `LruCache` of decoded pictures for the process**, an eighth of the heap and never more than 16 MiB,
  keyed by the document's id, its modified time, its size and the size asked, so a rewritten document is a new
  preview. **Nothing is ever written to the phone's storage**, which is why no image library is on the classpath. (F4,
  F1)
- **Dropped at every lock, when the session goes, and when previews are turned off**, with every request in flight;
  an answer to a request made before the drop is thrown away, never shown. A locked vault shows nothing of itself, and
  a picture is more of it than a name. (F4, F2)
- **A preview encri cannot make** (a type it does not preview, or an answer that does not decode) **is not asked for
  again until the next drop; one that merely failed is.** (F4)
- **One square slot per row while previews are on, 48 dp**, so names line up; encri fits each preview inside it
  without enlarging it (encri D121). Not asked for a folder, or for a type encri cannot preview. (F4)

## Traps already paid for
- The common image libraries keep a disk cache by default, which encri D112 forbids for previews. With none on the
  classpath there is nothing to configure wrongly. (F1, F4)
- `FLAG_SECURE` blacks out `screencap`: with the setting off, a hand check that captures the screen gets nothing. (F3)
- Allowed by default would put the vault's names in every app-switcher picture for a user who never chose that; a
  setting per screen is more to explain than it is worth. (F3, rejected)
- encri D112's background pass is encri's to build, and would fill a cache encri keeps. This app asks for what it
  shows. (F4, rejected)

## Measured, not assumed
- **On the A20 (API 30):** `screencap` came back empty, then captured the folder list once the switch was on, then
  came back empty again once it was off. (F3)
- **On the A20:** a photo shared into encri from the Gallery app showed its preview in the folder, the text file beside
  it none, and turning previews off took every slot away. (F4)
- Under Robolectric at API 27 and 36: removing the re-apply on return fails its test at both levels (F3); removing the
  drop at a lock fails the tests that a preview is dropped and that a request in flight is cancelled and never shown.
  94% of lines at F4. (F3, F4)
- The minified release build on the API 34 emulator opened the settings and showed a picture's preview. (F9)
- **Not run on a phone above API 30**, apart from that emulator walk-through.

## Decisions
| | Why it matters here |
|---|---|
| F3 | The screenshot switch, off by default; applied on every return; every window; kept on the phone |
| F4 | Previews: per row, in memory alone, dropped at every lock, 48 dp, the switch on by default |
| F5 | Every dialog follows the setting; no toast names a document |
| F10 | The notices row below the switches, and the notices under the setting |
| F2 | Nothing of a locked vault on screen; the flag's first form, now the setting |
| F1 | No image library, so no disk cache |
| encri D8 | Why the flag at all; encri's own screens always block screenshots |
| encri D112 | The toggle of this app's own, encri encodes, never a preview on storage |
| encri D120, D121 | Two previews at a time behind 256; fitted within the box, never enlarged |
| encri-api L12 | Cancelling a request cancels encri's work |

## Open here
- F3's flag: **blocked by default.**
- F4's flag: **no background pass of this app's own**, so the first look at a folder of photos waits for each preview.

## Read next
- Related briefs: `.claude/rules/vault-and-browsing.md`, `.claude/rules/release-and-signing.md` (the notices)
- Full records: `docs/decisions/INDEX.md`
