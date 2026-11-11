#!/usr/bin/env python3
"""Report which tracked files no component brief covers, and which globs match nothing.

A copy of encri's script (encri D127).

Run from the repository root:

    python3 scripts/docs/brief-coverage.py .

A brief in .claude/rules/ loads when a file matching one of its `paths:` globs is read or edited.
A source file no brief covers is one whose component knowledge never loads; a glob that matches
nothing is a brief that never loads. Both are reported; the exit status is 1 when either exists.
"""
import re
import subprocess
import sys
from pathlib import Path

# Files that are not any component's: documentation, generated or vendored artifacts.
UNOWNED = re.compile(
    r"^(AGENTS\.md|README\.md|ROADMAP\.md|CHANGELOG\.md|docs/|scripts/docs/|\.claude/|\.vscode/|\.gitignore$|\.editorconfig$"
    r"|gradlew(\.bat)?$|gradle/wrapper/|.*/src/test/resources/.*Inputs/|.*\.(png|webp|jpg)$)"
)


def glob_regex(pattern):
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif pattern.startswith("**", i):
            out.append(".*")
            i += 2
        elif pattern[i] == "*":
            out.append("[^/]*")
            i += 1
        elif pattern[i] == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(pattern[i]))
            i += 1
    return re.compile("^" + "".join(out) + "$")


def briefs(repo):
    found = {}
    for f in sorted((repo / ".claude" / "rules").glob("*.md")):
        text = f.read_text()
        m = re.match(r"^---\n(.*?)\n---\n", text, re.S)
        globs = re.findall(r'^\s*-\s*"([^"]+)"\s*$', m.group(1), re.M) if m else []
        found[f.stem] = globs
    return found


def main():
    repo = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    tracked = subprocess.run(
        ["git", "-C", str(repo), "ls-files", "--cached", "--others", "--exclude-standard"],
        check=True, capture_output=True, text=True,
    ).stdout.split()
    rules = briefs(repo)
    compiled = {name: [(g, glob_regex(g)) for g in globs] for name, globs in rules.items()}
    uncovered, used = [], set()
    for path in tracked:
        hits = [(n, g) for n, gs in compiled.items() for g, rx in gs if rx.match(path)]
        used.update(hits)
        if not hits and not UNOWNED.match(path):
            uncovered.append(path)
    dead = [(n, g) for n, gs in compiled.items() for g, _ in gs if (n, g) not in used]
    for name, globs in rules.items():
        if not globs:
            print(f"brief without paths (always loaded): {name}")
    for path in uncovered:
        print(f"uncovered: {path}")
    for name, g in dead:
        print(f"matches nothing: {name}: {g}")
    print(f"{len(rules)} briefs, {len(tracked)} files, {len(uncovered)} uncovered, {len(dead)} dead globs")
    sys.exit(1 if uncovered or dead or any(not g for g in rules.values()) else 0)


if __name__ == "__main__":
    main()
