---
paths:
  - "*.gradle.kts"
  - "app/build.gradle.kts"
  - "gradle.properties"
  - "gradle/**"
  - "app/gradle.lockfile"
  - "config/detekt/**"
  - "config/encri-api.pin"
  - "scripts/ci/**"
  - "scripts/hooks/**"
  - "lefthook.yml"
  - ".github/workflows/ci.yml"
  - ".github/workflows/commit-policy.yml"
---

# Build and CI — component brief

The Gradle build (`settings.gradle.kts`, the root and `:app` `build.gradle.kts`, `gradle/libs.versions.toml`,
`gradle/verification-metadata.xml`, `gradle/gradle-daemon-jvm.properties`, the wrapper), `config/detekt/`, the
lefthook hooks in `scripts/hooks/`, the CI scripts in `scripts/ci/` (`encri-api`, `gradle-wrapper-check`,
`diff-coverage.py`), the workflows `ci.yml` and `commit-policy.yml`, and the encri-api pin. The release's own
workflow and scripts are `release-and-signing`'s. This brief is level 1; the full records are in `docs/decisions/`.

## Rules that hold here
- **One module, `:app`**, which reaches the vault through `com.systemhalted.encri:encri-client` and nothing else.
  (F1)
- **The toolchain is encri-api's, version for version**: Gradle 9.7.1, AGP 9.4.1 with Kotlin 2.4.21 on the root
  classpath (encri D119), ktlint 1.8.0, detekt 1.23.8, Kover 0.9.9, JUnit 5.14.4 and Robolectric 4.17; the same daemon
  pin, `.editorconfig`, detekt deltas, hooks and pinned action SHAs. A bump is made in all three repositories or none.
  minSdk 27, because encri cannot run below it (encri D14); targetSdk 36. (F1)
- **encri-api is an included build until it is published**, at `../encri-api` or wherever `encri.api.dir` points.
  The app names the client by the coordinates it will have on Maven Central, so publishing changes one line of the
  version catalog. (F1)
- **Every build but a developer's is at the commit `config/encri-api.pin` names.** `scripts/ci/encri-api` is encri's
  script (encri B108, D126): `fetch` clones the pinned commit, and `check` says whether `../encri-api` is at it with
  nothing uncommitted. The pre-push hook runs `check`, so a push carries a pin the hook's tests ran against; CI fetches
  the pin into `../encri-api` and passes no `-Pencri.api.dir`. Bump the pin in the commit that needs the newer
  encri-api, and push encri-api first; `ENCRI_API_SOURCE` fetches from a local repository meanwhile. (F9)
- **The Gradle wrapper is held to what gradle.org publishes**: `distributionSha256Sum` in
  `gradle-wrapper.properties`, and `scripts/ci/gradle-wrapper-check`, encri's, holding the jar and the sum to
  gradle.org's checksums, first in `ci.yml`. A release's two builds share one distribution, so an altered one would
  run inside both and both would agree (encri B115). (F9)
- **One coverage gate on the whole module, with nothing excluded: 80% of lines** (Kover), plus 85% of changed lines in
  a pull request (`diff-coverage.py`, encri D5's rule). Line rather than branch. The screens run under Robolectric at
  API 27 and 36 against a stand-in for encri, so the number says what was tested, not what was easy to test. (F1)
- **The dependencies are pinned twice**: `gradle/verification-metadata.xml` by sha256 (encri D7), and
  `app/gradle.lockfile` for what ships, in STRICT mode (encri-api L10). The metadata is regenerated from an empty file
  over every task a workflow runs, and covers encri-api's build too, because a composite build is verified by the root
  build's file; the lockfile with `./gradlew assembleRelease --write-locks` (`docs/conventions.md`). (F1)
- **The hooks are a fast mirror of CI, never the only place a check exists**: signing preflight, ktlint and detekt
  before a commit; Conventional Commits on the message; signatures, the encri-api pin, and `allUnitTests` with
  `:app:checkThirdPartyNotices` before a push. The notices gate rides in the unit tests' invocation, because two Gradle
  runs at once fight over the included build's outputs. Every rule is enforced again in `.github/workflows/`. (F1, F9,
  F10)
- **`allUnitTests`, never a name-matched task**, which silently skips a module that lacks it (encri D11). Robolectric's
  JUnit 4 runs beside JUnit 5 under one task through the vintage engine. (F1)

## Traps already paid for
- CI checked out encri-api's moving `master`, so a tag built twice a week apart could be built against two different
  libraries, and nothing in the tree said which. Hence the pin. (F9)
- CI could not build at all until encri-api's rename was on GitHub (encri-api L19); pushed 2026-10-29 at `c27f139`.
  (F9, `ROADMAP.md`)
- The verification metadata must start from an empty file, as encri-api's note says (`docs/conventions.md`).

## Measured, not assumed
- The verification metadata, generated 2026-10-23: 375 components, and every artifact it shares with encri's and
  encri-api's files has the same hash. The one component neither has is `kotlinx-coroutines-android`.
  (`docs/conventions.md`)
- The suite (285 runs) and the minified release built against the pin with no change here; `fetch` from GitHub put a
  clone at the pin; `check` on a clone one commit back failed with exit 1. The wrapper check failed before the sum was
  added, naming it, and passes after. (F9)
- Coverage at the last record: 358 runs, 96.1% of lines. (F11)

## Decisions
| | Why it matters here |
|---|---|
| F1 | One module over encri-client, the toolchain, the included build, the coverage gate, two pins |
| F9 | The encri-api pin and its scripts, the wrapper's checksum |
| F10 | The notices gate in `check`, the pre-push hook and `ci.yml` |
| F12 | `scripts/docs/`, the docs layout, `.gitignore`'s `.claude/` and `signed/` |
| encri D5, D7, D11 | Diff coverage; sha256 verification and merge rules; the aggregate test task |
| encri D14, D119 | minSdk 27; Kotlin on the root classpath |
| encri D126, encri B108 | The pin, the fetch script and the pre-push check |
| encri B115 | Why the wrapper is checked against gradle.org |
| encri-api L10 | STRICT lockfile for what ships |

## Open here
- F1's flag: **no AndroidX**, so every screen is framework views written by hand.
- **The pin is at `c27f139`** (encri-api L22). Moving it to a commit with L24, for the *Send its name* tick, waits for
  encri-api to push `a414543` (`handoffs-and-sharing`).
- **Branch protection is partial**: the ruleset refuses a deletion, a force push and an unsigned commit; a review and
  required checks wait for a second maintainer (`ROADMAP.md`).
- **CodeQL and dependency scanning** are not here yet (encri-api L7, L10; `ROADMAP.md`).
- `commit-policy.yml`'s comment on squashing says *See AGENTS.md, Repo conventions*, which is now
  `docs/conventions.md` (F12). The build files' `AGENTS.md F1` and `AGENTS.md F9` mean the records (F12).

## Read next
- Related briefs: `.claude/rules/release-and-signing.md`
- Full records: `docs/decisions/INDEX.md`; the conventions in full: `docs/conventions.md`
