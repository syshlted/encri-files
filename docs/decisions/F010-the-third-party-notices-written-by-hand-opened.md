# F10 — The third-party notices: written by hand, opened from the settings, and held to the build
*2026-10-29 · accepted; owed before any APK leaves this machine, the internal testing track included (F9), for encri
D138's reason. Nothing new is flagged: each choice is encri D138's, which the user confirmed, or F3's.*

**What is owed.** Six modules ship (`app/gradle.lockfile`), besides encri-api's own. The Apache License asks for its
text to go with the app and for any NOTICE file to be reproduced, and kotlinx.coroutines has one
(`license/NOTICE.txt` at `1.10.2`). The Kotlin standard library carries code from ThreeTen-Backport, whose 3-clause
BSD licence asks for its notice in a binary distribution, and from Boost (encri D138 read Kotlin's
`license/README.md` at `v2.4.21`, the version that ships here too). None of the six jars holds a licence file, so
without a screen the APK carries none of it.

**Written by hand, as encri's are.** `app/src/main/assets/third-party-notices.txt` takes encri's entry for the Kotlin
standard library and its Apache License text word for word, the same versions having been read for it; the Apache
text was compared again, word for word, with kotlinx.coroutines' `LICENSE.txt`. The other two were read at the
versions that ship: JetBrains' annotations at `23.0.0`, whose licence's appendix gives *Copyright 2000-2016 JetBrains
s.r.o.* (encri ships 13.0, which says 2000-2012), and kotlinx.coroutines at `1.10.2`, with its NOTICE, whose border
of `=` is the one change, compared word for word. One paragraph to a line, as encri D138 has it, and `NoticeText` is
encri's, drawing `# ` and `## ` as headings.

**Held to the build by `:app:checkThirdPartyNotices`**, encri's gate without its list of notices owed, since this
app owes none: every module a variant's runtime classpath resolves to is named in the asset by a line holding only
`group:artifact:version`, and the asset names nothing else. By version, so a bump fails until the new release has
been read. encri-api's modules are this project's own. It runs in `check`, the pre-push hook and `ci.yml`.

**Opened from the settings**, as a row below the two switches, because the menu offers *Settings* in every state and a
licence's terms do not depend on there being a vault. The row is drawn in the switches' size and colour and is as tall
as a touch target: walking the release build through showed a plain `TextView` there in the explanations' grey, which
read as one more explanation rather than something to press. The screen follows the screenshot setting as every window
here does (F3), where encri's notices are always secure: here the rule is the setting, not every window. Not exported;
the text is selectable.

*Verified:* under Robolectric at API 27 and 36, the screen shows the whole asset as `noticeText` draws it, to the end
of the Apache License and with kotlinx.coroutines' NOTICE; it blocks screenshots until they are allowed; no other
app can open it; up leaves it; and the settings row opens it, drawn as the switches are and not as the explanations,
which fails with the colour taken out. The gate passes on both variants, which carry the same six modules, and fails
three ways: a module's line removed, a version moved (both lines named), and a module named that does not ship.
