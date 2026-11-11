# F9 — A release is built from a tag against a pinned encri-api, twice, and signed from a kit of its own
*2026-10-29 · accepted, at the user's direction: Encri Files' first release goes to Google Play's internal testing
track, and is checked as encri D116 has it, by GitHub's CI building the tag and the maintainer's build agreeing
("Push; CI builds the tag"). encri D136 gave both apps one first release; encri's own went to the internal track the
same day. Built in parts, each in a commit of its own, as below.*

**encri-api at the commit `config/encri-api.pin` names, in every build but a developer's.** encri-api is an input to
every APK (F1), and CI checked out its moving `master`, so a tag built twice a week apart could be built against two
different libraries, and nothing in the tree said which. `scripts/ci/encri-api` is encri's script of the same name
(encri B108, D126): `fetch` clones the pinned commit, `check` says whether `../encri-api` is at it with nothing
uncommitted, and the pre-push hook runs `check`, so a push carries a pin the hook's tests ran against. CI fetches the
pin into `../encri-api`, the sibling `settings.gradle.kts` includes by default, and no longer passes
`-Pencri.api.dir`. The pin starts at `c27f139`, the commit encri's first release was built against (encri-api L22),
which lists this app in the registry under the app signing key and puts that key in `EncriTrust.RELEASE_CERTIFICATES`.
*Measured:* the suite, 285 runs, and the minified release built against it with no change here; `fetch` from GitHub
put a clone at the pin; `check` on a clone one commit back failed with exit 1.

**The Gradle wrapper is held to what gradle.org publishes.** A release's two builds share one Gradle distribution, so an
altered one would run inside both and both would agree (encri B115). `gradle-wrapper.properties` now carries
`distributionSha256Sum`, the wrapper refuses a distribution that does not match it, and
`scripts/ci/gradle-wrapper-check`, encri's, holds the jar and that sum to the checksums gradle.org publishes for the
version, first in `ci.yml`. The jar and the sum are encri's, for the same Gradle 9.7.1. *Measured:* the check failed
before the sum was added, naming it, and passes after.

**A release is built twice, by GitHub's CI from the tag and by the maintainer, and must come out the same** (encri
D116 as written, which encri D136 keeps for this app). `scripts/release/reproduce.sh <commit>` and
`compare-builds.py` are encri's, carried here because this repository is the one a stranger can read: they are what
lets anyone rebuild a tag and hold the published APK to it, which is D115's second row and a claim encri's closed
source cannot make for itself. The two builds differ in path, umask, timezone, locale and Gradle home, share the
pinned Gradle and every pinned dependency, fetch encri-api at the tag's pin beside each clone, and keep the unsigned
APK and bundle as `build/reproducible/<commit>/encri-files-<commit>-unsigned.*` with their `SHA256SUMS` and a
provenance naming both commits. It refuses a commit that predates the pin, the wrapper's checksum or the notices
(F10). `.github/workflows/reproducible.yml`, encri's too, runs it on every `v*` tag, weekly and by hand, and keeps
what it built for 90 days; the maintainer's run must give the same SHA-256s before the kit is made. Nothing in
`app/build.gradle.kts` changed for it: this app ships no native library, so encri's `keepDebugSymbols` has nothing to
keep, and `dependenciesInfo.includeInApk = false` was there from F1. *Measured, 2026-10-29:* `27648e2` built the same
twice, APK `568abcc2…`, bundle `cdb0983e…`, against encri-api `c27f139`.

**Signed from a kit of this repository's own, on the key machine** (encri D141). `scripts/release/kit.sh`, `sign.sh`
and `bundletool.init.gradle.kts` are encri's, for this app's files: the kit packs the reproduced build with
apksigner and bundletool's jars, each pinned, and rehearses itself before it is kept; on the key machine `sign.sh`
takes the directory holding the keystores, asks for each password at a terminal, signs the APK with the app signing
key and the bundle with code transparency and then the upload key, holds both to the build, and keeps nothing signed
by a key `config/release-certificates` does not name. `sign.sh --verify`, here, checks what comes back before the
upload. The keystores are found as `encri-app-signing.p12` (`app-signing`) and `encri-transparency.p12`
(`transparency`), the same two keys as encri's (encri D140), and `encri-files-upload.p12` (`encri-files-upload`);
`ENCRI_UPLOAD_KEYSTORE` and `ENCRI_UPLOAD_KEY_ALIAS` name another, as for the other two.

**The upload key is this app's own**, `59:70:D4:B7…`, which Play takes from 7:03 PM on 2026-10-30 by the Console's
clock, 23:03:47 UTC, when the reset requested on 2026-10-28 takes effect (encri D140). ~~Until then Play takes only
encri's `46:0C:90:61…` for this app, so no bundle this kit signs can be uploaded before that time, and the release
waits for it rather than being signed with encri's key and leaving the reset unused.~~ Until then Play takes no upload
for this app at all, by either key, as was measured below.

