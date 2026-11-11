# F5 — Changes: every right asked for, a name taken refused, and a move made by going there
*2026-10-23 · accepted; **flagged for veto on two points** (below). **Supersedes F2** on the rights asked for.*

The app makes a folder, and renames, moves and deletes documents and folders. Each is one request to encri,
which checks the right and the name itself and refuses what it will not do (encri D120); nothing here decides
whether a change is allowed. Six choices are this app's.

**It asks for every right, because it uses every one**: reading, renaming — which encri counts as writing —
making folders, deleting and moving (`Asked.RIGHTS`). F2 asked to read alone while reading was all it did. A
grant may hold fewer, one made for an earlier version or one the user narrowed, and then the changes it lacks
are not offered and the menu offers *Allow more in encri*, which starts encri's consent screen for all five.
encri ends the old session when it replaces a grant, and the app opens a new one at once (F2).

**A name already taken is refused, never changed into another.** encri can pick a free name instead
(encri-api's `NameConflict.PICK_FREE_NAME`); this app never asks it to, so a file is never silently given a name
the user did not choose. The user is told something with that name is already there, and decides.

**A move is made by going to the folder**: *Move* puts a bar at the foot of the screen naming what is moving,
the user browses to where it goes, and *Move here* moves it. The button is off in the folder it is already in
and inside a folder being moved into itself; encri's own cycle check is still the authority, and its refusal is
said like any other. *Rejected:* a folder picker of its own, which would be a second browser to keep in step
with the first.

**A delete is confirmed by name, and says what it cannot undo**: that a folder goes with everything in it, that
nothing here brings it back, and that a backup encri made before may still hold a copy (encri D95 keeps eight
generations).

**What encri says is put in the user's terms** (`Outcomes`), per change: *invalid* means a name that cannot be
used for a rename, and a folder moved into itself for a move. encri's own message is for a developer and is
never shown.

**Every dialog follows the screenshot setting (F3)**, because a dialog is a window of its own and shows a name,
and **no toast names a document**: a toast is drawn by the system, outside this app's windows, where the setting
cannot reach. The name field is focused with the keyboard up and the name before its extension selected, so a
rename is typing the new name.

**Verified.** Under Robolectric at API 27 and 36 against a stand-in with a tree that changes: each change, a
blank name refused before encri is asked, a rename to the same name asking nothing, a delete confirmed first, a
move gone through and given up, a refusal in the user's terms, a read-only session offering nothing but *Allow
more*, and dialogs under the screenshot setting. Removing the dialogs' screenshot setting, or the check that a
folder is not moved into itself, each fails its tests. 95% of lines. **On the A20**, against encri: the read-only
grant offered *Allow more in encri*, whose consent screen listed all five rights; once allowed, a folder was
made, the note renamed by typing over its selected name, moved into the new folder from a bar at the foot, a
second folder with a taken name refused in words, and the new folder deleted with the note inside it.

**Flagged for veto:**

1. **A taken name is refused**, where many file managers offer to keep both.
2. **A delete cannot be undone here**: there is no bin.
