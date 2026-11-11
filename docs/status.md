# encri-files — status history

> Moved out of `AGENTS.md` on 2026-11-11 (F12). The text below is unchanged; it was the *Status* and *Build
> order* sections of that file. What is next is in `ROADMAP.md`, and what happened in `CHANGELOG.md`.

## Status

**Scaffolded, and the first screen works (2026-10-23).** It connects to encri, asks the user to let it
in on encri's own screen, unlocks the vault on encri's unlock screen, lists the vault a folder at a time,
and opens a document with another app through a handoff (F2). Since then it has a settings screen
with a switch for screenshots (F3) and one for previews (F4), and it makes folders and renames, moves and
deletes what is in the vault (F5). It copies files in from the system's picker and copies a document out to a
place the user picks (F6), and shares a document through the share sheet (F7). A copy runs behind a foreground
service, so it carries on when the user leaves the app (F8). It shows the third-party notices from the settings
(F10). **Pushed to GitHub on 2026-10-30**, where CI built the release and agreed with the maintainer's build; its
first release, 0.1.0 for Google Play's internal testing track, is waiting to be signed (F9). Since 2026-11-03 it also
copies a folder in, with everything in it (F11).

## Build order

1. ~~Scaffold and the first screen.~~ **Done 2026-10-23** — F1, F2.
2. Everything after is in `ROADMAP.md`.
