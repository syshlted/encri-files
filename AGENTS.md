# encri-files — agent guide

> **Level 0 of four**, loaded into every session: only what is true for every component, laid out as the suite's
> `docs/documentation.md` says. The rest loads when you touch a component or is read when needed. Keep it under 200
> lines. Dates: YYYY-MM-DD.

## What this is

**encri's file manager**: an Android app that browses an encri vault and hands its documents to other
apps. It reaches the vault only through encri's companion service over AIDL, never through the system
file picker (encri D112). So it works with encri's file-picker access off, which is how every vault
starts (encri D47), and Android keeps no record of which document went to which app.

It builds against [encri-api](https://github.com/syshlted/encri-api)'s client library exactly as any
other app would (encri D115). It is meant to be read and lifted from, and is licensed Apache-2.0, as
encri D115 chose and still flags for veto. encri itself is not open source (encri D116).

What crosses the boundary between this app and encri, and why, is decided in encri's decision records —
D112 to D118, D120 and D123 — and in encri-api's for the wire. It is not restated here. What this repository
owns is the app.

## Relationship to the other repositories

- **Decisions here are numbered F1, F2, …**, and the others' are cited as *encri D120*, *encri-api L12*,
  *encri-images I3* and *encri-backup K1*. Five logs each numbered from 1 would make "D3" mean five things.
- **About the app, this repository's decision records win.** About the contract, encri-api's do; about
  encri's internals, encri's do, and these link rather than restate.
- **The toolchain is encri's and encri-api's, version for version** (F1).
- **B is encri's bug numbering** (*encri B111*), and **S1 to S4 are severities**, never decision numbers.
- **encri-api is built from source at the commit `config/encri-api.pin` names**, in every build but a developer's:
  `ci.yml`, `reproducible.yml` and `reproduce.sh` fetch it with `scripts/ci/encri-api`, and the pre-push hook refuses
  a push while `../encri-api` is anywhere else (F9, encri D126). A developer's build includes `../encri-api` as it
  stands, or wherever `-Pencri.api.dir` points (F1).
- **The siblings**, each a repository with a decision log of its own: `../encri-core` (encri itself, D1, …),
  `../encri-api` (the contract, the client and the registry, L1, …), `../encri-images` (Encri Images, the gallery,
  I1, …) and `../encri-backup` (Encri Backup, the scheduled backup, K1, …). In the suite superproject they sit side
  by side, which is where this build finds `../encri-api`; the suite's `docs/documentation.md` sets how each one is
  documented.

## How the project's knowledge is laid out

| Level | Where | Loads | Holds |
|---|---|---|---|
| 0 | `AGENTS.md` | every session | constraints, invariants, hazards, conventions, this map |
| 1 | `.claude/rules/<component>.md` | when a file matching its `paths:` is read or edited (other tools: read it by hand) | one component's rules, traps, measurements and decisions |
| 2 | `docs/decisions/INDEX.md` | on demand | one line per decision: date, title, status, the briefs that cite it |
| 3 | `docs/decisions/Fnnn-<slug>.md` | on demand | the full decision record |

**Finding a decision.** `AGENTS.md F5` or `F5`, in code or older documents, means `docs/decisions/F005-*.md`: every
record lived in this file before 2026-11-11 (F12). Later decisions amend earlier ones in place, and a record's footer
lists the corrections `docs/decisions/amendments.md` holds.

**Elsewhere:** `ROADMAP.md` (what is next, and the bugs), `CHANGELOG.md` (what happened), `README.md` (for people)
and `FINGERPRINTS.md` (the keys a release is signed with). In `docs/`: `status.md` (history), `dev-environment.md`
(this machine, as this app uses it), `conventions.md` (in full), and `decisions/amendments.md` and
`decisions/open-questions.md`.

## Components (level 1)

| Brief | Covers |
|---|---|
| `vault-and-browsing` | the session with encri and the screen in front of the vault, browsing, the manifest and the strings |
| `settings-and-previews` | the settings screen, the screenshot setting on every window, previews held in memory |
| `vault-changes` | new folders, renames, moves and deletes, the rights asked for, encri's refusals in the user's terms |
| `handoffs-and-sharing` | *open with* and the share sheet through encri's handoffs, and what a share says first |
| `copies` | files and folders copied in and a document copied out: the copy, the picker's side, the bar's words |
| `transfer-service` | the foreground service a copy runs behind: its grants, permissions and notifications |
| `build-and-ci` | Gradle and the toolchain, the gates, dependency pinning, the hooks, CI, the encri-api pin |
| `release-and-signing` | reproducible releases, the signing kit, Play and GitHub, the fingerprints, the third-party notices |

A brief stays under 12 KB; one that outgrows that is split into two components rather than grown.

## Hard constraints

These are external facts that shape everything else. Not negotiable.

1. **A handoff lasts only as long as the session that made it, and ends when the vault locks** (encri D117). So the
   session belongs to the process, not to a screen (F2), and an app that reads a share after a lock, or after this
   app is closed, finds nothing, which the share says first (F7).
2. **encri keys a grant on this app's application id** (encri D120), so `com.systemhalted.encri.files` is permanent
   from the first build a user installs (F1).
3. **encri saves a write only when the writer finishes** (encri-api L11). A copy that is stopped is abandoned by
   throwing out of the writer, never by returning early (F6).
4. **A file the system's picker hands over is readable only while the task of the screen that received it is alive**,
   or, handed to a service in its start intent, until the service stops for that start; a folder's grant reaches what
   is in it only as a prefix of its tree URI (F6, F8, F11).
5. **`FLAG_SECURE` on an Activity reaches none of its dialogs, toasts or notifications.** A dialog is a window of its
   own (F3); a toast and a notification are drawn by the system, outside this app's windows (F5, F8).
6. **encri's notice that a folder changed is best effort** (encri-api's `PROTOCOL.md`), so a folder is listed again
   whenever the screen comes to the front (F2).