**On Play, the bundle needs the listing's automatic protection off** (encri D144): the bundle carries code
transparency, which Play refuses beside it. **The signed APK goes to a GitHub release** (encri D142), and not before
the trademark search encri D136 asks for before anything is published beyond testers.

*Flagged for veto:* **a kit of this app's own rather than one kit for both apps**, which encri D141 left open.
This repository is the one a stranger reads, so it carries everything its release is built and signed with; a kit
names one app's files and expects one app's upload key; and encri's measured scripts are left as they are. What it
costs: `reproduce.sh`, `compare-builds.py`, `kit.sh` and `sign.sh` are now in two repositories, and a fix to one has
to be made to the other, as encri's B138 already is in both.

**The minified build is walked through before it is signed** (encri D140), because no record showed a release build of
this app had ever been installed: R8's usual failures, a class reached by reflection, would reach every tester at
first launch. *Done 2026-10-29 on the API 34 emulator with `a2ef986`*, which `reproduce.sh` built the same twice (APK
`079033c4…`, bundle `fa4750e7…`), signed with the debug key and installed fresh beside encri's own walked-through
build: Encri Files connected, was let in through encri's consent screen (which, the copy being debug-signed, said an
app of this name is reviewed but *this one is signed with a different key*, as it should), listed the vault, opened
the settings and the notices, asked for notifications when the first copy started (API 33's question, which had never
run on a device), copied a 5 MiB file in with its date and out again with the same SHA-256, made, renamed and deleted a
folder, moved a document into it, shared one (encri not among the targets), opened one in HTML Viewer, showed a
picture's preview, and locked and unlocked through encri. Nothing crashed, and logcat held no class, method or field
R8 had removed. It found one thing, F10's row drawn as an explanation; the build after the fix was installed again and
the settings looked at.

*Measured, 2026-10-29,* with a kit of `27648e2` (21 MB) and three RSA-4096 scratch keystores under the names above: the
kit rehearsed itself; a real signing through a pseudo-terminal asked three questions, one per tool; with keys the kit
did not expect it failed with exit 1 and left no `signed/`; Ctrl-C at the third question, the terminal closed at the
second or the third, SIGTERM to `sign.sh` at the second, and jarsigner killed at its question each ended with no
`signed/` and no process left; and a kit expecting the scratch keys signed, its files the build but for alignment
padding (all 19 entries) and signing's 4 bundle entries, and `--verify` passed them with that kit and failed them with
the real one. *Seen once and not reproduced:* a `signed/` left holding the signed APK alone, when the terminal,
`sign.sh` and bundletool were each sent SIGTERM at the same moment; `--verify` refuses such a directory for want of
`SIGNED.txt`, and a second signing refuses to write over it.

*Measured, 2026-10-30, while the reset was pending,* at the user's direction to try an upload before the new key was
valid. A one-off kit of the same build, made from a scratch clone at `v0.1.0` whose `config/release-certificates` named
encri's upload key and nothing else differed, signed the bundle with that key and passed `--verify`; Play refused it
as signed with the wrong key, expecting SHA-1 `2C:EC:C9:1C…`, which the user's keytool showed is this app's own. So
Play stopped taking encri's key for this app when it approved the reset, not at the time it gave. This kit then signed
with this app's key and passed `--verify`, and Play refused that too, as "an upload certificate that is not yet valid
because it has been recently reset", until 23:03:47 UTC. While an upload key reset is pending, nothing can be uploaded.
The two kits' APKs came out the same byte for byte (`2fadc5ea…`), because the upload key signs the bundle alone. *Seen
on the key machine:* `ENCRI_UPLOAD_KEYSTORE` and `ENCRI_UPLOAD_KEY_ALIAS`, exported for the one-off kit, were still
set at the next signing, which opened encri's keystore and refused the password typed for this app's. A name given on
the command line alone is safer than one exported, and the line `sign.sh` prints for the upload key names the alias it
is about to use.

*Measured, 2026-10-30,* the release itself: `6644860` built the same twice here (APK `7d447874…`, bundle `857967e6…`),
was pushed, and `reproducible.yml`, run by hand on `master` at that commit, built the same two files on GitHub's runner
with another JDK (Adoptium 21.0.13 to this machine's Debian 21.0.12.1) and another kernel; the kit of it, 21 MB,
rehearsed itself. The repository's `master` was already held by a ruleset, *Protect default branch*: no deletion, no
force push, signed commits, and no one who may bypass it. The first push replaced GitHub's *Initial commit*, a
LICENSE alone, so the ruleset was turned off for that one push at the user's word and turned back on unchanged.
