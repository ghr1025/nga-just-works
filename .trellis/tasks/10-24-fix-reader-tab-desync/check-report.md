# Check report — reader tab desync

Independent `trellis-check` pass over the three-file diff (PASS), plus the focused
fail-first evidence collected before dispatch.

## Diff

- `ArticleReaderSession.kt` — foreground adoption moves `currentPage` only while
  `key.page == state.currentPage`. Generation, source, page size, `paging`, pending anchor
  and handoff are unchanged.
- `ArticleTabFragment.java` — `renderReaderState` moves the pager through
  `mTabLayout.setCurrentItem(selected, false)`, which force-syncs the highlight after the
  jump.
- `ArticleReaderSessionTest.kt` — two regression tests; 35 insertions, 2 deletions overall.

## Fail-first evidence

With only the session hunk reverted:

```
ArticleReaderSessionTest > laterSelectionSurvivesResultOfPageTheUserLeft FAILED
    expected:<6> but was:<7>
ArticleReaderSessionTest > leavingThePageKeepsSelectionWhenTheResultSwitchesSource FAILED
    expected:<6> but was:<13>
2 tests completed, 2 failed
```

With the fix restored, `ArticleReaderSessionTest` is 12/12.

## Full Debug gate

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew testDebugUnitTest lintDebug \
  :nga_phone_base_3.0:assembleDebug --continue --console=plain
```

- BUILD SUCCESSFUL, 634 tasks.
- Unit tests: 82 suites, 576 tests, 0 failures, 0 errors, 0 skipped.
- Lint: 13/13 Android modules present, 0 Error, 0 Fatal; warnings only.
- Debug APK produced at `nga_phone_base_3.0/build/outputs/apk/debug/nga_phone_base_3.0-debug.apk`.

## Contract review

- Still-selected foreground results keep alignment/handoff semantics; offline/background
  results still cannot change source or layout.
- A retargeted pending anchor cannot be consumed on the abandoned page
  (`ArticleListFragment.consumePendingAnchor` and `ArticleReaderSession.consumeAnchor`
  both require the anchor's page to be the current page).
- Every reader-state consumer of `paging` reads only global fields; none reads
  `effectivePage` from the reader state.
- `RecyclerTabLayout 1.5.0` API behavior re-checked against the resolved artifact.
- No library, resource, Gradle or manifest change; nothing else staged.

## Residual risks

- The unconditional tab re-sync runs on every reader-state publish. A background adoption
  landing mid-drag can transiently reset the tab's offset baseline. This is inherent to the
  required sync during settling; gating it on DRAGGING/SETTLING would reintroduce the bug.
- Device/visual confirmation is intentionally unverified (opt-in ADB policy). The user can
  repeat the quick swipe; no recovery swipe should be needed.
