# Contributing

## Kotlin string interpolation

Kotlin identifiers embedded next to Korean particles, counters, or other Korean
suffix text **must use braced interpolation**.

Use:

```kotlin
"신규 ${added}건"
"상위 ${previewCount}만 보기"
"${drawNo}회"
```

Do not use:

```kotlin
"신규 $added건"
"상위 $previewCount만 보기"
"$drawNo회"
```

Reason: Kotlin can parse the Korean text as part of the identifier, producing
compile errors such as `Unresolved reference: added건`. The Android CI runs
`tools/check_kotlin_interpolation.py` before compilation and rejects this
pattern in tracked Kotlin source.

Braces are also recommended whenever adjacent text could make the identifier
boundary visually ambiguous, even when the compiler would currently accept it.


### Checker scope

The checker uses a small Kotlin-aware lexer before applying the interpolation
rule. It removes real `//` and nested `/* ... */` comments only when those
markers occur outside string/character literals. Comment markers inside normal
strings or triple-quoted strings are preserved, because interpolation in those
strings is executable Kotlin and must still follow the braced rule.

The lexer is regression-tested for block comments, nested block comments,
normal strings, triple-quoted strings, and inline comments. It is deliberately
not a full Kotlin parser, but it no longer relies on line-prefix heuristics such
as `stripped.startswith("/*")`.
