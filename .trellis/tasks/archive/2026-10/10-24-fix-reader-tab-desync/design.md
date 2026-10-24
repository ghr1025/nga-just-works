# Reader page-selection and tab-sync design

Task: `10-24-fix-reader-tab-desync`. Research/evidence: [research/pager-tab-desync.md](research/pager-tab-desync.md).

## Boundary

Two production files and one test file, all in the pager reader:

1. `nga_phone_base_3.0/src/main/java/sp/phone/mvp/model/thread/ArticleReaderSession.kt` — stop a
   late foreground result from overwriting the user's newer selection.
2. `nga_phone_base_3.0/src/main/java/sp/phone/ui/fragment/ArticleTabFragment.java` — after a
   state-driven jump, force the tab strip onto the pager's actual item.
3. `nga_phone_base_3.0/src/test/java/sp/phone/mvp/model/thread/ArticleReaderSessionTest.kt` —
   regression tests for the decision half.

No transport, parser, cache, prefetch-policy or library changes.

## Change 1 — the selection is owned by the user, not by an in-flight result

`ArticleReaderSession.adopt` currently writes `currentPage = paging.effectivePage` for every
foreground adoption (`ArticleReaderSession.kt:87`). The request key records the page the
request was issued for; `state.currentPage` records the user's current selection, advanced by
`select()` (`:51`) from the pager listener. The result may follow its page only while those
still agree.

```kotlin
val transition = coordinateChange || layoutChange
val generation = state.generation + if (transition) 1 else 0
val accepted = paging.withRequest(key.owner, generation)
data.pagingInfo = accepted
val anchor = state.pendingAnchor?.let {
    if (transition) it.copy(generation = generation, page = paging.effectivePage) else it
}
// The result may follow its page only while the reader still shows that page. A page the
// user has already left must not pull the pager back after a late foreground response.
val followsSelection = foreground && key.page == state.currentPage
state = state.copy(generation = generation, source = paging.source, pageSize = paging.pageSize,
    currentPage = if (followsSelection) paging.effectivePage else state.currentPage,
    paging = if (foreground || state.paging == null) accepted else state.paging?.copy(
        totalPages = accepted.totalPages, totalRows = accepted.totalRows), pendingAnchor = anchor)
if (transition) handoff = data
return if (transition) 2 else 1
```

| `foreground` | `key.page == state.currentPage` | `currentPage` after |
| --- | --- | --- |
| true | true (user still there) | `paging.effectivePage` — unchanged |
| true | false (user swiped away) | unchanged — new |
| false | any | unchanged — unchanged |

Only the selection write is gated. Source, page size, generation, `paging` metadata, pending
anchor and handoff still update exactly as before, so an App fallback completed while the
user reads another page still establishes the source for that generation, as the compat
contract requires.

The session stays pager-free: the comparison uses the request key and the session's own
selection, so it is deterministic and unit-testable on the JVM.

## Change 2 — the tab strip follows the pager after a state-driven jump

`renderReaderState` (`ArticleTabFragment.java:141-155`) maps `state.currentPage` to an
adapter position and moves the pager without animation (`:145`). The tab layout only advances
its highlight from `onPageScrolled` thresholds, and that update is a no-op when the jump
dispatches `(position, 0f)` right after a settle's final `0f` frame (evidence §4 of the
research note). `onPageSelected` is ignored by the library while the pager is not IDLE, and
the fragment's own listener is suppressed by `mApplyingState` anyway.

Use the tab layout's public `setCurrentItem(item, smooth)` instead of moving the pager
directly:

```java
int selected = mPagerAdapter.positionOfPage(state.currentPage);
if (selected >= 0) mTabLayout.setCurrentItem(selected, false);
```

`RecyclerTabLayout.setCurrentItem(int, boolean)` moves the attached pager and then calls
`scrollToTab(mViewPager.getCurrentItem())`, which force-writes the adapter's indicator
position and notifies — bypassing the offset thresholds. If the pager clamps or ignores the
target, the sync follows the pager's actual item, which is what `mCurrentPage` reads on the
next line. `TabLayoutEx` inherits the method unchanged.

## Rejected alternatives

- **Fix only the tab sync.** The content would still be yanked back to the page the user
  left; that is the more damaging half of the bug.
- **Fix only the session.** Legitimate state-driven jumps (handoff/alignment, prefetch
  promotion of the page now shown) can hit the same threshold no-op. The tab sync is one
  line of insurance.
- **`mTabLayout.notifyDataSetChanged()` after the pager call.** Rebinding reads
  `adapter.getCurrentIndicatorPosition()`, which is the stale value; it changes nothing.
- **Skip state-driven jumps while the pager is DRAGGING/SETTLING.** Drops legitimate
  programmatic jumps and can leave the state and pager permanently apart.
- **Remove the `mApplyingState` guard from `onPageSelected`.** The state is authoritative
  during a publish; letting the listener call `select()` there can overwrite a transition
  result and risks state/pager feedback.

## Compatibility and risks

- **User still on the requested page.** `key.page == state.currentPage`, so transitions,
  alignment (`effectivePage`), generation bump and handoff behave exactly as today. The
  existing `sourceAndLayoutHandoffRetiresEveryOldKeyAndIsConsumedOnce` test covers it.
- **Background results.** Unchanged: they still cannot change source/page size and never
  touch the selection.
- **Transition while the user moved.** Source/page size/generation still update for the
  whole reader. The pending anchor is re-targeted to `effectivePage` (existing rule) and can
  only be consumed on that page: `consumeAnchor(expected)` also requires
  `expected.page == state.currentPage`, so it cannot fire on the page the user moved to. The
  handoff stays available for the recreated page of the new generation; `finishScoped`
  returns early on adoption `2`, so nothing is force-displayed on the abandoned page.
- **`state.paging` may describe the abandoned page.** Every reader-state consumer uses only
  global fields — `totalPages`, `totalRows`, `pageSize`, `source`, `canEstimateFloorPage`,
  `candidatePage(floor)` — never `effectivePage` (checked in `ArticleTabFragment`,
  `ArticlePagerAdapter`, `ArticleListPresenter`). No consumer needs the extra merge.
- **Search/cache hosts.** `ArticleListFragment`'s reader-state observer only exists for
  non-pager hosts; this design does not touch it.
- **Rollback.** Revert the two hunks and the tests. No migration, no persisted state, no API
  change.

## Verification

- `ArticleReaderSessionTest`:
  - `laterSelectionSurvivesResultOfPageTheUserLeft` — select another page after the request
    key was captured, adopt that key's foreground result, assert the selection is kept and
    the data is still delivered (owner/generation stamped, `paging` adopted).
  - `leavingThePageKeepsSelectionWhenTheResultSwitchesSource` — the same with an App
    fallback/aligned result: return `2`, source and generation still switch, selection kept,
    handoff still targets the effective page, the old key is retired.
  - Existing tests stay green, especially the foreground alignment case that must still
    move `currentPage`.
- Combined repository Debug test/lint gate from
  [android-quality-guidelines](../../../spec/backend/android-quality-guidelines.md#validation-gate);
  inspect lint XML for Error/Fatal, not only the exit code.
- Device behavior: the user's screenshot proved the symptom; after the fix, the same quick
  swipe should need no recovery step. Unless the user confirms on device, record the visual
  half as unverified per the project's opt-in ADB policy.
