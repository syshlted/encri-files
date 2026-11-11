---
paths:
  - "scripts/release/**"
  - "config/release-certificates"
  - ".github/workflows/reproducible.yml"
  - "app/proguard-rules.pro"
  - "app/build.gradle.kts"
  - "app/gradle.lockfile"
  - "app/src/main/assets/**"
  - "app/src/**/NoticesActivity*.kt"
  - "app/src/**/ui/NoticeText*.kt"
  - "FINGERPRINTS.md"
  - "LICENSE"
---

# Release and signing — component brief

How a release is built, checked, signed and published: `scripts/release/` (`reproduce.sh`, `compare-builds.py`,
`kit.sh`, `sign.sh`, `bundletool.init.gradle.kts`), `.github/workflows/reproducible.yml`,
`config/release-certificates`, `FINGERPRINTS.md`, the minified build's R8 rules; and the third-party notices a binary
distribution owes, which are the asset, `NoticesActivity`, `NoticeText` and the `:app:checkThirdPartyNotices` gate.
The pin and the wrapper check are `build-and-ci`'s. This brief is level 1; the full records are in `docs/decisions/`.

## Rules that hold here
- **A release is built twice from one commit and must come out the same**: by GitHub's CI from the tag and by the
  maintainer (encri D116, kept for this app by encri D136). `reproducible.yml` runs `reproduce.sh` on every `v*` tag,
  weekly and by hand, and keeps what it built for 90 days. The maintainer's run must give the same SHA-256s before the
  kit is made. (F9)
- **`reproduce.sh <commit>` builds in two environments that differ on purpose** (path, umask, timezone, locale, Gradle
  home), sharing the pinned Gradle and every pinned dependency, with encri-api fetched at the tag's pin beside each
  clone. It keeps the unsigned APK and bundle as `build/reproducible/<commit>/encri-files-<commit>-unsigned.*` with
  their `SHA256SUMS` and a provenance naming both commits, and refuses a commit that predates the pin, the wrapper's
  checksum or the notices. (F9)
- **Signed from a kit of this repository's own, on the key machine** (encri D141). `kit.sh` packs the reproduced build
  with apksigner and bundletool's jars, each pinned, and rehearses itself before it is kept. `sign.sh` takes the
  directory holding the keystores, asks for each password at a terminal, signs the APK with the app signing key and
  the bundle with code transparency and then the upload key, holds both to the build, and keeps nothing signed by a key
  `config/release-certificates` does not name. `sign.sh --verify`, here, checks what comes back before the upload. (F9)
- **The keystores**: `encri-app-signing.p12` (`app-signing`) and `encri-transparency.p12` (`transparency`), encri's
  two keys (encri D140), and `encri-files-upload.p12` (`encri-files-upload`), this app's own upload key,
  `59:70:D4:B7…`. `ENCRI_UPLOAD_KEYSTORE` and `ENCRI_UPLOAD_KEY_ALIAS` name another; give them on the command line,
  never exported. (F9)
- **`reproduce.sh`, `compare-builds.py`, `kit.sh` and `sign.sh` are encri's, in two repositories**: a fix to one is
  owed to the other, as encri's B138 is in both. They are here because this repository is the one a stranger can
  read, and they let anyone rebuild a tag and hold the published APK to it (encri D115). (F9)
- **The minified build is walked through on a device before it is signed** (encri D140): R8's usual failure, a class
  reached by reflection, would reach every tester at first launch. (F9)
- **On Play, the listing's automatic protection is off** (encri D144), because the bundle carries code transparency.
  **The signed APK goes to a GitHub release** (encri D142), not before the trademark search encri D136 asks for. (F9)
- **No native library ships**, so there are no debug symbols to keep; `dependenciesInfo.includeInApk` is false, as it
  was from F1 (encri D116). (F9)
- **`FINGERPRINTS.md` lists the app signing and code transparency certificates**, the same as encri-api's (encri-api
  L22), inside the GPG-signed tag. Upload keys prove nothing to a user and are not in it. (F9)
- **The third-party notices are owed before any APK leaves this machine**, the internal testing track included (encri
  D138). Six modules ship (`app/gradle.lockfile`), besides encri-api's own, and none of their jars holds a licence
  file. (F10)
- **Written by hand** in `app/src/main/assets/third-party-notices.txt`: encri's entry for the Kotlin standard library
  and its Apache License text, word for word; JetBrains' annotations at `23.0.0`; kotlinx.coroutines at `1.10.2` with
  its NOTICE. One paragraph to a line; `NoticeText`, encri's, draws `# ` and `## ` as headings. (F10)