7. **Android limits work in the background**: from API 31 it refuses a foreground service to an app in the
   background, from API 34 it may pause a background process, and from API 35 a `dataSync` service has six hours a day
   (F8).
8. **encri cannot run below API 27** (encri D14), so minSdk is 27; targetSdk is 36 (F1).

## Invariants

Violating any of these is a design bug, not a tradeoff.

- **The vault is reached only through encri-client**, never through the system file picker (F1, encri D112).
- **Nothing of the vault is written to this phone's storage by this app.** Previews live in memory alone (F4, encri
  D112), a copy is never staged (F6), and a share is a handoff, never a decrypted copy (F7). A copy out goes only to
  the place the user picked (F6).
- **A locked vault shows nothing of itself**: no document, no folder's name in the title, no name in a copy's bar, and
  no preview. The place is kept in memory, and an unlock lists it again (F2, F4, F6, F11).
- **No document's name reaches a notification or a toast.** A notification counts (F5, F8, F11).
- **Every window this app draws follows the screenshot setting**, each dialog and the notices included; off, which is
  the default, screenshots are blocked (F3, F5, F10).
- **Plaintext leaves the vault only once the user has read what it costs, every time.** A copy out and a share are
  each confirmed, with no *don't ask again* (F6, F7; encri D118, D123).
- **A name is never changed into one the user did not choose.** A taken name is refused, for a change, a file copied
  in and a folder copied in (F5, F6, F11); only a folder's `(unfinished)` marker takes a free name while it fills
  (F11).
- **Nothing reads as whole that is not.** A copy in is made as `(unfinished) <name>` and takes its own name only when
  whole, and a folder only once everything in it has (F6, F11). What cannot be listed whole is said, never skipped
  (F11).
- **Nothing here starts one of encri's screens on its own.** A remedy (*Ask encri*, *Unlock*, *Try again*, *Allow
  more in encri*) is a button the user presses (F2, F5; encri D17).
- **No persistable grant is taken** on what the picker hands over, so nothing is written to the system's record of
  kept grants (F6, F11).

## Hazards

- **Never run encri's `:app:connectedDebugAndroidTest` on a phone that holds a vault.** It uninstalls encri, and the
  vault and its keys go with it (encri D39). A device run here goes against the encri already installed.
- **Never run two Gradle invocations at once**: the included encri-api build is one more set of outputs for them to
  fight over. Gradle runs on JDK 21 (`docs/dev-environment.md`).
