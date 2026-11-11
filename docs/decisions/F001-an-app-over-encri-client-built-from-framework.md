# F1 — An app over encri-client, built from framework views, with encri's toolchain
*2026-10-23 · accepted; **flagged for veto on one point** (below)*

**A separate application, `com.systemhalted.encri.files`**, signed with encri's key (encri D112, D115),
that reaches the vault through `com.systemhalted.encri:encri-client` and nothing else. One module,
`:app`. The application id is permanent from the first build a user installs, because encri keys a grant
on it (encri D120).

**The launcher name is "Encri Files", for now**, at the user's direction on 2026-10-23. It is a string
resource and can change in any release without touching anything a user holds. The visible name was left
open at encri's rename (encri D124), and a trademark search for encri is owed before anything ships.

**Framework views, built in code, and no AndroidX.** encri's own screens are built the same way (encri
D33 turned down `androidx.biometric` partly for the appcompat and fragment it would pull in), and here it
buys three things. What a user receives is the client, its coroutines and the Kotlin standard library —
`app/gradle.lockfile` lists exactly that. An auditor reading this app reads Android's API and nothing in
between. And the common image libraries keep a disk cache by default, which encri D112 forbids for
previews; with none on the classpath there is nothing to configure wrongly. A `ListView` with a framework
row layout is enough for a folder.

*Rejected:* AndroidX (RecyclerView, AppCompat, Lifecycle) — more to ship and to audit, for a screen a
`ListView` serves. *Rejected:* Compose — a large runtime, in an app whose purpose includes being an
example others lift code from.

**The toolchain is encri-api's, version for version**: Gradle 9.7.1, AGP 9.4.1 with Kotlin 2.4.21 on
the root classpath (encri D119), ktlint 1.8.0, detekt 1.23.8, Kover 0.9.9, JUnit 5.14.4 and Robolectric
4.17; the same daemon pin, `.editorconfig`, detekt deltas, hooks and pinned action SHAs. minSdk 27,
because encri cannot run below it (encri D14), and targetSdk 36.

**encri-api is an included build until it is published**, beside this checkout or wherever
`encri.api.dir` points, as encri builds it. The app names the client by the coordinates it will have on
Maven Central, so publishing changes one line of the version catalog.

**One coverage gate, on the whole module, with nothing excluded: 80% of lines**, plus 85% of changed
lines in a pull request (encri D5's rule). The screens are kept thin and run under Robolectric at API 27
and 36 against a stand-in for encri, so the number says what was tested rather than what was easy to
test. encri exempts its own `:app`; this repository is an app whose screens are its subject, so it does
not. Line rather than branch, as encri's `:provider` gate is.

**The dependencies are pinned twice**: `gradle/verification-metadata.xml` by sha256 (encri D7) and
`app/gradle.lockfile` for what ships, in STRICT mode (encri-api L10).

**Flagged for veto:**

1. **No AndroidX**, so every screen is framework views written by hand, and predictive back, insets and
   results use the platform's APIs directly.
