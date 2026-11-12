# encri-files — changelog

What happened, newest first. Why is in `docs/decisions/`, and what is next is in `ROADMAP.md`.

## Unreleased

### API 33 on Waydroid, and the delete questions on the A20 (2026-11-12)

**Checked.** Encri Files at `c12b8f6`, a debug build, against encri at `e632c7e`, which has built D147, on the Galaxy
A20 (API 30) and on Waydroid (LineageOS 20, API 33), a ROM at API 33 and not a handset. On Waydroid, the first run of
these at API 33: back went up out of a folder and, at the top, left the app with its activity stopped and not finished
(F2, and F6's account of API 31 and up); the notification question was asked as a copy started, after the picker had
closed, and again at the next copy once refused, and once allowed a 192 MiB copy's notification counted the bytes and
named no file (F8); a taken name was refused in words (F6); `screencap` got nothing with *Allow screenshots* off and a
whole frame with it on (F3); and previews off took every slot away and gave the names its room (F4). On the A20 the
updated app was let in again through encri's consent screen.

**The delete questions on a phone.** The entry below checked the new words with the unit suite alone. On the A20 they
read as written: a folder's question says that everything in it is deleted with it, that it is deleted from the vault,
not put in a bin, and cannot be undone, and that a backup encri made before may still hold a copy; a document's says
the same without the first sentence. Both were cancelled.

**Known issues.** Not run: API 34's type permission and API 35's budget, which are the S24's (F8); back at the top
during a copy, which the A20's picker kept from being tried by showing no tiles in Downloads (F6); a folder copy above
API 30, and its bytes against the originals (F11); a share or an *open with* against an encri that has built D147; and
the G6, the S9 and the S24.

### A delete says it is not put in a bin, as Encri Images' does (2026-11-11)

**Completed.** Deleting a document or a folder now says that it is deleted from the vault, not put in a bin, and
cannot be undone: the words encri-images' I11 calls F5's, which Encri Images already shows. The folder's question
still begins by saying everything in the folder goes with it, which `EditsTest` checks. The suite's
`docs/documentation.md` asks the companions to explain the same things in the same words. The unit suite passes, 358
tests.

### The documentation laid out as the suite's (2026-11-11)

**Completed (F12).** The decision log left `AGENTS.md`, as encri's did: each of F1 to F11 is a record of its own in
`docs/decisions/`, word for word, with an index that `scripts/docs/decision-index.py` writes. The status history, the
conventions, the development environment and the open questions moved to `docs/` unchanged, and the old file was
rebuilt from them byte for byte to check that nothing was lost. `AGENTS.md` is now the map every session loads, 186
lines where it was 783, and eight component briefs in `.claude/rules/` load when their files are touched.
`README.md`, `ROADMAP.md`, `CHANGELOG.md` and `FINGERPRINTS.md` took the suite's titles and pointers, with their
entries unchanged. `signed/` is gitignored.

**Brought up to date in `ROADMAP.md`.** The backup screens' item, with encri-api's L21 and encri D146; B111, which
encri closed on 2026-10-24; and the *Send its name* tick (encri D148) added to the queue.

**Known issues.** Code and older documents still cite `AGENTS.md F5` and the like, which now means
`docs/decisions/F005-*.md`, as `AGENTS.md` says (F12).

### A folder copied in (2026-11-03)

**Completed (F11).** *Copy a folder in*, beside *Copy files in*, copies a folder chosen in the system's picker into the
vault, with the folders in it and all their files. The whole folder is looked through first, so a copy that will not
fit, or whose name is already taken, is refused before anything is written. The folder is made as *(unfinished)* and
takes its own name only once everything in it has arrived; one that did not all arrive keeps the marker with what did,
and the bar names, by its path, each folder and file that was not copied and why. A folder its app will not list, or is
still fetching after half a minute, is said rather than skipped. Checked on the A20 against encri with a folder of five
files (1 GB the largest), three folders in it and one of them empty, with the app's task removed four seconds into the
copy: everything arrived, at its size and with its date. The same build without the change that hands the folder's
grant to the copy's service as a prefix lost the three files read after the task was gone, which is what the change is
for.

### The first release (2026-10-29)

**Under way (F9).** Every build but a developer's is made against the encri-api commit `config/encri-api.pin` names,
`c27f139`, which encri's first release was built against; CI had been building against whatever encri-api's `master`
held. The pre-push hook refuses a push while `../encri-api` is anywhere else. The Gradle wrapper and its distribution
are held to the checksums gradle.org publishes. `scripts/release/reproduce.sh` builds the release twice from one
commit, in two environments that differ on purpose, and says whether they came out the same; GitHub's CI runs it on
every tag, and the maintainer's run must agree with CI's before anything is signed. `scripts/release/kit.sh` packs
the build with what signs it, for the machine that holds the keys, whose `sign.sh` keeps no signature but by the three
keys `config/release-certificates` names; `FINGERPRINTS.md` lists them.

**Third-party notices (F10).** *Settings* ends with *Third-party notices*, which shows the copyright notices and
licences of the libraries the app is built with, as their licences require of a binary distribution. A build fails
when the app carries a library the notices do not name, at its version, or the notices name one it does not carry.

**Checked (F9).** The minified release build, which had never been installed before, was walked through on the API 34
emulator against encri: let in, browsing, the settings and notices, copies in and out byte for byte, folders, moving,
deleting, sharing, opening with another app, a preview, and locking. Nothing crashed.

**Bug found and fixed before it shipped.** The *Third-party notices* row in the settings was drawn in the grey of the
explanations around it, so it did not look like something to press. It is drawn as the switches are now.

**Pushed (F9), 2026-10-30.** The repository is on GitHub, its `master` held by a ruleset against deletion, force pushes
and unsigned commits. GitHub's CI built the release from `6644860` and got the same two files, byte for byte, as the
maintainer's two builds: APK `7d447874…`, bundle `857967e6…`. The kit that signs them is made.

### A copy that carries on (2026-10-23)

**Completed (F8).** A copy into or out of the vault runs behind a foreground service and a notification, so it carries
on when the user goes to another app or swipes Encri Files away. The notification counts files and bytes, offers
*Stop*, and says how the copy ended when no screen of the app is in front; it never names a file. The app now declares
its first permissions: two for the service, and one, asked for when a copy starts, to post the notification. Checked on
the A20 against encri with a 2 GB file each way, the app's task removed mid-copy, and *Stop* pressed in the
notification.

**Known issues.** ~~The notification question at API 33+, and the service at API 34+, have not run on a phone.~~ The
service at API 34+ has not run on a phone; the notification question ran at API 33 on Waydroid, a ROM, on 2026-11-12.

### Sharing out (2026-10-23)

**Completed (F7).** *Share* on a document's long press says what a share costs, then hands the document to the share
sheet through a handoff. Checked on the A20: Nextcloud uploaded a shared document to the test server byte for byte.

**Bug found and fixed before it shipped.** The share sheet offered encri itself, whose share screen refuses its own
documents. encri is left out of the sheet.

### Copying in and out (2026-10-23)

**Completed (F6).** *Copy files in* in the menu takes any number of files from the system's picker into the folder
being looked at; *Copy out of the vault* on a document's long press says what a copy out costs and then asks where it
goes. Each file is copied in under an *(unfinished)* name and renamed once it is whole, a taken name is refused, a copy
that will not fit is refused before it starts, and a file keeps its date. A bar at the foot shows how far a copy has
got, with Stop, and how it ended. Checked on the A20 against encri, a 400 MB file included.

**Bug found and fixed before it shipped.** A file copied in lost its date, because encri's rename stamps the time and
the date was set before the rename. It is set after.

**Known issues.** ~~A copy runs in the app's process, so Android may pause or stop it while another app is in front
(`ROADMAP.md`, *Later*).~~ Fixed by F8.

### Folders, renames, moves and deletes (2026-10-23)

**Completed (F5).** *New folder* in the menu, and *Rename*, *Move* and *Delete* from a long press, each one
request to encri. The app asks for every right; a grant that holds fewer offers *Allow more in encri*. A taken name
is refused rather than changed, a move is made by going to the folder, and a delete is confirmed by name. Every
dialog follows the screenshot setting. Checked on the A20 against encri.

### Previews (2026-10-23)

**Completed (F4).** A photo or a video gets a preview in its row, made and encoded by encri, held in memory alone
and dropped at every lock. *Show previews*, on by default, is in the settings screen and changes Encri Files alone;
off, nothing is asked for. Checked on the A20 with a photo shared into encri from the Gallery app.

**Bug found and fixed.** A document changed under a minute ago read *0 minutes ago* on the A20, as encri's own
screens once did (its B98). It reads *just now*.

### A setting for screenshots (2026-10-23)

**Completed (F3).** *Allow screenshots*, off by default, in a settings screen reached from the menu. It says what a
screenshot of the vault costs, and that it changes Encri Files alone. Checked with `screencap` on the A20.

### The first screen (2026-10-23)

**Completed (F2).** The app connects to encri, asks the user to let it in on encri's consent screen,
unlocks the vault on encri's unlock screen, lists the vault a folder at a time, folders first, and opens a
document with another app through a handoff. It asks to read and for nothing more. The session belongs to
the process, so a rotation keeps it and the place; a locked vault leaves nothing of itself on the screen.
Run by hand against encri on the Galaxy A20.

**Bug found, in encri.** A document saved through encri's share screen came with no notice that a folder
changed, so this app showed an empty vault until it was asked again. Filed as encri's B111. This app now
lists the folder again whenever its screen comes to the front.

**Known issues.** ~~The API 33+ back callback has not run on a phone.~~ The callback ran on Waydroid, a ROM at API
33, on 2026-11-12, and on no handset yet. Every document opened mints a handoff
that lasts until the vault locks (`docs/decisions/open-questions.md`).

### The scaffold (2026-10-23)

**Completed (F1).** A Gradle project with one application module, `com.systemhalted.encri.files`, called
"Encri Files" for now. encri-api's toolchain, hooks, CI and gates, an 80% line gate on the module, the
dependencies pinned by sha256 and the release classpath locked. encri-api is an included build.

**Known issues.** CI cannot build until encri-api's rename is pushed (`ROADMAP.md`).
