#!/usr/bin/env python3
"""Reject unbraced Kotlin interpolation immediately followed by Korean text."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KOREAN_SUFFIX = re.compile(
    r"(?<!\\)\$([A-Za-z_][A-Za-z0-9_]*)(?=[\u3131-\u318E\uAC00-\uD7A3])"
)
SKIP_DIRS = {"build", ".gradle", ".git"}

violations: list[tuple[Path, int, str, str]] = []

for path in ROOT.rglob("*.kt"):
    if any(part in SKIP_DIRS for part in path.parts):
        continue
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        continue
    for line_no, line in enumerate(lines, start=1):
        for match in KOREAN_SUFFIX.finditer(line):
            violations.append((path.relative_to(ROOT), line_no, line.strip(), match.group(1)))

if violations:
    print("Kotlin interpolation convention violation(s):")
    for path, line_no, line, identifier in violations:
        print(f"  {path}:{line_no}: {line}")
        print("    Use braced interpolation: ${" + identifier + "} before Korean suffix text.")
    sys.exit(1)

print("Kotlin interpolation convention check passed.")
