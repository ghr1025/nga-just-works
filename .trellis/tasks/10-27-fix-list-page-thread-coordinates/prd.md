# Fix list page leaking into thread page coordinates

## Goal

Opening a thread from a forum, search, or favorite list must start at thread
page 1, and a read response whose rendered page differs from the requested page
must not collapse the reader pager into one tab. Issue #14 reports both
symptoms as one user-visible failure.

User value: tapping a thread always opens a readable thread with complete page
navigation, regardless of which list page the user was browsing.

## Background and confirmed facts

- The topic-list converter stores the *list* page on every row:
  `TopicConvertFactory.java:192` receives `page` from
  `TopicListModel.java:118` / `:152` (forum, search, favorites list). No
  per-topic page exists in `TopicListBean.TBean`.
- The shared list click handler copies that list page into the thread request:
  `TopicSearchFragment.java:347` and `:364` call
  `ArticleNavigation.fromSearchResult`, whose `page = info.page` is at
  `ArticlePage.kt:122-134`. Ordinary forum/favorite/search rows all use this
  handler.
- Normal thread entry without a parcelable param defaults to page 1 at
  `ArticleListActivity.java:65`; explicit launch coordinates (URL, history,
  cache) are separate inputs and must keep working.
- The ordinary read response carries the server-rendered page and page size as
  `__PAGE` / `__R__ROWS_PAGE`: decoded at `ReadThreadWire.kt:15-16` and
  `ReadThreadWireDecoder.kt:85-86`. `ReadThreadLegacyMapper.kt:159` keeps
  `__PAGE` only inside `ThreadPageInfo.page` and drops `__R__ROWS_PAGE`.
- `ArticlePagingInfo.normal` (`ArticlePage.kt:70-81`) ignores both fields: it
  always uses the requested page and a hard-coded 20-row page. When the
  requested page exceeds the computed total it sets `totalPages = null`.
- `ArticlePagerAdapter.updateReaderState` (`ArticlePagerAdapter.java:67-78`)
  renders an unknown total as one tab labelled with the requested page, and
  `ArticleTabFragment.renderReaderState` (`:189-206`) cannot move to pages that
  are not in the adapter. That is the single wide tab in the issue screenshot.
- The App compatibility path already adopts the response's `currentPage`
  (`AppArticleParser.kt:52-61`), so it does not collapse when the server clamps
  a page; the ordinary read path is the asymmetric one.
- This exact defect is already recorded in
  `.trellis/tasks/archive/2026-10/10-03-floor-favorite-restore/research/findings.md`
  §3 ("单独的 7"), including the same floors 20/21 screenshot shape. It was
  deliberately deferred there. The old upstream pager masked it by forcing
  `position + 1`; the current reader accepts the launch page, exposing it.
- Unverified: "jumping to an earlier floor crashes back to the home screen".
  The reader's floor-jump path in code either scrolls to a row on the loaded
  page or shows "未找到目标楼层"; no deterministic crash was found. It stays a
  separate evidence request, not part of this fix.

## Requirements

- R1. List-to-reader navigation must not reuse the list page as the thread
  page. `ArticleNavigation.fromSearchResult` must produce `page = 1` for both
  full and reply-search results. `ThreadPageInfo.page` keeps its existing list
  meaning so favorite deletion (`TopicListModel.java:89`) is unchanged.
- R2. The ordinary read parser must use a positive response `__PAGE` as the
  effective page and a positive `__R__ROWS_PAGE` as the page size. Missing or
  invalid values keep the current fallbacks (requested page; 20 rows).
- R3. A reported page is authoritative for navigation: total pages must be at
  least the reported page, and an out-of-range request must not null
  `totalPages`, mark the page invalid, or collapse the pager. Floor estimation
  stays gated by the existing row-window check.
- R4. Without a reported page, existing conservative behavior is unchanged:
  requested page, hard-coded 20-row fallback, and `badBounds` → unknown window.
- R5. Cache contracts stay intact: stored page, page size and page basis are
  still validated on replay; legacy raw cache files keep the stored page as
  their page authority instead of adopting the raw `__PAGE`.
- R6. Do not change the App compatibility parser, the core wire decoder, the
  pager/adapter fallback for genuinely unknown metadata, or PID/floor
  navigation.
- R7. Out of scope: the history/favorite list entry's own page field
  (`TopicHistoryFragment.java:203`), favorite-to-PID navigation, and the
  unverified crash symptom. The reported-page fix should still turn a history
  entry whose page is out of range into the real last page instead of a
  collapsed tab.

## Acceptance Criteria

- [ ] AC1 (R1): Unit coverage proves a list row on page 3 produces
  `ArticleListParam.page == 1`; reply search still produces
  `ArticleQuery(tid, pid, replyAuthor, 1)` and keeps `searchPost`.
- [ ] AC2 (R2/R3): A read fixture with `__ROWS = 25`, `__PAGE = 3`,
  `__R__ROWS_PAGE = 10` parsed with requested page 3 yields
  `effectivePage == 3`, `pageSize == 10`, `totalPages == 3`,
  `pageBasis == REPORTED`; a request for page 9 against the same response keeps
  `effectivePage == 3`, `totalPages == 3` (not null) and does not collapse.
- [ ] AC3 (R3): When the reported page exceeds the local row-count estimate,
  total pages are clamped up to the reported page instead of nulled; floor
  estimation is false when the loaded rows do not match the computed window.
- [ ] AC4 (R4): Fixtures without `__PAGE` still use the requested page and the
  20-row fallback, and an out-of-range request without a reported page still
  takes the existing unknown-window path.
- [ ] AC5 (R5): Existing cache tests pass; a legacy raw replay keeps the stored
  page as its effective page; owned read cache envelopes still validate stored
  page size and basis.
- [ ] AC6 (R6): No production change to `ThreadPageInfo.page` usage in list
  deletion or favorite removal; the App parser and core decoder are untouched.
- [ ] AC7: `./gradlew testDebugUnitTest lintDebug --continue --console=plain`
  passes at the end of implementation.

## Manual confirmation

- Browse a forum to page 3 (or any page > 1), tap a thread: the reader opens
  page 1 with every page tab present.
- Open a thread URL whose `page` exceeds the thread's last page: the reader
  shows the last page with tabs 1..last instead of a single tab.
- The issue reporter's crash path remains unverified until steps/logcat are
  supplied; do not claim it fixed.