- **Held to the build by `:app:checkThirdPartyNotices`**: every module a variant's runtime classpath resolves to is
  named by a line holding only `group:artifact:version`, and nothing else is named. By version, so a bump fails until
  the new release's licence has been read. encri-api's modules are this project's own. It runs in `check`, the
  pre-push hook and `ci.yml`. (F10)
- **Opened from the settings**, a row below the two switches, drawn in the switches' size and colour and as tall as a
  touch target. The screen follows the screenshot setting (F3); not exported; the text is selectable. (F10)

## Traps already paid for
- **While an upload key reset is pending, Play takes no upload for this app at all**, by either key. It stopped taking
  encri's key when it approved the reset, and refused this app's own as "not yet valid" until 23:03:47 UTC on
  2026-10-30. (F9)
- An `ENCRI_UPLOAD_KEYSTORE` and `ENCRI_UPLOAD_KEY_ALIAS` exported for a one-off kit were still set at the next
  signing, which opened encri's keystore and refused the password typed for this app's. The line `sign.sh` prints for
  the upload key names the alias it is about to use. (F9)
- *Seen once and not reproduced:* a `signed/` holding the signed APK alone, when the terminal, `sign.sh` and
  bundletool were each sent SIGTERM at once. `--verify` refuses it for want of `SIGNED.txt`, and a second signing
  refuses to write over it. (F9)
- Walking the release build through found the notices row drawn as a plain `TextView` in the explanations' grey,
  which read as one more explanation. (F9, F10)
- `signed/` sat untracked, one `git add .` from being committed; it is gitignored now (F12).

## Measured, not assumed
- 2026-10-29: `27648e2` built the same twice (APK `568abcc2…`, bundle `cdb0983e…`) against encri-api `c27f139`.
  `a2ef986` (APK `079033c4…`, bundle `fa4750e7…`), signed with the debug key, was walked through on the API 34
  emulator against encri: nothing crashed, and logcat held no class, method or field R8 had removed. (F9)
- A kit of `27648e2` (21 MB) with three RSA-4096 scratch keystores: it rehearsed itself; a signing asked three
  questions; wrong keys failed with exit 1 and no `signed/`; Ctrl-C, a closed terminal, SIGTERM and a killed
  jarsigner each left no `signed/` and no process; the signed files were the build but for alignment padding (19
  entries) and signing's 4 bundle entries; `--verify` passed them with that kit and failed them with the real one. (F9)
- 2026-10-30: `6644860` built the same twice (APK `7d447874…`, bundle `857967e6…`), and `reproducible.yml` built the
  same two files on GitHub's runner, on another JDK (Adoptium 21.0.13 to Debian's 21.0.12.1) and another kernel. Two
  kits, signed with two upload keys, gave the same APK byte for byte (`2fadc5ea…`): the upload key signs the bundle
  alone. (F9)
- The notices gate passes on both variants, which carry the same six modules, and fails three ways: a module's line
  removed, a version moved, a module named that does not ship. (F10)

## Decisions
| | Why it matters here |
|---|---|
| F9 | Built twice from a tag, a kit of its own, the upload key and Play's reset, the walk-through |
| F10 | The notices: owed, written by hand, held to the build, opened from the settings |
| F1 | `dependenciesInfo.includeInApk = false`; the lockfile of what ships |
| F12 | `signed/` gitignored |
| encri D115, D116, D136 | One signing key; reproducible releases; one first release with encri |
| encri D138 | The notices, the gate, `NoticeText` |
| encri D140, D141 | Three keys and `sign.sh`; the kit on the key machine |
| encri D142, D144 | The APK on GitHub; Play's automatic protection off |
| encri D124 | A trademark search before anything is published beyond testers |
| encri B115, B138 | One Gradle in both builds; a fix owed to both copies of the scripts |
| encri-api L22 | The registry entry and `FINGERPRINTS.md` |

## Open here
- F9's flag: **a kit of this app's own rather than one kit for both apps**.
- **The first release is not uploaded** (`ROADMAP.md`, *Before the first release*): the kit signed with this app's
  upload key after the reset took effect, `sign.sh --verify` here, and the bundle uploaded to the internal track.
  `signed/` holds a signing made at 02:40 UTC on 2026-10-30, before the reset took effect; no later one is recorded.
- The roadmap still lists **the signed `v0.1.0` tag on `6644860`** as owed, but the tag is in the repository and on
  GitHub, signed, made 2026-10-30. Whether CI built it again is not recorded.
- Nightlies are encri D143's: GitHub pre-releases under a `.nightly` package and a key nothing trusts, after the first
  release.

## Read next
- Related briefs: `.claude/rules/build-and-ci.md`, `.claude/rules/settings-and-previews.md` (the notices row)
- Full records: `docs/decisions/INDEX.md`; encri's own release brief is `../encri-core/.claude/rules/release-and-signing.md`
