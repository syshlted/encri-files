# F7 — Sharing: a handoff through the share sheet, said first, and never back to encri
*2026-10-23 · accepted; **flagged for veto on one point** (below)*

A document's long press offers *Share* first, for a document and not a folder; reading is all it needs. It is encri
D118's sharing out, which that decision gave to the file manager. Four choices are this app's.

**Before the share sheet opens, the screen says what a share costs**: the app chosen gets what is in the document,
not encrypted, and can keep a copy that deleting the document does not delete; and it reads from the vault, so if it
waits to read until after the vault locks or this app is closed, it finds nothing (encri D117's lock rule, which encri
D118 said the file manager's share must say). Every time, as a copy out is (F6).

**A handoff, sent with `ACTION_SEND` through `Intent.createChooser`**, the URI in the stream extra and in the clip,
with the read grant, so the grant reaches whichever app is picked (encri D118). The clip's label is empty, so the
document's name goes only where an app asks encri's handoff provider for it — the share sheet does, to draw its
preview, as encri D118 recorded.

**encri is left out of the share sheet.** Its share screen refuses a URI on its own authorities (encri D118), so a
vault document offered back to it is a choice certain to fail — found on the A20, where choosing it answered *There is
nothing here encri can save*. Its components are excluded by asking the package manager which of encri's
(`Encri.PACKAGE`) activities would take the share, so this holds whatever encri names them, and nothing else is left
out. To put a document in another folder of the vault, a move does it (F5).

**What Android keeps of a share is not repeated here.** The share sheet records the app chosen and the type in the
phone's usage history (encri D118, which says so on encri's privacy screen); the user is choosing an app on that screen,
which is the act the record describes.

*Rejected:* a decrypted copy shared through a `FileProvider` of this app's own, which writes the document to the
phone's storage. *Rejected:* revoking the handoff once the share sheet closes, which nothing reports and which would
break an app that reads when it sends rather than when it is chosen. *Not built:* several documents at once, which
needs a selection the list does not have yet, and a folder.

**Verified.** Under Robolectric at API 27 and 36: a document offers a share first and a folder none; nothing is handed
over before the confirmation; the share sheet carries a handoff in the stream and the clip with the grant; encri's
component is left out and another app's is not; a refused handoff opens nothing. 231 runs, 94.9% of lines. **On the A20,
against encri:** the share sheet named the document, read through the handoff; Nextcloud uploaded it to the test
server, whose copy has the original's SHA-256; and once encri was left out, the sheet offered everything it had but
encri.

**Flagged for veto:**

1. **Every share is confirmed**, with no way to stop being asked.