- **`signed/` is never committed.** It holds a release's signed files, brought back from the key machine; it is
  gitignored (F12).
- **The project's earlier name is never written**: not in code, docs, commits or any sibling repository (encri D124).

## What this project keeps finding

- **A phone finds what Robolectric does not.** encri's rename stamping the time over a copied file's date (F6), encri
  offered in its own share sheet (F7), a share that came with no notice (F2, encri B111), and the notices row drawn
  as one more explanation (F9, F10). Every record says what ran on a phone, and what did not.
- **A test that has never failed has not been tested.** Each record reverts its guards one at a time and watches the
  tests that hold them fail (F3, F4, F5, F6, F8, F11).
- **Measure both ways, and record what was not measured as unmeasured.** The prefix grant was measured with and
  without its flag (F11); Play's key timing was found by trying an upload (F9).
- **Defects live in the seams between correct pieces**, as encri found: a date set before a rename that stamps the
  time is two correct steps in the wrong order (F6; encri B6, B14).

## Code rules in every module

- **Framework views, built in code: no AndroidX, no Compose, no image library** (F1). What a user receives is the
  client, its coroutines and the Kotlin standard library, which `app/gradle.lockfile` lists.
- **Screens are thin, and what they show is decided by pure functions with tests that need no phone** (`Screen.of`,
  `Outcomes`, `Rows`, `TransferWords`). The screens run under Robolectric at API 27 and 36 against a stand-in for
  encri (F1, F2, F5).
- **One coverage gate on the whole module, nothing excluded: 80% of lines, and 85% of changed lines in a pull
  request** (F1, encri D5).
- **Another app's provider is reached only through an unstable client**, so its death is a failed read and not this
  app's death; `ForeignDocumentsTest` holds every source to it (F6, encri B100).
- **A message encri meant for a developer never reaches the screen** (F2, F5; encri-api's `PROTOCOL.md`).

## Repository conventions

Full text, with the reasons: `docs/conventions.md`.

- **Default branch is `master`**, by SystemHalted organization policy. Never rename it to `main`.
- **Commits are signed and Conventional.** lefthook runs signing preflight, ktlint and detekt on commit, the message
  check, and signatures, the encri-api pin, the unit tests and the notices gate on push. Every rule a hook enforces is
  enforced again in `.github/workflows/`, because `--no-verify` always exists.
- **Every commit builds and passes its own tests, not just the branch tip.**
- **Merge with a merge commit or a rebase, never a squash** (encri D7).
- **Dependencies are pinned twice** (F1): `gradle/verification-metadata.xml`, regenerated from an empty file over
  every task a workflow runs, and `app/gradle.lockfile` for what ships, in STRICT mode
  (`./gradlew assembleRelease --write-locks`).
- **Bootstrap a fresh clone**: `lefthook install`, then `scripts/ci/encri-api fetch ../encri-api`. Bump
  `config/encri-api.pin` in the commit that needs the newer encri-api, and push encri-api first (F9).
- **`.claude/rules/` is committed; the rest of `.claude/` stays per-machine, and `signed/` is never committed** (F12).

## Recording what is decided

1. **A settled design question becomes a decision record**, not only an answer in chat:
   `docs/decisions/Fnnn-<slug>.md`, the next free number, beginning `# Fnnn — <title>` then
   `*YYYY-MM-DD · accepted…*`, with what was rejected and measured, and *flagged for veto* where unconfirmed.
2. **Regenerate the index and footers**, `python3 scripts/docs/decision-index.py .`, never by hand; then check that
   the briefs still cover the tree with `python3 scripts/docs/brief-coverage.py .`.
3. **Update the brief** of every component the decision governs: add the rule and a row to its *Decisions* table.
   A brief must stay under 12 KB; if one outgrows that, split the component.
4. **Amend earlier records in place**, with strike-through and "amended by Fnnn", and add a line to
   `docs/decisions/amendments.md`. Bugs go in `ROADMAP.md`; what landed, in `CHANGELOG.md`.

**When they disagree, the decision records win**, and the briefs, `ROADMAP.md` or `CHANGELOG.md` are fixed.
