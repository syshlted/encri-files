---
paths:
  - "app/src/**/FilesActivity*.kt"
  - "app/src/**/FilesApplication*.kt"
  - "app/src/**/link/**"
  - "app/src/**/browse/Browser*.kt"
  - "app/src/**/ui/Screen*.kt"
  - "app/src/**/ui/FilesViews*.kt"
  - "app/src/**/ui/Rows*.kt"
  - "app/src/**/ui/SystemBars*.kt"
  - "app/src/test/**/TestApplication.kt"
  - "app/src/test/**/Fixtures.kt"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/res/**"
---

# Vault and browsing — component brief

The connection to encri and the screen that browses the vault: `FilesApplication`, which holds the process's
`VaultLink` and `Browser` behind `Vaults`; the `link` package (`ClientOpener`, `Opening`, `VaultLink`,
`VaultSession`, `Asked`); `FilesActivity` and what it draws (`Screen`, `FilesViews`, `Rows`, `padForSystemBars`);
the manifest and `strings.xml`; and the stand-in for encri the screen tests run against (`TestApplication`,
`Fixtures`). What crosses the boundary is encri's and encri-api's to decide (encri D112 to D118, D120). This brief
is level 1; the full records are in `docs/decisions/`.

## Rules that hold here
- **The session belongs to the process, not to a screen.** `FilesApplication` holds the `VaultLink` and the
  `Browser`, and a screen draws them. A handoff ends when the session that made it ends (encri D117), so a session
  owned by the Activity would close a document another app is showing whenever the phone turned. (F2)
- **A session encri ends is asked for again at once**: the user turned this app out, turned access for other apps
  off, or encri's process went. The screen then says why there is no session, never shows a vault that is gone. When
  encri replaces a grant it ends the old session, and the app opens a new one at once. (F2, F5, encri-api L23)
- **It asks for every right, because it uses every one** (`Asked.RIGHTS`). A grant that holds fewer offers only the
  changes it covers, and *Allow more in encri*. The rules for changes are `vault-changes`'. (F5, superseding F2)
- **While the vault is locked the screen shows nothing of it**: no document, and no folder's name in the title. The
  place is kept in memory and an unlock lists it again, because the session outlives the lock (encri D120) and the
  names are already in this process. (F2, encri D17)
- **A remedy is a button the user presses**: *Ask encri*, *Unlock*, *Try again*. Nothing here starts one of encri's
  screens on its own. (F2, encri D17)
- **The folder is listed again whenever the screen comes to the front**, as well as when encri says it changed.
  encri's notice of a change is best effort (encri-api's `PROTOCOL.md`), and one listing per return is the price. (F2)
- **Folders first, then names as the phone's language sorts them** (`Ordering`, `java.text.Collator`), never encri's
  catalog order. (F2)
- **Back goes up a folder before it leaves**: through `OnBackInvokedCallback` from API 33, opted into in the manifest
  so every ROM uses it, and through `onBackPressed` below. While a copy runs, back at the top sends the app to the
  background instead (F6, `copies`). (F2)
- **A message encri meant for a developer never reaches the screen** (encri-api's `PROTOCOL.md`). (F2)
- **The application id, `com.systemhalted.encri.files`, is permanent** from the first build a user installs, because
  encri keys a grant on it (encri D120). The launcher name, *Encri Files*, is a string resource and can change in any
  release. (F1)
- **Framework views built in code, a `ListView` with a framework row for a folder**, and no AndroidX: predictive back,
  insets and results use the platform's APIs directly. (F1)
- **Every screen pads its content out from under the system bars with `padForSystemBars`**: targeting 36, the window
  is edge to edge from Android 15 whatever it asks for, which encri found on a phone rather than in a test (encri D39).
- **The manifest asks nothing for the vault.** Its three permissions are a copy's (F8, `transfer-service`);
  `allowBackup` is false (F3); the settings and the notices are not exported (F3, F10).
- **The screens run under Robolectric at API 27 and 36 against a stand-in for encri**, so every refusal is reachable
  off a phone; `Screen.of` is a pure function, so each state has a test that needs no phone. (F1, F2)

## Traps already paid for
- A note saved through encri's share screen arrived with no notice that its folder changed, and the screen showed an
  empty vault until asked again (encri B111, closed by encri 2026-10-24). Listing again on return is what hides such a
  miss from this app's users. (F2)
- `configChanges` keeps the Activity through a rotation and through nothing else. (F2, rejected)
- encri lists in its catalog's byte order, which puts `Zebra` before `apple`. (F2)
- A document changed under a minute ago read *0 minutes ago* on the A20, as encri's own screens once did (encri
  B98). `Rows` says *just now*. (CHANGELOG, 2026-10-23)

## Measured, not assumed
- **On the Galaxy A20 (API 30), against encri's debug build:** with no vault, encri answered *locked* and its own
  screen made one; the consent screen named the app from the system; a note in *Received* listed and opened in HTML
  Viewer; locking left no name on screen, and the unlock came straight back to *Received*; a rotation kept the place
  and the session; Up and back went up, and back at the top left. (F2)
- At F2: 57 test cases, 76 runs at API 27 and 36, 91% of lines. Removing the listing on return fails its test at both
  levels. (F2)
- The minified release build, on the API 34 emulator, connected, was let in through encri's consent screen, listed
  the vault, and locked and unlocked through encri. (F9)
- **Not run on a phone:** the API 33+ back callback, which Robolectric only registers; and the G6, the S9 and the
  S24. (F2)

## Decisions
| | Why it matters here |
|---|---|
| F2 | The session for the process, nothing shown while locked, listing on return, the order, back |
| F1 | One module, framework views, the application id, the screens under Robolectric at two levels |
| F5 | Every right asked for, *Allow more in encri*, a new session when encri replaces the grant |
| F3 | `allowBackup` false; every window under the screenshot setting (`settings-and-previews`) |
| F8 | The manifest's three permissions and its service (`transfer-service`) |
| F10 | The notices activity, not exported (`release-and-signing`) |
| F12 | `AGENTS.md Fn` in KDocs means `docs/decisions/F00n-*.md` |
| encri D112, D117, D120 | The vault through AIDL alone; a handoff ends with its session; grants and sessions |
| encri D17 | A prompt starts with the user; a locked vault's names come off the screen |
| encri D39 | Edge to edge, and why every screen calls `padForSystemBars` |
| encri-api L23 | Allowing replaces a grant and ends its sessions; a refusal changes nothing |

## Open here
- The API 33+ back callback has not run on a phone. (F2)
- **The visible name**: *Encri Files* is for now (F1, `docs/decisions/open-questions.md`); a trademark search for
  encri is owed before anything is published beyond testers (encri D124, D136).
- **Device tests against encri**, as encri-api's L13 runs them, are not built: every run so far was by hand
  (`ROADMAP.md`, *Now*).
- **A grant whose folder another app deletes** leaves this app's session open and told nothing, in encri 0.1.0; its
  root then answers not found. encri-api's L23 promises the end, and it is encri's to fix (encri B143).
- KDocs here cite `AGENTS.md F2` and the like, which now means `docs/decisions/F002-*.md` (F12).

## Read next
- Related briefs: `.claude/rules/settings-and-previews.md`, `.claude/rules/vault-changes.md`,
  `.claude/rules/handoffs-and-sharing.md`
- Full records: `docs/decisions/INDEX.md`. How the screen in front of the vault is worded in every companion: the
  suite's `docs/documentation.md`, *Text in the apps*.
