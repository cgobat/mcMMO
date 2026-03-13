#!/usr/bin/env python

# usage: parse_changelog.py <version #>

import re
import sys
from pathlib import Path

WORKFLOWS_DIR = Path(__file__).resolve().parent
REPO_DIR = WORKFLOWS_DIR.parent.parent
CHANGELOG_PATH = REPO_DIR/"Changelog.txt"


def get_changes_for_version(version_num: str) -> str:
    with CHANGELOG_PATH.open("r") as changelog:
        version_matched = False
        version_lines = []
        for line in changelog:
            if re.fullmatch(f"Version {version_num.strip()}"+r"\s*", line):
                version_matched = True
                continue
            if version_matched:
                if line.strip() and not line.startswith(" "):
                    break
                version_lines.append(line.strip())
    return version_lines


if __name__ == "__main__":

    if not CHANGELOG_PATH.exists():
        raise FileNotFoundError(f"Didn't find changelog file at expected location: {CHANGELOG_PATH}")

    version = sys.argv[1]
    results = get_changes_for_version(version)
    print("Version", version)
    print()
    print("\n".join(results).strip())
