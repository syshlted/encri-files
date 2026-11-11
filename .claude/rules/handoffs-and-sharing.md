---
paths:
  - "app/src/**/ui/Handoffs*.kt"
  - "app/src/**/ui/Outgoing*.kt"
  - "app/src/**/ShareScreenTest.kt"
---

# Handoffs and sharing — component brief

How a document reaches another app without leaving the vault: `Handoffs`, which opens a document with another app
(*open with*) or hands it to the share sheet, each through a handoff encri makes for it (encri D117, D118); and
`Outgoing`, the confirmations a document's long press shows before a share and before a copy out. The copy out itself
is `copies`'. This brief is level 1; the full records are in `docs/decisions/`.

## Rules that hold here
- **Opening a document is a handoff and `ACTION_VIEW`**, with `FLAG_GRANT_READ_URI_PERMISSION`, through the
  platform's own resolver rather than a chooser, so a user can set a default app for a type. A tap opens; it asks
  nothing first. (F2)
- **The URI names neither the document nor its id** (encri D117), and nothing this app puts in the Intent names it:
  the clip's label is empty. (F2, F7)
- **A handoff lasts until the vault locks or the session ends** (encri D117). This app never revokes one: revoking it
  when the share sheet closes, which nothing reports, would break an app that reads when it sends rather than when it
  is chosen. (F7)
- **A share is said first, every time**: the app chosen gets what is in the document, not encrypted, and can keep a
  copy that deleting the document does not delete; and it reads from the vault, so if it waits to read until after the
  vault locks or this app is closed, it finds nothing (encri D117, D118). No *don't ask again*. (F7)
- **A share is a handoff sent with `ACTION_SEND` through `Intent.createChooser`**, the URI in the stream extra and in
  the clip, with the read grant, so the grant reaches whichever app is picked (encri D118). For a document; a folder
  offers no share. (F7)
- **encri is left out of the share sheet.** Its share screen refuses a URI on its own authorities (encri D118), so a
  vault document offered back to it is a choice certain to fail. Its components are found by asking the package
  manager which of `Encri.PACKAGE`'s activities would take the share, so this holds whatever encri names them, and
  nothing else is left out. To put a document in another folder, a move does it (F5). (F7)
- **What Android keeps of a share is not repeated here**: the share sheet records the app chosen and the type in the
  phone's usage history (encri D118). (F7)
- **A copy out is confirmed the same way before the picker opens**: the copy is not encrypted, the app keeping the
  place can read it and so can whatever it backs up to, and deleting the document later does not delete the copy
  (encri D123). (F6)
- **A refused handoff opens nothing**, and says so in a toast that names nothing (F7, F5).
- **The name the receiving app reads is encri's to give.** Where an app asks encri's handoff provider for it (the
  share sheet does, for its preview), an encri that answers contract version 1, encri 0.1.0 among them, gives the
  document's own name; one that answers 2, which encri built on 2026-11-11 (encri D147), gives a made-up name, twelve
  hex digits and an extension from the type, unless the handoff was asked for by `handOffNamed` (encri-api L24). This
  app's pin, `c27f139`, speaks version 1 and never asks for the name. (F7, F9)

## Traps already paid for
- The share sheet offered encri itself, and choosing it on the A20 answered *There is nothing here encri can save*.
  Hence the exclusion. (F7)
- A decrypted copy shared through a `FileProvider` of this app's own writes the document to the phone's storage. (F7,
  rejected)
- Revoking a handoff when the share sheet closes breaks an app that reads when it sends. (F7, rejected)

## Measured, not assumed
- Under Robolectric at API 27 and 36: a document offers a share first and a folder none; nothing is handed over
  before the confirmation; the share sheet carries a handoff in the stream and the clip with the grant; encri's
  component is left out and another app's is not; a refused handoff opens nothing. 231 runs, 94.9% of lines. (F7)
- **On the A20, against encri:** the share sheet named the document, read through the handoff; Nextcloud uploaded it
  to the test server, whose copy has the original's SHA-256; once encri was left out, the sheet offered everything it
  had but encri. (F7) HTML Viewer, another app, read a note's name and content through an *open with* handoff. (F2)
- The minified release build on the API 34 emulator shared a document (encri not among the targets) and opened one in
  HTML Viewer. (F9)
- **Not measured:** a share or an *open with* against an encri that has built D147.

## Decisions
| | Why it matters here |
|---|---|
| F7 | Sharing: said first, a handoff through the chooser, encri left out, nothing repeated of Android's record |
| F2 | *Open with*: a handoff and `ACTION_VIEW` through the resolver; a remedy the user presses |
| F6 | The copy out's confirmation, in `Outgoing` beside the share's |
| F5 | No toast names a document; a move, not a share back to encri |
| F9 | The encri-api pin, `c27f139`, which speaks contract version 1 |
| encri D117 | The handoff provider; the URI names nothing; the handoff ends at a lock or the session's end |
| encri D118 | The file manager's share; what it must say; encri's share screen refuses its own URIs |
| encri D123 | What a copy out must say |
| encri D147, encri-api L24 | A made-up name for a handoff unless `handOffNamed` asks; contract version 2 |
| encri D148, encri-api L25 | The *Send its name* tick in the dialog before a handoff; no list of apps |

## Open here
- F7's flag: **every share is confirmed**, with no way to stop being asked.
- **The *Send its name* tick is owed** (encri D148, encri-api L25; `ROADMAP.md`, *Now*): in the dialog shown before a
  share, unticked every time and never remembered, which asks `EncriSession.handOff(ids, withNames = true)`. The
  dialog's words say the app chosen next sees the name. An encri older than version 2 answers `STATUS_UNSUPPORTED`,
  and the app says so rather than sending the document under a name the user did not choose (encri-api L24). A tap
  that opens a document asks nothing, so it sends no name; whether a named *open with* is offered from the long press
  is this app's to decide in a record of its own (encri D148). Waits for the pin to move to a commit with L24 (encri-api
  `a414543`, pushed 2026-11-11).
- **Several documents at once**, which needs a selection the list does not have yet, **and a folder**, are not built.
  (F7)
- **Whether handoffs should be revoked sooner** than encri ends them (`docs/decisions/open-questions.md`).

## Read next
- Related briefs: `.claude/rules/vault-and-browsing.md`, `.claude/rules/copies.md`
- Full records: `docs/decisions/INDEX.md`; the handoff itself is encri's (D117, D118, D147, D148) and encri-api's
  (L24, L25)
