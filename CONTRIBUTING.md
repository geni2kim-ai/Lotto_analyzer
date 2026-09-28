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

The checker scans Kotlin source code and skips comment-only lines, including
KDoc/block-comment bodies and `//` comment lines. This avoids false positives
from documentation examples. Inline comments that share a line with executable
code are still scanned; keep braced interpolation in those lines as well.
