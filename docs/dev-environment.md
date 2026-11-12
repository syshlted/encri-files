# encri-files — development environment

> Moved out of `AGENTS.md` on 2026-11-11 (F12). The first section below is unchanged; it was the *Local development
> environment* section of that file. encri's notes, which it defers to, are `../encri-core/docs/dev-environment.md`.

## Local development environment

The same machine as encri and encri-api, and encri's notes on its JDKs, phones, emulators and Waydroid
apply. Two facts matter here:

- **Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`**, and never run two Gradle invocations at once:
  the included encri-api build is one more set of outputs for them to fight over.
- **encri-api must be checked out beside this one**, at `../encri-api`, or pointed at with
  `-Pencri.api.dir=<path>`. Before a push it must be at the commit `config/encri-api.pin` names, which the
  pre-push hook checks and CI fetches there (F9).

## The A20 and Waydroid, as this app uses them

Added 2026-11-12, after a round on both (`CHANGELOG.md`). encri's notes have the rest of each, how to reach Waydroid
among it.

- **Waydroid is a ROM at API 33, not a handset.** What runs there is recorded as Waydroid's (LineageOS 20, API 33), and
  a handset above API 30 is still the S24. Since 2026-11-11 it has encri, this app and a test vault, which has a device
  slot as well as a passphrase because the tablet has a screen lock (encri D35, D74). Its picker takes taps.
- **The A20's vault was made again on 2026-11-12**, passphrase only. Both test vaults' passphrases are on file outside
  every repository.
- **On the A20 the system's picker takes taps in its drawer and on *Save*, not in its file list.** A file is picked from
  the keyboard: Tab to the view-mode button above the list, Down onto the file, then Enter (F11 has the rest).
- **The A20's keyguard is a swipe, and it comes back**: dismiss it (`adb shell wm dismiss-keyguard`) before driving a
  screen.
