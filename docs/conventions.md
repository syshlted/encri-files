# encri-files — conventions

> Moved out of `AGENTS.md` on 2026-11-11 (F12). The text below is unchanged but for one amendment, struck
> through in place; it was the *Repo conventions* section of that file. `AGENTS.md` keeps a summary.

## Repo conventions

**Three files, and they do not overlap**, as in encri and encri-api. ~~This one is the decision log.~~
**The decision log is `docs/decisions/`, one record to a file (amended by F12).**
`ROADMAP.md` is what is next and in what order. `CHANGELOG.md` is what happened. When they disagree,
~~**this file wins**~~ **the decision records win (amended by F12)**.

**Default branch is `master`**, a SystemHalted organization-wide policy. Do not rename it to `main`.

**Commits are signed and Conventional**, with the repo-local config and the lefthook hooks encri-api
records under its *Repo conventions*: `signing-preflight`, `ktlint` and `detekt` before a commit,
`conventional-commit` on the message, and `verify-signatures` and `allUnitTests` before a push. Fresh
clones bootstrap with `lefthook install`. Every rule a hook enforces is enforced again in
`.github/workflows/`, because `--no-verify` always exists.

**Every commit builds and passes its own tests, not just the branch tip** — encri's rule, for encri's
reason: `verify-signatures` walks every commit in a push and `unit-tests` runs only at the tip.

**Merge with a merge commit or a rebase, never a squash** (encri D7).

**Regenerating the dependency verification metadata**, after adding or bumping anything, from an empty
file over every task a workflow runs (encri D7, and encri-api's note on why the file must start empty):

```sh
rm gradle/verification-metadata.xml
./gradlew --write-verification-metadata sha256 --refresh-dependencies --rerun-tasks \
  ktlintCheck detekt allUnitTests koverVerify koverXmlReport assemble assembleDebugAndroidTest bundleRelease
```

The file covers encri-api's build as well, because a composite build is verified by the root build's
file. Generated on 2026-10-23: 375 components, and every artifact it shares with encri's and encri-api's
files has the same hash. The one component neither has is `kotlinx-coroutines-android`.

**Changing what a user receives** means updating the lockfile, which STRICT mode demands:
`./gradlew assembleRelease --write-locks`.
