# F3 — Screenshots are a setting, blocked until the user allows them
*2026-10-23 · accepted, at the user's direction ("a toggle to allow or reject screenshots"); **flagged for veto
on one point** (below)*

**A switch in a settings screen of this app's own**, *Allow screenshots*, off by default. Off, every window this
app draws carries `FLAG_SECURE`: Android blocks screenshots and screen recordings, keeps the app off a screen it
is cast to, and shows a blank card among the recent apps. On, the flag is cleared and all of that is allowed.
The screen says what turning it on costs — a screenshot is kept in the photos, outside the vault, where photo
backups and other apps reach it — and that it changes this app alone: encri's own screens always block
screenshots (encri D8), and this app cannot change that, nor should it.

**Off by default**, which keeps F2's position for everyone who never opens the settings: the screen shows what
is in the vault, and D8's reason for the flag is encri's whole stance. The user who wants a screenshot of their
own folder turns it on, knowing what it costs.

**Applied when a window is made and again whenever a screen comes to the front**, so a change made in settings
reaches the browsing screen without a restart, and the first frame — the one the app switcher keeps — is
already right. Every dialog this app shows will follow it too: a dialog is a window of its own, and the
Activity's flag does not reach it (`followScreenshotSetting`).

**Kept in the app's private preferences**, a single yes or no. The app is out of Android's backups
(`allowBackup` is false), so the choice stays on the phone.

*Rejected:* allowed by default, which would put the vault's names in every app-switcher picture for a user who
never chose that. *Rejected:* a setting per screen, which is more to explain than it is worth.

**Verified.** Under Robolectric at API 27 and 36: blocked by default, allowed once chosen, applied on return from
settings — removing that re-apply fails its test at both levels — and the switch shows what was chosen. **On the
A20:** `screencap` came back empty, then captured the folder list once the switch was on, then came back empty
again once it was off.

**Flagged for veto:**

1. **Blocked by default.**
