---
paths:
  - "app/src/**/Edits*.kt"
  - "app/src/**/Outcomes*.kt"
  - "app/src/**/link/Asked*.kt"
---

# Vault changes — component brief

The changes a user makes to the vault from this app: a new folder, a rename, a move and a delete, in `Edits`, which
also offers a document's share and copy out from its long press (`handoffs-and-sharing`, `copies`); the rights the
app asks for, in `Asked`; and what encri's refusals say to the user, in `Outcomes`. This brief is level 1; the full
records are in `docs/decisions/`.

## Rules that hold here
- **Each change is one request to encri, which checks the right and the name itself and refuses what it will not
  do** (encri D120). Nothing here decides whether a change is allowed: it offers what the session's rights cover and
  reports what encri says. (F5)
- **It asks for every right, because it uses every one** (`Asked.RIGHTS`): reading, renaming (which encri counts as
  writing), making folders, deleting and moving. A grant may hold fewer, one made for an earlier version or one the
  user narrowed; then the changes it lacks are not offered, and the menu offers *Allow more in encri*, which starts
  encri's consent screen for all five. (F5, superseding F2's reading alone)
- **Allowing replaces the grant with exactly the rights asked for, and ends the old session**, which the app opens
  again at once (F2). So it always asks for every right it means to keep. (F5, encri-api L23)
- **A name already taken is refused, never changed into another.** encri can pick a free name
  (`NameConflict.PICK_FREE_NAME`); this app never asks it to for a user's document. The user is told something with
  that name is already there, and decides. (F5)
- **A move is made by going to the folder.** *Move* puts a bar at the foot of the screen naming what is moving, the
  user browses to where it goes, and *Move here* moves it. The button is off in the folder it is already in and inside
  a folder being moved into itself; encri's own cycle check is still the authority, and its refusal is said like any
  other. (F5)
- **A delete is confirmed by name, and says what it cannot undo**: that a folder goes with everything in it, that
  nothing here brings it back, and that a backup encri made before may still hold a copy (encri D95). (F5)
- **What encri says is put in the user's terms** (`Outcomes`), per change: *invalid* means a name that cannot be used
  for a rename, and a folder moved into itself for a move. encri's own message is for a developer and is never shown.
  `Outcomes` is a pure function, so each answer has a test that needs no phone. (F5)
- **Every dialog follows the screenshot setting** (`showFollowing`), because a dialog is a window of its own and
  shows a name; **no toast names a document**. (F5, F3)
- **The name field is focused with the keyboard up and the name before its extension selected**, so a rename is
  typing the new name. A blank name is refused before encri is asked, and a rename to the same name asks nothing. (F5)

## Traps already paid for
- A folder picker of the app's own for a move would be a second browser to keep in step with the first. (F5,
  rejected)
- encri's rename stamps the time it happened (its `Catalog.rename`), so whatever renames a document also changes its
  date; a copy in sets its date after the rename for that reason (F6, `copies`).

## Measured, not assumed
- Under Robolectric at API 27 and 36, against a stand-in with a tree that changes: each change; a blank name refused
  before encri is asked; a rename to the same name asking nothing; a delete confirmed first; a move gone through and
  given up; a refusal in the user's terms; a read-only session offering nothing but *Allow more*; dialogs under the
  screenshot setting. Removing the dialogs' screenshot setting, or the check that a folder is not moved into itself,
  each fails its tests. 95% of lines. (F5)
- **On the A20, against encri:** the read-only grant offered *Allow more in encri*, whose consent screen listed all
  five rights; once allowed, a folder was made, the note renamed by typing over its selected name, moved into the new
  folder from the bar at the foot, a second folder with a taken name refused in words, and the new folder deleted with
  the note inside it. (F5)
- The minified release build on the API 34 emulator made, renamed and deleted a folder and moved a document into it.
  (F9)
- **The delete questions of `c12b8f6`, on the A20, 2026-11-12**, against encri (`CHANGELOG.md`, 2026-11-11, which
  checked them with the unit suite alone). A folder: *Delete “Albums”?* / *Everything in the folder is deleted with it.
  It is deleted from the vault, not put in a bin: this cannot be undone. A backup encri made before now may still hold
  a copy.* A document: *Delete “IMG_00001.jpg”?* / the same, without the first sentence. Both cancelled. (F5)

## Decisions
| | Why it matters here |
|---|---|
| F5 | Every right, a taken name refused, a move by going there, a delete said first, refusals in the user's terms |
| F2 | Superseded on the rights asked for; a session encri ends is asked for again |
| F3 | Every dialog under the screenshot setting |
| F6, F11 | The same refusal of a taken name for a copy in |
| encri D120 | encri checks the right and the name, and refuses |
| encri D95 | A backup encri made before may still hold a deleted document |
| encri-api L23 | Allowing replaces the grant with exactly the rights asked for, and ends the session |

## Open here
- F5's flags: **a taken name is refused**, where many file managers offer to keep both; **a delete cannot be undone
  here**: there is no bin.
- **Choosing what happens to a taken name** is not built (F6), nor choosing several documents at once (F7,
  `ROADMAP.md`, *Later*).

## Read next
- Related briefs: `.claude/rules/vault-and-browsing.md`, `.claude/rules/copies.md`,
  `.claude/rules/handoffs-and-sharing.md`
- Full records: `docs/decisions/INDEX.md`
