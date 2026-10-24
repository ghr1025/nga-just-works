# Pager/tab desync while leaving the last page — evidence

Recorded 2026-10-24 for `10-24-fix-reader-tab-desync`. All product citations are
`nga_phone_base_3.0/...` unless stated. Library facts were read from the exact resolved
artifacts in the Gradle cache (`androidx.viewpager:viewpager:1.0.0`,
`com.nshmura:recyclertablayout:1.5.0`, declared in `lib_base_common/build.gradle:52`).

## Symptom

Content shows the page the user left (page 4 of 4, floors 60–63); the tab strip highlights
the page the user swiped to (tab "3"). One further swipe clears it. Display-only: rows,
floors and actions are the real page-4 content, and the fragment's own `mCurrentPage`
field ends up on the page the pager shows.

## Why only the last page

`ArticlePagePrefetchPlanner.plan` (`mvp/viewmodel/ArticlePagePrefetchPlanner.java:17-26`)
only plans candidates `< totalPages`, so the final page is never prefetched. Every other
page is usually READY before it is selected; the final page's request is always in flight
while the user enters or leaves it, which is why this race shows up there first.

## Chain of events

1. **The response is adopted as foreground during the settle.**
   `ArticleListPresenter` passes `mForeground` into `adoptPage`
   (`mvp/presenter/ArticleListPresenter.java:232`, `:261`); `mForeground` is only cleared in
   `ON_PAUSE` (`:343-345`). ViewPager 1.0.0 does not pause the outgoing fragment at
   `ACTION_UP`: `onTouchEvent` sets `mPopulatePending = true` before calling
   `setCurrentItemInternal`, and `populate()` assigns `mCurItem` and then returns early
   while pending. `setPrimaryItem` — and therefore the fragment's `ON_PAUSE` — runs from the
   `mEndScrollRunnable` posted by `completeScroll` at the end of the settle. A response that
   lands during that window is still "foreground" for the page the user just left.

2. **The session rewrites the selection.**
   `ArticleReaderSession.adopt` (`mvp/model/thread/ArticleReaderSession.kt:71-92`) writes
   `currentPage = paging.effectivePage` whenever `foreground` is true (`:87`). It never checks
   whether `select()` (`:51`) has since moved the reader elsewhere. At `ACTION_UP`,
   `ViewPager.scrollToItem` dispatches `onPageSelected` before the settle starts, so the
   fragment has already called `select(3)`; the late adopt then sets `currentPage` back to 4.

3. **The publish jumps the pager back.**
   `ArticleShareViewModel.adoptPage` publishes synchronously
   (`mvp/viewmodel/ArticleShareViewModel.java:54-58`); the fragment's observer
   (`ui/fragment/ArticleTabFragment.java:137`) runs `renderReaderState`
   (`:141-155`), which maps `state.currentPage` to a position and calls
   `mViewPager.setCurrentItem(selected, false)` (`:145`). That non-animated jump re-scrolls
   the content to page 4 while the user's selected tab remains page 3.

4. **The tab strip keeps the old highlight.**
   `RecyclerTabLayout` 1.5.0 ignores `onPageSelected` unless its scroll state is IDLE
   (`ViewPagerOnPageChangeListener.onPageSelected`); during DRAGGING/SETTLING the highlight
   only advances from `onPageScrolled` → `scrollToTab(position, offset, false)` →
   `updateCurrentIndicatorPosition(position, offset - mOldPositionOffset, offset)` with the
   default `0.6f/0.4f` thresholds. The jump in step 3 dispatches
   `pageScrolled(position, 0f)`; after a settle's final frame, `mOldPositionOffset` is
   already `0f`, so `offset - mOldPositionOffset == 0f`, no threshold is crossed, and the
   adapter indicator stays at the page the user swiped to. The content is page 4 and the
   tab is "3" — exactly the screenshot. The next page change dispatches a non-zero delta
   and repairs it, which is the reported self-heal.

Order inside the non-animated `ViewPager.scrollToItem` (verified in 1.0.0 bytecode):
`dispatchOnPageSelected` → `completeScroll(false)` (which may post the end-of-scroll
runnable) → `scrollTo(destX)` → `pageScrolled(destX)`. That ordering is what lets the
fragment's selection and the tab's highlight diverge.

## Fix surface verified in the libraries

- `RecyclerTabLayout.setCurrentItem(int item, boolean smooth)` is public. When a pager is
  attached it calls `mViewPager.setCurrentItem(item, smooth)` and then
  `scrollToTab(mViewPager.getCurrentItem())`.
- `scrollToTab(int)` force-writes the adapter's indicator position and calls
  `notifyDataSetChanged()`, bypassing the threshold logic. That is the supported way to
  guarantee the highlight matches the pager after a state-driven jump.
- `TabLayoutEx` (`lib_base_common/.../widget/TabLayoutEx.java`) extends the library class and
  does not override `setCurrentItem`, so the call is available on the fragment's field.

## Reproduction constraints

The race needs a response landing in a narrow window (during the settle / before the
outgoing fragment pauses), so `adb shell input` reproduction is unreliable and was rejected
by the user. Evidence used instead: the user's screenshot, the "one more swipe recovers"
confirmation, the library bytecode above, and a deterministic JVM test over the session
state machine for the decision half of the bug.

## Re-verifying library behavior

```bash
cd /tmp && rm -rf rtl && mkdir rtl && cd rtl
unzip -o ~/.gradle/caches/modules-2/files-2.1/com.nshmura/recyclertablayout/1.5.0/*/recyclertablayout-1.5.0.aar classes.jar
javap -c -p -classpath classes.jar 'com.nshmura.recyclertablayout.RecyclerTabLayout' | less
```

Look at `setCurrentItem(int, boolean)`, `scrollToTab(int)` and
`updateCurrentIndicatorPosition(int, float, float)`; the inner
`ViewPagerOnPageChangeListener` class carries the IDLE-only `onPageSelected` rule. For
ViewPager, unzip `androidx.viewpager/viewpager/1.0.0`'s aar and read
`onTouchEvent`/`setCurrentItemInternal`/`scrollToItem`/`populate`/`completeScroll`.
