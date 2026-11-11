---
paths:
  - "app/src/**/transfer/TransferService*.kt"
  - "app/src/**/transfer/TransferNotice*.kt"
  - "app/src/**/ui/ProgressWords*.kt"
  - "app/src/main/AndroidManifest.xml"
---

# Transfer service — component brief

The foreground service a copy runs behind, `TransferService`, and its notifications, `TransferNotice` and
`SystemTransferNotice`; `ProgressWords`, which the bar and the notification share; and the three permissions the
manifest declares for them. The copy itself is `copies`'. This brief is level 1; the full records are in
`docs/decisions/`.

## Rules that hold here
- **The service runs no copy.** `Transfers` does, in the application (F6). The service keeps the process in the
  foreground for as long as a copy runs, so Android neither pauses it (from API 34) nor stops it for being in the
  background, and it holds the picker's grants. Nothing about how a copy is made depends on it. (F8)
- **It follows `Transfers.state`.** Started when a copy starts, it goes to the foreground at once; when the copy ends
  it stops itself with the latest start's id, which is the one `stopSelf(int)` has to name. Not exported, binds to
  nothing, and `START_NOT_STICKY`, because a restarted service would have no copy to hold. (F8)
- **The grants ride in its start intent**, as `ClipData` with the grant flags: read for a copy in, and read and write
  for a copy out, whose length is read back after the close (encri B102). Handed to a service, a grant lasts until the
  service stops for that start (`Service`'s documentation; encri D118 measured it), so a copy goes on after its screen
  is closed or swiped away from the recent apps. (F8)
- **A folder's grant goes as a prefix.** For a tree URI the start intent also carries
  `FLAG_GRANT_PREFIX_URI_PERMISSION`; without it the service holds the folder's own URI and nothing under it. (F11)
- **Three permissions, the first this app declares, for one capability.** `FOREGROUND_SERVICE` (API 28) and
  `FOREGROUND_SERVICE_DATA_SYNC` (API 34) are install-time, and an app targeting 36 needs both: the second is a
  declaration of its own, not a successor (encri D78 read the same). `POST_NOTIFICATIONS`, a runtime permission from
  API 33, is asked for when a copy starts, after the picker has closed, and again at the next copy until answered.
  Refused, the copy still carries on; only its notification is not drawn. (F8)
- **Notifications count, and never name.** A notification is read off a locked phone by whoever holds it (F2). The
  ongoing one says which file of how many and how much, with a progress bar and *Stop*, which reaches the copy at its
  next block. The ending is a count: *Copied 3 files into the vault*, *Copied a folder with 37 files into the vault*,
  with *Open Encri Files to see what happened* when something was not whole. (F8, F11)
- **At most once a second**, because Android drops a package's notifications beyond a few a second and a fast copy
  reports a mebibyte at a time. (F8)
- **Private on the lock screen, in one channel, *Copies*, at low importance**, so a copy makes no sound and silencing
  it silences nothing else. Tapping either notification opens the browsing screen, whose bar has the rest. (F8)
- **The ending is not said while a screen of this app is in front**, because the bar already says it, and the one said
  away from the screen is taken back when a screen comes to the front. (F8)
- **A refusal is said, not hidden.** From API 31 Android refuses a foreground service to an app in the background. If
  it does, the copy runs as it did before (F6), and the bar says to keep the app open rather than that it can be left;
  it tells which from `Transfer.Running.held`. (F8)
- **From API 35 a `dataSync` service has six hours a day**, and `onTimeout` stops the copy at its next block. At the
  rates measured, that is a terabyte or more. (F8)

## Traps already paid for
- From API 31 Android holds a new foreground service's notification back for up to ten seconds, so a short copy
  flashes nothing; `setForegroundServiceBehavior` to show it at once was rejected. (F8)
- Rejected: a user-initiated data transfer job (API 34 against a floor of 27, and a network constraint a copy does not
  have); WorkManager (AndroidX, F1, and a foreground service underneath); `shortService` (three minutes, which a large
  copy outlasts); a service that runs the copy itself. (F8)
- The control for the prefix flag lost the three files read after the task was gone: *Copied 2 of 5 files from a
  folder into the vault*. The provider's refusal went back over binder and was not in the log. (F11)

## Measured, not assumed
- Under Robolectric at API 27 and 36: the service in the foreground with a notification that counts and names
  nothing; the ending said away from the screen and not in front of it; a copy already ended; the latest start's id; a
  stop and a spent budget reaching the copy; the grants each direction needs; *Stop* as a service intent; once a
  second; nothing posted with notifications refused; the question asked from API 33 and not below. 285 runs, 95.3% of
  lines; eight mutations each fail the test written for them, at both levels. (F8)
- **On the A20 (API 30), against encri:** a 2 GB file copied in with the app sent Home straight after, counting up to
  the end, the service gone once it said *Copied 1 file into the vault*; the same document copied out with the task
  removed two seconds in (`am stack remove`), carrying on to the end with the original's SHA-256; *Stop* about 700 MB
  in left nothing and said *Nothing was copied into the vault*. (F8) The prefix grant measured with and without its
  flag. (F11)
- The minified release build on the API 34 emulator asked for notifications when the first copy started, API 33's
  question, which had never run on a device. (F9)
- **Not run on a phone:** API 33's notification question, API 34's type permission and API 35's budget, which the S24
  would take; and the G6 and the S9. (F8)

## Decisions
| | Why it matters here |
|---|---|
| F8 | The `dataSync` service: runs no copy, holds the grants, three permissions, counting notifications |
| F11 | The prefix flag for a folder's grant; a folder's ending as a count |
| F6 | The copy the service holds, and *back at the top keeps the app* for when the service is refused |
| F2 | No name where a locked phone shows it |
| F1 | No AndroidX, so no WorkManager |
| encri D118 | A grant handed to a service lasts until it stops for that start, measured by encri |
| encri D78 | Both foreground-service permissions, read the same way |
| encri B102 | A copy out's length read back after the close, so its grant carries write |

## Open here
- F8's flags: **notifications asked for when a copy starts**, rather than never or at first launch; **no ending said
  while a screen of this app is in front.**
- API 34's and 35's halves, and API 33's question on a phone, are unmeasured. (F8)

## Read next
- Related briefs: `.claude/rules/copies.md`, `.claude/rules/vault-and-browsing.md` (the manifest)
- Full records: `docs/decisions/INDEX.md`
