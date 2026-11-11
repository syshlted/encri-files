---
paths:
  - "app/src/**/transfer/Transfers*.kt"
  - "app/src/**/transfer/FolderWalk*.kt"
  - "app/src/**/transfer/FolderCopyTest.kt"
  - "app/src/**/transfer/Outside*.kt"
  - "app/src/**/transfer/ResolverOutside*.kt"
  - "app/src/**/transfer/ResolverFoldersTest.kt"
  - "app/src/**/ui/TransferWords*.kt"
  - "app/src/**/ui/ProgressWords*.kt"
  - "app/src/**/CopyScreenTest.kt"
  - "app/src/**/FakeOutside.kt"
  - "app/src/**/ForeignDocumentsTest.kt"
---

# Copies — component brief

Copying into the vault and out of it: `Transfers`, the one copy the process runs at a time; `FolderWalk`, which walks
a folder before anything of it is copied in; `Outside` and `ResolverOutside`, the phone's storage as the system's
picker hands it over; and the bar at the foot of the screen (`TransferWords`, `ProgressWords`). The service a copy runs
behind is `transfer-service`'s; the confirmation before a copy out is in `Outgoing` (`handoffs-and-sharing`). This
brief is level 1; the full records are in `docs/decisions/`.

## Rules that hold here
- **What moves, and how.** In: any number of files of any type from `ACTION_OPEN_DOCUMENT`, or one folder from
  `ACTION_OPEN_DOCUMENT_TREE`, into the folder the user is looking at. Out: one document, from its long press, to a
  place from `ACTION_CREATE_DOCUMENT`. All through the session's ordinary requests (encri-api L11), so they work with
  encri's file-picker access off. (F6, F11)
- **A file is copied in as `(unfinished) <name>`, written, and only then renamed to its own name**: encri's own rule
  for a share, with the same marker (encri D118). encri saves a write only when the writer finishes (encri-api L11),
  so a copy that is stopped is abandoned by throwing out of the writer, never by returning early, and what it made is
  deleted. One that cannot be deleted (the vault locked under it) keeps the marker, and the report counts it. (F6)
- **A taken name is refused**, checked against a fresh listing before anything is written and by encri again at the
  rename, where a name taken in between removes the copy. Two picked files of one name: the second is refused. (F6, F5)
- **A copy in that will not fit is refused before anything is written**, when the sizes the files' apps give add up
  to more than encri says it can take (encri D118). A file with no size is not counted; encri's `NO_SPACE` still stops
  the copy. (F6)
- **A file keeps the date its app gives it, set after the rename**, because encri's rename stamps the time. Only the
  last change is kept: a picker says nothing of when a file was made. (F6)
- **A folder is walked whole before anything is made**, so the copy knows how many files there are, refuses at once
  for room, and refuses a taken name before a byte is written. The bar says *Looking through the folder…*, and Stop ends
  the walk at the next folder with nothing made. (F11)
- **What cannot be listed whole is said, never skipped.** A folder whose app refuses to list it, dies part way through
  the rows, reports `EXTRA_ERROR` or gives a row with no document id is reported by its path; so is one still
  `EXTRA_LOADING` after half a minute of asking again (`LOADING_ASKS`). (F11)
- **A folder is named only when whole.** It is made as `(unfinished) <name>`, taking a free name, filled, and renamed
  only once every file in it has arrived. Not all of it: it keeps the marker with what did. None of it: it is removed
  again. Cannot be removed: it is said to be left. Its own name taken by the end: what arrived stays under the marker.
  Each file inside still goes through its own marker and rename. Folders keep their app's dates, set last. (F11)
- **Inside a folder**, a second item of one name is refused and reported by its path; a folder that cannot be made is
  reported once, not file by file; once a copy has had to stop, every file it did not reach is counted, in folders
  not yet made too. (F11)
- **A folder met again among its own parents is not entered, nor one more than 64 below the one chosen**
  (`MAX_DEPTH`), so a loop cannot fill the vault. (F11)
- **A copy out opens its place asking for truncation and settling for less** (`wt`, then `w`; encri D78), and after
  the close asks its length again for three seconds before a short answer is believed (encri B102). One that fails
  leaves the file the picker made and says so, never deletes it: the picker may have handed back a file that was
  there before (encri D78). (F6)
- **Another app's files are reached only through an unstable client** (`ResolverOutside`, encri B100), so a cloud app
  that dies mid-copy is a failed copy and not the death of this app, its session and every handoff. (F6)
