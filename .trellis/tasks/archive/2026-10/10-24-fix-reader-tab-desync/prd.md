# Fix reader tab desync when leaving the last page

## Goal

While a page is still loading, the user swipes away and the reader keeps showing the page
they swiped away from, while the top page tab highlights the page they swiped to. The
mismatch clears on the next swipe. Fix both halves: a late response must not move the
reader back, and a state-driven pager jump must leave the tab on the page the pager
actually shows.

## Confirmed facts

- User report: on the last page, quickly swiping to the previous page leaves the content
  on the last page while the tab strip highlights the second-to-last page. One more swipe
  restores consistency.
- Screenshot evidence: 4 tabs, tab "3" highlighted, body showing floors 60–63 (page 4 of 4).
  Display-only: no data corruption, no wrong rows.
- The last page is uniquely exposed because `ArticlePagePrefetchPlanner` never prefetches
  the final page, so its response is always in flight while the user is entering or
  leaving it. Other pages are usually already READY.
- ADB reproduction of this timing race is not practical and is not part of acceptance.
  The user supplied a screenshot and the self-heal check instead.

## Requirements

- R1. A foreground page result may change the reader's selected page only if that
  request's page is still the selected page when the result is adopted. A selection the
  user made after the request was issued wins over the result.
- R2. Background results keep the existing rule: they never change the selected page, and
  they still cannot adopt a changed source, page size or effective page.
- R3. Source/page-size transitions, generation bumps, alignment handoff and pending-anchor
  rules are otherwise unchanged, including when the user is still on the requested page.
- R4. After a state-driven, non-animated pager jump, the tab highlight reflects the pager's
  actual current item even when the pager is still in a settling/transition state.
- R5. No new device or NGA traffic is required; the behavior is proven by JVM tests over
  the reader session plus code review of the fragment wiring.

## Acceptance Criteria

- [x] `ArticleReaderSessionTest` fails before the change and passes after: selecting a
  different page, then adopting a foreground result for the page that was requested
  earlier, keeps the later selection and still delivers the page data.
  (`laterSelectionSurvivesResultOfPageTheUserLeft` failed `expected 6 but was 7` with the
  guard removed; `leavingThePageKeepsSelectionWhenTheResultSwitchesSource` failed
  `expected 6 but was 13`.)
- [x] The same behavior shows a foreground result still moves the selection when the
  requested page is still selected (alignment/handoff behavior preserved) — covered by the
  existing `sourceAndLayoutHandoffRetiresEveryOldKeyAndIsConsumedOnce` and
  `pendingAnchorSurvivesSourceAlignmentAndOldPostedScrollCannotConsumeNewAnchor`, both
  green.
- [x] Background adoption behavior is unchanged (existing tests stay green).
- [x] `ArticleTabFragment.renderReaderState` syncs the tab strip to the pager's current item
  after its state-driven jump, using the tab layout's own API.
- [x] The combined repository Debug test/lint gate passes; lint reports contain no
  Error/Fatal (576 tests / 82 suites, 0 failures; 13 lint reports, 0 Error/Fatal).
- [ ] Manual confirmation on device: **unverified**. The user may repeat the quick swipe to
  confirm; the visual half is not proven by the JVM gate.

## Out of scope

- Replacing `ViewPager`/`RecyclerTabLayout`, or changing library internals.
- Prefetch policy changes (the final page stays unprefetched).
- Request/transport, cache or parser changes.
- Fixing the unrelated case where `totalPages` shrinks below the selected page.

## Notes

- Keep `prd.md` focused on requirements, constraints, and acceptance criteria.
- Complex task: technical design is in `design.md`, execution order in `implement.md`.
