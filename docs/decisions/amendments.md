# encri-files — amendments to earlier decisions

Each entry corrects a record. Its bold lead names the record possessively ("F2's …") so that
`scripts/docs/decision-index.py` copies it into that record's footer. Newest first.

## Amendments

- **F2's back callback ran at API 33, on Waydroid (2026-11-12).** LineageOS 20, a ROM at API 33 and not a handset:
  inside a folder back went up to the top, and at the top it left Encri Files, whose activity stayed in its task,
  stopped and not finished. The G6, the S9 and the S24 are still not run. Struck through in place.
- **F6's back at the top from API 31, recalled and not measured, was seen at API 33 on Waydroid (2026-11-12)**, with
  no copy running, when this app leaves back at the top to the platform: the activity stayed in its task, stopped and
  not finished (F2). Back at the top during a copy is still not measured. Struck through in place.
- **F8's notification question ran at API 33, on Waydroid (2026-11-12).** LineageOS 20, a ROM at API 33 and not a
  handset: asked as a copy started, after the picker had closed; refused, the copy finished with nothing posted, and
  the next copy asked again; allowed, a copy's notification counted the bytes and named no file. API 34's type
  permission and API 35's budget are still the S24's, and the G6 and the S9 still not run. Struck through in place.
- **The repository conventions' "This one is the decision log" and "this file wins" are moved by F12
  (2026-11-11).** The decision log is `docs/decisions/`, one record to a file, and it wins over `ROADMAP.md`,
  `CHANGELOG.md` and the component briefs in `.claude/rules/`. `AGENTS.md` is the always-loaded map. Struck through
  in place in `docs/conventions.md`.
- **F6's copying a folder in, which it left unbuilt, is built by F11 (2026-11-03).** *Copy a folder in* takes one
  folder from the system's picker and copies it, the folders in it and all their files, into the folder the user is
  looking at. Copying a folder out, several documents out at once, and choosing what happens to a taken name are still
  not built.
- **F6's first flag, no foreground service, is answered by F8 (2026-10-23)**, at the user's direction: a copy runs
  behind a `dataSync` foreground service that holds the picker's grants, so it carries on while another app is in
  front. F6's *back at the top keeps the app* stays, for when the platform refuses the service.
- **F2's `FLAG_SECURE` on the window is a setting since F3 (2026-10-23)**, at the user's direction: *Allow
  screenshots*, off by default, so screenshots stay blocked unless the user allows them. F2's second flag is answered.
- **F2's reading alone is superseded by F5 (2026-10-23).** The app asks for every right, because it uses every one,
  and a grant that holds fewer offers *Allow more in encri*. F2's first flag is moot.
