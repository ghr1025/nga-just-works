# Implementation plan: list page vs thread page coordinates

## Order

1. **Entry coordinate** — `ArticlePage.kt`
   - In `ArticleNavigation.fromSearchResult`, set `page = 1` and update the
     comment: a list/search row's `page` is a list coordinate, never a thread
     page. Keep `pid`, reply author, `searchPost`, title and `topicInfo` logic.
   - Do not change `ThreadPageInfo`, `TopicConvertFactory`,
     `TopicListPresenter` or favorite deletion.

2. **Wire facts** — `ThreadData.java` and `ReadThreadLegacyMapper.kt`
   - Add `__PAGE` / `__R__ROWS_PAGE` int fields with
     `get__PAGE/set__PAGE` and `get__R__ROWS_PAGE/set__R__ROWS_PAGE`.
   - In `ReadThreadLegacyMapper.map`, set them from
     `wire.page.value ?: 0` and `wire.rowsPerPage.value ?: 0` next to
     `set__ROWS(...)`. Leave `ThreadPageInfo.page` mapping unchanged.
   - Touch no core module file.

3. **Reader paging** — `ArticlePage.kt:ArticlePagingInfo.normal`
   - Add `@JvmOverloads` parameter
     `reportedPage: Int? = data.__PAGE.takeIf { it > 0 }`.
   - Derive `size` from positive `__R__ROWS_PAGE`, else 20.
   - Use `effectivePage = reportedPage ?: page`.
   - Compute `computed = pages(__ROWS, size)` for non-LOOKUP queries and
     `totalPages = max(computed ?: 0, reportedPage ?: 0)` (null for LOOKUP or
     when both are zero).
   - Apply `badBounds` only when no reported page:
     `reportedPage == null && computed != null && page > computed`.
   - Set `pageBasis`: LOOKUP → `LOOKUP_WINDOW`; reported → `REPORTED`;
     badBounds → `UNKNOWN_WINDOW`; else `REQUESTED`.
   - Set `invalidMetadata = badBounds || (reportedPage != null && computed != null && computed < reportedPage)`.
   - Fill `reportedCurrentPage = reportedPage`.
   - Keep `canEstimateFloorPage = !badBounds && floorWindow(query, effective, size, rows)`
     and `totalRows = __ROWS.takeIf { it > 0 }`.
   - Keep Java callers source-compatible via the 3-argument overload.

4. **Cache replay** — `ArticleCache.kt`
   - Legacy raw branch (`entry.isLegacy()`) calls
     `ArticlePagingInfo.normal(query, stored.page, data, reportedPage = null)`
     so pre-existing `<page>.json` files keep their stored page authority.
   - Owned replay path stays unchanged; it still validates stored source,
     page size and page basis against the recalculated paging.
   - Do not change `ArticlePageCache`, eligibility, envelope schema or file
     layout.

5. **Tests**
   - `ReplySearchNavigationTest`: existing `assertEquals(3, param.page)` and
     `assertEquals(3, full.page)` become 1; keep the query-kind assertions.
     Add one explicit assertion that the list page is not propagated for a
     non-reply row.
   - `NormalArticleParserTest`: add focused cases over the real parser:
     - `__ROWS=25, __PAGE=3, __R__ROWS_PAGE=10`, requested page 3 → size 10,
       effective 3, total 3, `REPORTED`, floor window true for floors 20..24.
     - same response, requested page 9 → effective 3, total 3, not bad bounds,
       floor estimation still based on the loaded rows.
     - `__PAGE` present, `__R__ROWS_PAGE` absent, requested 9 → effective 3,
       total 3, size 20.
     - `__PAGE` absent, `__ROWS=160`, requested 7 → existing behavior
       (size 20, total 8, `REQUESTED`).
     - `__PAGE` absent, requested 9 with `__ROWS=160` → existing
       `UNKNOWN_WINDOW` / null total behavior.
     - mapper assertion: `data.__PAGE == 3` and `data.__R__ROWS_PAGE == 10`
       survive the facade.
   - `ArticleCacheStoreTest` (or the closest existing legacy-replay test): a
     legacy raw page whose stored page differs from the raw `__PAGE` still
     replays with the stored page and is readable.
   - Do not add tests for `ArticlePagerAdapter`/`ArticleTabFragment`; their
     behavior is unchanged and already covered by the reader-session suite.

## Validation

Targeted first:

```bash
./gradlew :nga_phone_base_3.0:testDebugUnitTest \
  --tests 'sp.phone.mvp.model.thread.NormalArticleParserTest' \
  --tests 'sp.phone.mvp.model.thread.ReplySearchNavigationTest' \
  --tests 'sp.phone.mvp.model.thread.ArticleCacheStoreTest' \
  --console=plain
```

Then the repository Debug gate once:

```bash
./gradlew testDebugUnitTest lintDebug --continue --console=plain
```

No live NGA request, no device install, no cache-file migration.

## Review gates

- Diff contains no change to `ThreadPageInfo.page` writers/readers used by
  lists or favorite deletion.
- `AppArticleParser` and `lib_core` are untouched.
- `ArticlePagingInfo.normal` without reported metadata produces exactly the
  previous values for the existing fixtures.
- Cache envelopes still satisfy `ArticleCacheWrite` / `ArticleCacheReplay`
  invariants; legacy replay test is green.
- Spec follow-up in Phase 3: state in `thread-detail-compat-contract.md` (or the
  ordinary-wire contract) that ordinary reads take `__PAGE`/`__R__ROWS_PAGE`
  from `ThreadData`, and that list pages never become thread pages.

## Rollback points

1. After step 1 only: entry fix is independently revertible and already removes
   the reported trigger from ordinary list navigation.
2. After step 3/4: revert `ArticlePage.kt`, `ThreadData.java`,
   `ReadThreadLegacyMapper.kt` and the legacy replay call together; no
   persisted format changes. Clear local thread cache only if owned pages with
   a non-20 layout id must replay under the old parser.
3. Tests in step 5 revert with their production change.
