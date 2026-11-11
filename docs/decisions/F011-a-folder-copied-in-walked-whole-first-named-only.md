# F11 — A folder copied in: walked whole first, named only when whole, and its grant handed on as a prefix
*2026-11-03 · accepted, at the user's direction ("allow encri-files to import directories, not just files");
**flagged for veto on three points** (below)*

**Copy a folder in**, beside *Copy files in*, takes one folder from the system's picker (`ACTION_OPEN_DOCUMENT_TREE`)
and copies it, the folders in it and all their files into the folder the user is looking at, through the session's
ordinary requests (encri-api L11) as F6's copies are. Nothing in encri or encri-api changed: `VaultSession.createFolder`
gained the choice of a free name the client's own already had. Nine choices are this app's.

**The folder is walked whole before anything is made.** Every folder in it is listed first, so the copy knows how many
files there are, refuses at once when the vault has no room for them (F6's rule), and refuses a name already taken
before a byte is written. Meanwhile the bar says *Looking through the folder…*, and Stop ends the walk at the next
folder with nothing made.

**What cannot be listed whole is said, never skipped**, which is the folder's form of encri's rule that a failed read
is never a short one. A folder whose app refuses to list it, dies part way through the rows, reports an error beside
them (`EXTRA_ERROR`) or gives a row no document id is reported by its path, and so is one its app still says it is
loading (`EXTRA_LOADING`) after half a minute of asking again: a cloud app answers at once with what it has and fetches
the rest, so its first answer is not all there is.

**Named only when whole.** The folder is made as `(unfinished) <name>`, F6's marker, taking a free name so a copy left
unfinished before does not stand in the way; filled; and renamed to its own name only once every file in it has
arrived. One that not everything reached keeps the marker with what did, one that nothing reached is removed again, and
one that cannot be removed (the vault locked under it) is said to be left. Unlike a file (F6), a folder whose own name
is taken by the time it is whole keeps what arrived under the marker rather than being thrown away, because it holds
copies that are whole. Each file inside still goes through F6's own marker and rename, so a folder renamed by hand never
holds a file that reads as whole and is not. Folders keep the dates their app gives, set last, and the folder chosen
after its rename, which encri stamps.

**A name taken inside is refused, as F5 and F6 refuse one.** Two files, or a file and a folder, of one name in one
folder, which a cloud app can hold: the second is reported by its path. A folder inside that cannot be made is reported
once, not file by file. Once a copy has had to stop (stopped, locked, out of room), every file it did not reach is
counted, as F6 counts them, in folders not yet made too.

**A folder inside itself is not entered again, nor one more than 64 below the one chosen.** The first is a document id
met again among a folder's own parents; the second guards an app whose ids are paths, where a link back up makes each
turn a new id, as a terminal app's home folder can. Either would otherwise copy the same files over and over until
the vault was full. Neither has been seen.

**The grant goes to the service as a prefix.** F8 hands the picker's grants to `TransferService` in its start intent. A
folder's grant reaches what is in it only as a prefix of its tree URI, which is how the picker gave it, so the start
intent carries `FLAG_GRANT_PREFIX_URI_PERMISSION` for a tree; without it the service holds the folder's own URI and
nothing under it. **Measured both ways** on the A20 (below).

**No persistable grant is taken**, although the system's confirmation says *"This will let Encri Files access current
and future content stored in <folder>"*. The access ends with the copy's service and the task, and nothing is written
to the system's record of kept grants: F6's reason, for a folder.

**Names only over an unlocked vault, counts only in the notification.** The bar says *Copied “Trip” into the vault,
with 37 files*, or how many of how many, each folder and file not copied by its path (ten, then a count), and where
what arrived is. Over a locked vault neither the folder nor anything in it is named (F2), and the notification never
names either (F8): *Copied a folder with 37 files into the vault*. A folder's own ending (a name taken, the vault
locked) is said of the folder, never counted as files.

*Rejected:* copying as the walk goes, which starts sooner but can neither refuse for room nor give a total until it has
made a partial folder. *Rejected:* merging into a folder of the same name already there, which is the choice of what
happens to a taken name that F6 put off, made for every file inside at once. *Rejected:* a folder that did not all
arrive taking its own name with the missing files listed in the bar, which a user who presses OK without reading would
take for a whole copy. *Rejected:* taking the folder's persistable grant for the copy and releasing it after, which
writes the system's record and leaves it written if the process dies; the prefix needs no record. *Rejected:*
`EXTRA_INITIAL_URI`, since nothing on this screen knows where the user's folder is.

**Not built:** copying a folder out, several folders in one choice (the picker hands back one), and files and folders
in one choice.

**From API 30 the picker refuses** the root of the phone's storage, the Download folder itself, `Android/data` and
`Android/obb`. A folder inside any of them can be chosen.

**Verified.** Under Robolectric and coroutine tests, against stand-ins for encri and for the app keeping the folder, and
against a real `ContentProvider` serving a tree for the resolver: a folder arriving whole, nested and dated, made
unfinished and named last; empty; beside an earlier unfinished copy; still loading and then listed; refused for a taken
name, no room, a folder that will not list, no name, a vault folder that cannot be listed, encri refusing the folder,
and a stop during the walk; and left not whole for a folder inside that will not list, one still loading after the
asks, one inside itself, one 65 deep, two names alike, a folder named as a file beside it, a folder encri will not make,
a nameless folder, a file that will not be read, a stop part way and before anything arrived, a vault locking under it,
and its own name taken by the end. 358 runs, 96.1% of lines. Eight lines reverted one at a time each fail the tests that
guard them: the loading asks, the loop guard, the depth guard, naming only when whole, the prefix flag, a loading
listing and a row with no id in the resolver, and a folder's own ending said of the folder.

**On the Galaxy A20 (API 30), against encri:** a folder of a 1 GB file, a 5 MB one, three small ones, three folders
inside (one empty) and a file dated March 2025, copied with the app's task removed (`am stack remove`) four seconds in:
*Copied a folder with 5 files into the vault*, every folder and file there at its size, the empty folder too, and the
March 2025 date kept. **The control**, the same build without the prefix flag and the same removal: *Copied 2 of 5 files
from a folder into the vault*, the three files inside its folders, read after the task was gone, named as not handed
over, and the rest left in `(unfinished) F11 trip`. The refusal itself was not in the log: the provider's exception goes
back to the app over binder. **Not compared:** the copies' bytes with the originals', which F6 did by SHA-256 for the
same write path. **Not run:** a cloud app's folder (a real `EXTRA_LOADING`), a loop or a depth over 64 on a real app,
a phone above API 30, the S9, the G6 and the S24.

*Seen on the A20:* the picker's list takes no taps, and its arrow keys stay on *Create new folder* under *Can't use this
folder*; Tab moves into the list, and the picker opens next time where it was last used.

**Flagged for veto:**

1. **A folder that did not arrive whole keeps `(unfinished)` in its name** until the user renames it, even when one file
   of thousands is what is missing.
2. **A folder whose name is taken where it is going is refused**, not merged into nor copied beside under a free name.
3. **A copy stops at 64 folders deep**, and waits half a minute for a folder its app says it is still loading.
