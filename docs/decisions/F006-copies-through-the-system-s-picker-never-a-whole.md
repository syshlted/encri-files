# F6 — Copies: through the system's picker, never a whole-looking file that is not, one at a time
*2026-10-23 · accepted; **flagged for veto on two points** (below)*

**Copying in** takes any number of files of any type from the system's picker (`ACTION_OPEN_DOCUMENT`) into the
folder the user is looking at. **Copying out** takes one document from its long press to a place the user picks
(`ACTION_CREATE_DOCUMENT`). Both go through the session's ordinary requests (encri-api L11), so they work with
encri's file-picker access off. Nine choices are this app's.

**A file is copied in as `(unfinished) <name>`, written, and only then renamed to its own name**, which is encri's
own rule for a share (encri D118), with the same marker so a user meets one convention. encri saves a write only
when the writer finishes (encri-api L11), so a copy that is stopped is abandoned by throwing out of the writer,
never by returning early, and what it had made is deleted. One that cannot be deleted — the vault locked under it
— keeps the name that says what it is, and the report counts it.

**A name already in the folder is refused**, F5's rule: checked against a fresh listing before anything is written,
and by encri again at the rename, where a name taken in between removes the copy. Two picked files of one name:
the second is refused.

**A copy in that will not fit is refused before anything is written**, when the sizes the files' apps give add up
to more than encri says it can take (encri D118's rule). A file whose app gives no size is not counted, and encri's
own `NO_SPACE` still stops the copy if it runs out.

**A file keeps the date its app gives it**, set after the rename, because encri's rename stamps the time it happened
(its `Catalog.rename`) — which the first device run found by showing *just now* for a file from that morning. Only
the last change is kept: a picker says nothing of when a file was made.

**Before anything leaves, a copy out says what it costs**: the copy is not encrypted, the app keeping the place
chosen can read it and so can whatever it backs up to, and deleting the document later does not delete the copy
(encri D123). Every time — there is no *don't ask again*. Then the place is opened asking for truncation and
settling for less (`wt`, then `w`, encri D78), and after the close its length is asked again for three seconds
before a short answer is believed (encri B102). A copy out that fails leaves the file the picker made, empty or
part-written, and says so rather than deleting it, for encri D78's reason: the picker may have handed back a file
that was there before.

**Another app's files are reached through an unstable client**, as encri reaches them (its B100), so a cloud app
that dies mid-copy is a copy that failed rather than the death of this app, its session and every handoff another
app is reading. `ForeignDocumentsTest` holds every source to it.

**One copy at a time, for the process, in a bar at the foot of the screen**: how far it has got, with Stop, and
then how it ended, until the user presses OK. Names appear in the bar only while the vault is open and unlocked
(F2): an ending over a locked vault is counted, and promises the names after an unlock. Each file refused on its own
is named with why; the ones a stop or a lock never reached are counted once, with the reason. A copy in that brought
anything says the originals are still where they were, unencrypted, because a user may believe the file is now
protected.

**While a copy runs, back at the top sends the app to the background instead of closing it.** A file a picker hands
over is readable while the task of the activity that received it is alive (Android's own `FileProvider`
documentation: *"Permissions granted in an Intent remain in effect while the stack of the receiving Activity is
active"*), so closing the task mid-copy could leave the rest unreadable. From API 31 back at the top does that anyway;
below it, this keeps it so. Recalled from the documentation, ~~and not measured~~ and seen at API 33 on Waydroid
(LineageOS 20), 2026-11-12, with no copy running: back at the top, which this app leaves to the platform then, left
Encri Files' activity in its task, stopped and not finished (F2). During a copy it is not measured.

**64 KiB at a time**, which no FUSE request an AppFuse bridge carries can fail on (encri D88), whichever side another
app's provider is on.

*Rejected:* taking a persistable grant on each picked file so the copy survives its task — it writes the files'
URIs into the system's grant record until released, where keeping the task alive needs no record at all.
*Rejected:* a free name for a taken one (F5). *Rejected:* deleting a part-written copy out (encri D78).
*Rejected:* staging a copy anywhere on the phone's storage — nothing of the vault is written outside it.

**Not built:** copying a folder ~~in or~~ out (*a folder in since F11*), copying several documents out at once, and
choosing what happens to a taken name. Each needs a choice of its own, and none is needed to move one file either way.

**Verified.** Under Robolectric and coroutine tests, against stand-ins for encri and for the apps the picker reaches,
and against a real `ContentProvider` for the resolver: a file arriving whole with its date; several in turn; a taken
name refused before and during a copy; no room; a file that will not open, that stops part way, or has no name; a
full vault ending the copy; a stop keeping only what finished; a copy that cannot be removed counted; and a copy out
whole, refused by encri, cut off by encri, refused by its place at the open, the write and the close, stopped, and
believed only once its length agrees. 217 runs, 94.8% of lines. A stop that returned from the writer instead of
throwing fails two of them; back at the top closing the app during a copy fails one. **On the Galaxy A20 (API 30),
against encri:** three files chosen with the picker's *Select all*, two copied, the third refused in words for a taken
name, and a file from March 2025 keeping its date; a 400 MB file copied in and back out, the SHA-256 of the copy out
the same as the original's; and Stop pressed a second into a copy left nothing in the folder and said so.
**Not run on a phone:** back at the top during a copy, and the S9, the G6 and the S24.

**Flagged for veto:**

1. ~~**No foreground service.** A copy runs in the app's process, so Android may pause it (from API 34) or stop it
   while another app is in front; the bar says to keep the app open, and a copy that is stopped leaves its
   unfinished name. A service would need `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` and a notification, the
   first permissions this app would declare.~~ **Answered by the user the same day, who asked for one: F8.**
2. **A copy out is confirmed every time**, with no way to stop being asked.

<!-- amendments: generated by scripts/docs/decision-index.py; do not edit -->

## Amended since, as recorded in `amendments.md`

- **F6's back at the top from API 31, recalled and not measured, was seen at API 33 on Waydroid (2026-11-12)**, with
  no copy running, when this app leaves back at the top to the platform: the activity stayed in its task, stopped and
  not finished (F2). Back at the top during a copy is still not measured. Struck through in place.
- **F6's copying a folder in, which it left unbuilt, is built by F11 (2026-11-03).** *Copy a folder in* takes one
  folder from the system's picker and copies it, the folders in it and all their files, into the folder the user is
  looking at. Copying a folder out, several documents out at once, and choosing what happens to a taken name are still
  not built.
- **F6's first flag, no foreground service, is answered by F8 (2026-10-23)**, at the user's direction: a copy runs
  behind a `dataSync` foreground service that holds the picker's grants, so it carries on while another app is in
  front. F6's *back at the top keeps the app* stays, for when the platform refuses the service.

<!-- /amendments -->
