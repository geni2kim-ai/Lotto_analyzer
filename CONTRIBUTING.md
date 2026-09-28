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