- **One copy at a time, for the process, in a bar at the foot**: how far it has got, with Stop, then how it ended,
  until the user presses OK. Names only while the vault is open and unlocked; an ending over a locked vault is counted
  and promises the names after an unlock (F2). A copy in that brought anything says the originals are still where they
  were, unencrypted. (F6, F11)
- **A folder's report** names the folder and how many of how many files arrived, each folder and file not copied by
  its path (ten, then a count), and where what arrived is. A folder's own ending is said of the folder, never counted
  as files. (F11)
- **64 KiB at a time**, which no FUSE request an AppFuse bridge carries can fail on (encri D88). (F6)
- **While a copy runs, back at the top sends the app to the background** rather than closing it, for when the
  platform refuses the service (F8): a picked file is readable while the receiving task is alive. (F6)
- **No persistable grant, and nothing staged on the phone's storage.** (F6, F11)

## Traps already paid for
- A copied file showed *just now* for a file from that morning: the date had been set before the rename, which stamps
  the time. (F6)
- Without `FLAG_GRANT_PREFIX_URI_PERMISSION` on the service's start intent, the files in a folder's own folders,
  read after the task was gone, were not handed over (F11, `transfer-service`).
- A cloud app answers a listing at once with what it has and fetches the rest, so its first answer is not all there
  is. (F11)
- From API 30 the picker refuses the storage's root, the Download folder itself, `Android/data` and `Android/obb`; a
  folder inside any of them can be chosen. On the A20 the picker's list takes no taps and its arrow keys stay on
  *Create new folder*; Tab moves into the list. (F11)
- Rejected: copying as the walk goes; merging into a folder of the same name; a partial folder taking its own name;
  a persistable grant taken and released; `EXTRA_INITIAL_URI`. (F11)

## Measured, not assumed
- F6: 217 runs, 94.8% of lines. A stop that returned instead of throwing fails two tests; back at the top closing the
  app during a copy fails one. **On the A20, against encri:** three files picked, two copied and the third refused in
  words for a taken name, a file from March 2025 keeping its date; a 400 MB file in and back out with the original's
  SHA-256; Stop a second in left nothing and said so. (F6)
- F11: 358 runs, 96.1% of lines; eight lines reverted one at a time each fail their tests. **On the A20:** a folder of
  five files (1 GB the largest), three folders inside (one empty) and a March 2025 date, copied with the task removed
  four seconds in, arrived whole and dated. (F11)
- The minified release build on the API 34 emulator copied a 5 MiB file in with its date and out again with the same
  SHA-256. (F9)
- **Not measured:** back at the top during a copy, on a phone (F6, recalled from the documentation); a folder copy's
  bytes against the originals; a cloud app's folder; a loop or a depth over 64 on a real app; a phone above API 30
  for a folder; the S9, the G6 and the S24. (F6, F11)

## Decisions
| | Why it matters here |
|---|---|
| F6 | Files in and a document out: marker, taken names, room, dates, the copy out, one at a time, the bar |
| F11 | A folder in: walked first, said not skipped, named only when whole, loop and depth guards, its report |
| F8 | The service that holds the grants, and what remains when it is refused (`transfer-service`) |
| F5 | A taken name refused, never changed |
| F2 | Names only over an unlocked vault |
| encri D118 | The `(unfinished)` marker; refusing a copy that will not fit |
| encri D78, encri B102 | `wt` then `w`; a failed copy out kept; a short length believed only after three seconds |
| encri D123 | What a copy out says first (`Outgoing`) |
| encri B100 | Another app's provider through an unstable client |
| encri D88 | 64 KiB at a time |
| encri-api L11 | Ordinary requests; a write saved only when the writer finishes |

## Open here
- F6's flag: **a copy out is confirmed every time**, with no way to stop being asked.
- F11's flags: **a folder not all of which arrived keeps `(unfinished)`**; **a folder whose name is taken is
  refused**, not merged nor copied beside it; **a copy stops at 64 folders deep**, and waits half a minute for a
  folder its app says is still loading.
- **Not built:** a folder out, several documents out at once, choosing what happens to a taken name, several folders
  in one choice, and files and folders in one choice. (F6, F11)

## Read next
- Related briefs: `.claude/rules/transfer-service.md`, `.claude/rules/handoffs-and-sharing.md`,
  `.claude/rules/vault-changes.md`
- Full records: `docs/decisions/INDEX.md`
