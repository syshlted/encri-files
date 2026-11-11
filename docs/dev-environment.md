# encri-files — development environment

> Moved out of `AGENTS.md` on 2026-11-11 (F12). The text below is unchanged; it was the *Local development
> environment* section of that file. encri's notes, which it defers to, are `../encri-core/docs/dev-environment.md`.

## Local development environment

The same machine as encri and encri-api, and encri's notes on its JDKs, phones, emulators and Waydroid
apply. Two facts matter here:

- **Set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`**, and never run two Gradle invocations at once:
  the included encri-api build is one more set of outputs for them to fight over.
- **encri-api must be checked out beside this one**, at `../encri-api`, or pointed at with
  `-Pencri.api.dir=<path>`. Before a push it must be at the commit `config/encri-api.pin` names, which the
  pre-push hook checks and CI fetches there (F9).
