#!/usr/bin/env python3
"""Reject unbraced Kotlin interpolation immediately followed by Korean text."""

from __future__ import annotations

import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KOREAN_SUFFIX = re.compile(
    r"(?<!\\)\$([A-Za-z_][A-Za-z0-9_]*)(?=[\u3131-\u318E\uAC00-\uD7A3])"
)
SKIP_DIRS = {"build", ".gradle", ".git"}


@dataclass
class LexState:
    block_comment_depth: int = 0
    in_triple_string: bool = False


def strip_kotlin_comments(line: str, state: LexState) -> str:
    """Remove real Kotlin comments while preserving string/char literal text."""

    out: list[str] = []
    i = 0
    length = len(line)

    while i < length:
        if state.block_comment_depth > 0:
            if line.startswith("/*", i):
                state.block_comment_depth += 1
                i += 2
                continue
            if line.startswith("*/", i):
                state.block_comment_depth -= 1
                i += 2
                continue
            i += 1
            continue

        if state.in_triple_string:
            end = line.find('"""', i)
            if end < 0:
                out.append(line[i:])
                break
            out.append(line[i : end + 3])
            state.in_triple_string = False
            i = end + 3
            continue

        if line.startswith('"""', i):
            state.in_triple_string = True
            out.append('"""')
            i += 3
            continue

        char = line[i]

        if char == '"':
            start = i
            i += 1
            escaped = False
            while i < length:
                current = line[i]
                if escaped:
                    escaped = False
                    i += 1
                    continue
                if current == "\\\\":
                    escaped = True
                    i += 1
                    continue
                i += 1
                if current == '"':
                    break
            out.append(line[start:i])
            continue

        if char == "'":
            start = i
            i += 1
            escaped = False
            while i < length:
                current = line[i]
                if escaped:
                    escaped = False
                    i += 1
                    continue
                if current == "\\\\":
                    escaped = True
                    i += 1
                    continue
                i += 1
                if current == "'":
                    break
            out.append(line[start:i])
            continue

        if line.startswith("//", i):
            break

        if line.startswith("/*", i):
            state.block_comment_depth = 1
            i += 2
            continue

        out.append(char)
        i += 1

    return "".join(out)


def find_violations_in_lines(lines: list[str]) -> list[tuple[int, str, str]]:
    state = LexState()
    violations: list[tuple[int, str, str]] = []

    for line_no, line in enumerate(lines, start=1):
        code = strip_kotlin_comments(line, state)
        for match in KOREAN_SUFFIX.finditer(code):
            violations.append((line_no, line.strip(), match.group(1)))

    return violations


def scan_repository(root: Path = ROOT) -> list[tuple[Path, int, str, str]]:
    violations: list[tuple[Path, int, str, str]] = []

    for path in root.rglob("*.kt"):
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue

        for line_no, line, identifier in find_violations_in_lines(lines):
            violations.append((path.relative_to(root), line_no, line, identifier))

    return violations


def main() -> int:
    violations = scan_repository()

    if violations:
        print("Kotlin interpolation convention violation(s):")
        for path, line_no, line, identifier in violations:
            print(f"  {path}:{line_no}: {line}")
            print("    Use braced interpolation: ${" + identifier + "} before Korean suffix text.")
        return 1

    print("Kotlin interpolation convention check passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
