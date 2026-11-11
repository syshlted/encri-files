# F12 — The decision log leaves AGENTS.md, as encri's did (encri D127)
*2026-11-11 · accepted on the user's direction ("all of the AGENTS.md files … need to be evaluated to place the task maps, to-do lists, bugs, artifacts, etc. in their appropriate places"; "make sure the documentation … for all of the apps is written and formatted similarly"), and **flagged for veto on three points** (below).*

**What moved where.** `AGENTS.md` was 783 lines and 60,532 bytes: what the app is, its status, its relationship to
encri and encri-api, the conventions, the development environment, F1 to F11, the open questions and the build
order. It is laid out now as the suite's `docs/documentation.md` says, which is encri's layout (encri D127):

| What | Where |
|---|---|
| F1 to F11 | `docs/decisions/F001-*.md` to `F011-*.md`, one record to a file |
| *Status* and *Build order* | `docs/status.md` |
| *Repo conventions* | `docs/conventions.md` |
| *Local development environment* | `docs/dev-environment.md` |
| *Open questions* | `docs/decisions/open-questions.md` |
| *What this is* and *Relationship to encri and encri-api* | the new `AGENTS.md`, as its first two sections |

A record differs from its old text only in its heading level (`###` to `#`) and, below it, a footer that
`scripts/docs/decision-index.py` writes from `amendments.md`. Each moved section opens with a line saying where it
came from. `docs/decisions/INDEX.md` is generated.

**The move is lossless, and that was checked rather than assumed**, as encri D127 checked it. A script gave every line
of the old file one destination; a second rebuilt the old file from the written files alone and compared it byte for
byte. It came out identical. The only lines not carried are the old title block, the bare `## Decisions` heading and
the three `---` rules. Seven corrections made in place are listed in the check and reversed before the comparison:
the conventions' two sentences below; *What this is* pointing at encri's decision records and saying *this
repository* where it said *this file*; and in *Relationship*, the section's name, the five logs where there were three,
and *this repository's decision records win* where it said *this file wins*. Ten lines that are new follow the old
*Relationship* text.

**The sizes.** `AGENTS.md` is 186 lines and 13,426 bytes, loaded in every session. It holds what is true for every
component: the hard constraints, invariants and hazards, distilled from the records and citing them. Opening a file
adds the briefs whose globs match it, each between 5.5 and 9.5 KB; the eight are about 60 KB together.

**The components are features, as encri's are.** The app is one module, so a brief per module would be one brief.
Eight: `vault-and-browsing` (the session, the screen in front of the vault, browsing; F2), `settings-and-previews`
(F3, F4), `vault-changes` (F5), `handoffs-and-sharing` (*open with* and F7), `copies` (F6, F11),
`transfer-service` (F8), `build-and-ci` (F1, F9's pin and wrapper) and `release-and-signing` (F9, F10). Copies and the
service they run behind are two briefs because one would pass 12 KB. Globs overlap where a file serves two features:
the manifest loads `vault-and-browsing` and `transfer-service`. `scripts/docs/brief-coverage.py` finds every tracked
file covered, documentation and generated files excepted, and no glob that matches nothing.

**Amends the repository conventions** (`docs/conventions.md`), which named `AGENTS.md` as the decision log and said
it wins. Struck through in place: the decision log is `docs/decisions/`, and the decision records win.

**`signed/` is gitignored.** It holds a release's signed outputs, brought back from the key machine, and sat untracked
one `git add .` from being committed. `.claude/` is narrowed to `.claude/*` with an exception for `.claude/rules/`.

**`README.md`, `ROADMAP.md`, `CHANGELOG.md` and `FINGERPRINTS.md`** took the suite's titles, preambles and section
names, and point at `docs/decisions/` where they pointed at `AGENTS.md`. Their entries are unchanged, but for the
roadmap's items that later records had overtaken, struck through and followed by what is true now.

**Flagged for veto:**

1. **`.claude/rules/` is committed, in a public repository.** The briefs are project documentation that happens to
   load by path, as encri D127 decided for encri; a brief only on one machine would be lost with it. Here they are
   published with the source, so a stranger reads them too. They hold nothing the records do not.
2. **Code and older documents keep citing `AGENTS.md Fnn`.** 34 tracked files name `AGENTS.md`: 27 Kotlin files,
   `strings.xml`, two workflows, the version catalog, two scripts and `settings.gradle.kts`. `AGENTS.md` now says that
   `AGENTS.md F5` means `docs/decisions/F005-*.md`, and rewriting them would touch all of them to change a pointer.
   Two are not decisions: `commit-policy.yml` says *See AGENTS.md, Repo conventions*, which is now
   `docs/conventions.md`, and `scripts/hooks/commit-msg` gives an example message that names the file.
3. **Eight briefs, by feature, with globs that overlap.** Touching `AndroidManifest.xml` or `app/build.gradle.kts`
   loads two.

*Rejected:* keeping `AGENTS.md` as it was, at 60 KB in every session, when the suite's other projects are laid out
another way. *Rejected:* fewer briefs: copies and the service together, or the build with the release, pass 12 KB.

**Verified.** The lossless rebuild above. The index generator, run twice, changed nothing the second time. The brief
coverage check: eight briefs, every tracked file covered, no dead glob. Every brief is under 12 KB.

**What building it found, and did not fix.** Listed in the briefs' *Open here* and in `ROADMAP.md`: the roadmap still
lists the signed `v0.1.0` tag as owed, though it exists on `6644860`, made 2026-10-30; and *Before the first release*
still says "this week" of 2026-10-27.
