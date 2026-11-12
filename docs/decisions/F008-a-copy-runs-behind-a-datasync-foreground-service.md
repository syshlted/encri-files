# F8 — A copy runs behind a `dataSync` foreground service, which holds the picker's grants and runs no copy
*2026-10-23 · accepted, at the user's direction ("add a foreground service to the files app so the user isn't stuck
on the app screen while running long operations"); answers F6's first flag; **flagged for veto on two points** (below)*

**`TransferService` keeps the process in the foreground for as long as a copy runs, and runs no copy itself.**
`Transfers` still does, in the application (F6). So nothing about how a copy is made changes: the service is what
stops Android pausing the process (from API 34) or stopping it for being in the background, and it is what holds the
picker's grants. It follows `Transfers.state`: started when a copy starts, it goes to the foreground at once, and when
the copy ends it stops itself with the latest start's id, which is the one `stopSelf(int)` has to name. Not exported,
binds to nothing, and `START_NOT_STICKY`, because a restarted service would have no copy to hold.

**The grants ride in its start intent.** A file the picker handed over is readable while the task of the screen that
received it is alive. Handed to a service as `ClipData` with the grant flags, the grant lasts until the service stops
for that start, which `Service`'s documentation says and encri measured for its own share screen (encri D118): read
for a copy in, and read and write for a copy out, whose length is read back after the close (encri B102). So a copy
goes on after its screen is closed or swiped away from the recent apps. F6's *back at the top keeps the app* stays,
for the case below where the platform refuses the service.

**The first permissions this app declares, three for one capability.** `FOREGROUND_SERVICE` (API 28) and
`FOREGROUND_SERVICE_DATA_SYNC` (API 34) are both install-time and both needed by an app targeting 36, the second a
declaration of its own and not a successor (encri D78 read the same). `POST_NOTIFICATIONS` is a runtime permission
from API 33, asked for when a copy starts — after the picker has closed and the copy is under way, so the question
does not hold the copy up — and again at the next copy until answered, which Android stops showing after a second
refusal. Refused, the copy still carries on; only its notification is not drawn.

**What the notifications say: counts, never a name.** A notification is read off a locked phone by whoever holds it,
and the names of what is in the vault are what F2 keeps off a locked screen. The ongoing one says which file of how
many and how much, with a progress bar and *Stop*; *Stop* reaches the copy at its next block, as the bar's does.
It is told at most once a second, because Android drops a package's notifications beyond a few a second and a fast
copy reports a mebibyte at a time. The ending is said as a count — *Copied 3 files into the vault*, *A document was
not copied out of the vault* — with *Open Encri Files to see what happened* when something was not whole. Both are
private on the lock screen, in one channel, *Copies*, at low importance, so a copy makes no sound and silencing it
silences nothing else. Tapping either opens the browsing screen, whose bar has the rest.

**The ending is not said while a screen of this app is in front**, because the bar already says it, and the
notification that said it away from the screen is taken back when a screen comes to the front.

**A refusal is said, not hidden.** From API 31 Android refuses a foreground service to an app in the background. A
screen choosing files is never in the background, so this is not expected to happen; if it does, the copy runs as it
did before (F6), and the bar says to keep the app open rather than that it can be left. The bar tells which from
`Transfer.Running.held`.

**From API 35 a `dataSync` service has six hours a day**, and the platform's `onTimeout` stops the copy at its next
block. At the rates measured below that is a terabyte or more.

*Rejected:* a user-initiated data transfer job (`JobInfo.Builder.setUserInitiated`, API 34), which Android's
documentation points to for transfers a user starts and which may one day replace `dataSync`. It needs API 34 against
a floor of 27, and a network constraint, which a copy between this phone's storage and the vault does not have.
*Rejected:* WorkManager, which is AndroidX (F1) and, for this, a foreground service underneath. *Rejected:*
`shortService` (API 34), whose three minutes a large copy outlasts. *Rejected:* a service that runs the copy itself,
which would move the copy engine for nothing the service needs. *Rejected:* `setForegroundServiceBehavior` to show the
notification at once: from API 31 Android holds a new foreground service's notification back for up to ten seconds,
so a short copy flashes nothing.

**Verified.** Under Robolectric at API 27 and 36: the service goes to the foreground with a notification that counts
and names nothing, says how a copy ended away from the screen and not in front of it, goes to the foreground and stops
at once for a copy already ended, stops with the latest start's id, takes a stop and a spent budget to the copy, and
is handed the grants each direction needs; the notice counts, offers *Stop* as a service intent, is told at most once
a second, words each ending without a name, and posts nothing with notifications refused; the screen starts the
service, asks for notifications from API 33 and not below, and takes back the ending. 285 runs, 95.3% of lines. Eight
mutations each fail the test written for them, at both levels. **On the Galaxy A20 (API 30), against encri:** a 2 GB
file copied in with the app sent to the Home screen straight after, the notification counting up to the end and the
service gone once it said *Copied 1 file into the vault*, which opening the app took back; the same document copied
out with the app's task removed two seconds in (`am stack remove`, which is what swiping it from the recent apps does),
carrying on in the same process to the end, its SHA-256 the same as the original's; and *Stop* in the notification
about 700 MB into a copy left nothing in the vault and said
*Nothing was copied into the vault*. **Not run on a phone:** ~~the API 33+ notification question,~~ API 34's type
permission and API 35's budget, which the S24 would take; and the G6 and the S9. **The API 33+ notification question
ran on Waydroid (LineageOS 20, API 33), 2026-11-12**, a ROM at API 33 and not a handset. *Allow Encri Files to send you
notifications?* was asked as the copy started, after the picker had closed. Refused with *Don't allow*, the copy, of
32 MiB, finished with nothing posted, the permission refused and set by the user; the next copy asked again, and was
allowed. A third, of 192 MiB (201 MB), posted *Copying 1 of 1 into the vault…* with *0 B of 201 MB*, then *92 MB of
201 MB* with its bar at 458 of 1000, naming no file, and the notification was gone once the copy ended.

**Flagged for veto:**

1. **Notifications asked for when a copy starts**, rather than never — the copy runs either way — or at first launch.
2. **No ending said while a screen of this app is in front.**

<!-- amendments: generated by scripts/docs/decision-index.py; do not edit -->

## Amended since, as recorded in `amendments.md`

- **F8's notification question ran at API 33, on Waydroid (2026-11-12).** LineageOS 20, a ROM at API 33 and not a
  handset: asked as a copy started, after the picker had closed; refused, the copy finished with nothing posted, and
  the next copy asked again; allowed, a copy's notification counted the bytes and named no file. API 34's type
  permission and API 35's budget are still the S24's, and the G6 and the S9 still not run. Struck through in place.

<!-- /amendments -->
