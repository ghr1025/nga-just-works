# Design: list page vs thread page coordinates

## Boundary

This fix stays in the app layer:

- `sp.phone.mvp.model.thread.ArticleNavigation` / `ArticlePagingInfo` own the
  list-to-reader page contract and the reader's page arithmetic.
- `sp.phone.http.bean.ThreadData` gains the two wire facts the ordinary parser
  already decodes but currently drops.
- `ReadThreadLegacyMapper` (app adapter) copies those facts from
  `ReadThreadWire`; the core decoder, DTOs, transport and App compatibility
  parser are unchanged.

The core repository already exposes `ReadThreadWire.page` and
`ReadThreadWire.rowsPerPage` (`lib_core_data/.../ReadThreadWire.kt:15-16`,
decoded at `ReadThreadWireDecoder.kt:85-86`). No core change is needed.

## Current failure chain

```text
forum/search/favorites list page N
  -> TopicConvertFactory.convertTopic(..., page=N) sets ThreadPageInfo.page = N
  -> TopicSearchFragment.handleClickEvent -> ArticleNavigation.fromSearchResult
  -> ArticleListParam.page = N (list page reused as thread page)
  -> read.php?page=N
  -> server renders the last available page but local code keeps requested N
  -> ArticlePagingInfo.normal: N > ceil(__ROWS/20) => totalPages = null, badBounds
  -> ArticlePagerAdapter: count = 1, single title = N
  -> reader shows one wide tab "N"; pages 1..last are unreachable
```

## Decisions

### 1. List rows keep their page; the thread request does not

`ArticleNavigation.fromSearchResult` always sets `page = 1`. `ThreadPageInfo.page`
continues to mean the list page for list semantics (favorite deletion uses it at
`TopicListModel.java:89`). Explicit page coordinates from URLs, history, cache
openers and `显示全部` are untouched.

Trade-off: a future "open at the topic's last page" list affordance must pass an
explicit page rather than deriving it from the row's list page. There is no such
affordance today.

### 2. Wire page facts travel on `ThreadData`

Add to `ThreadData`:

```java
private int __PAGE;
private int __R__ROWS_PAGE;
public int get__PAGE(); public void set__PAGE(int page);
public int get__R__ROWS_PAGE(); public void set__R__ROWS_PAGE(int size);
```

`ReadThreadLegacyMapper.map` populates them from
`wire.page.value ?: 0` and `wire.rowsPerPage.value ?: 0` (same place it sets
`__ROWS`). 0 means absent/invalid.

Why not read `ThreadPageInfo.page`: that model field already means the list page
in list rows, the cache contract explicitly forbids using it as page authority,
and the same bean is serialized into `topicInfo`. A dedicated wire field keeps
the two coordinates separate.

`ThreadData` is never JSON-deserialized or persisted as a whole (only `raw` and
`ThreadPageInfo` are stored), so the added bean fields do not alter any stored
format.

### 3. `ArticlePagingInfo.normal` adopts reported coordinates

```kotlin
@JvmStatic @JvmOverloads
fun normal(query, page, data, reportedPage: Int? = data.__PAGE.takeIf { it > 0 }): ArticlePagingInfo {
    val size = data.__R__ROWS_PAGE.takeIf { it > 0 } ?: 20
    val effective = reportedPage ?: page
    val computed = if (query.kind == LOOKUP) null else pages(data.__ROWS, size)
    val total = if (query.kind == LOOKUP) null
                else maxOf(computed ?: 0, reportedPage ?: 0).takeIf { it > 0 }
    val badBounds = reportedPage == null && computed != null && page > computed
    ...
}
```

Resulting behavior matrix:

| `__PAGE` | `__R__ROWS_PAGE` | requested vs total | effective | totalPages | basis |
| --- | --- | --- | --- | --- | --- |
| absent | absent | in range | requested | ceil(__ROWS/20) | REQUESTED |
| absent | absent | out of range | requested | null (existing) | UNKNOWN_WINDOW |
| present | present | in range | reported | max(ceil(__ROWS/size), reported) | REPORTED |
| present | present | out of range | reported | at least reported, not null | REPORTED |
| present | absent | out of range | reported | at least reported, not null | REPORTED |

Additional rules:

- `pageSize` is the positive `__R__ROWS_PAGE`, else 20.
- `reportedCurrentPage` is filled for ordinary reads too (diagnostics/tests).
- `invalidMetadata` is true when `badBounds`, or when a reported page exceeds
  the local row-count estimate (metadata disagreement, not a user error).
- `canEstimateFloorPage` stays `!badBounds && floorWindow(query, effective, size, rows)`
  so a short page or a deleted-floor gap still disables floor arithmetic.
- LOOKUP queries keep `totalPages = null` as today; the reported page is still
  the effective coordinate, matching the App parser's existing behavior.

This mirrors the App path, which already treats a positive `currentPage` as
authoritative (`AppArticleParser.kt:52-61`).

### 4. Cache compatibility

- New owned envelopes record the real `pageSize`/`pageBasis`; replay keeps
  validating them.
- Legacy raw cache replay (`ArticleCacheReplay`, owner == null) calls
  `normal(query, stored.page, data, reportedPage = null)`. The stored page stays
  the page authority for pre-existing files keyed by `<page>.json`.
- Known caveat: an owned read cache written before this change stored layout
  `read_php-20` even if the response now yields a different size. Such pages
  fail envelope validation (locally, as designed) and simply need re-caching.
  There is no live NGA evidence that ordinary read pages use a size other than
  20, so this should be rare or empty.

### 5. Session transition side effect

`ArticleReaderState.pageSize` starts at 20. If a read reports a different size,
the existing `ArticleReaderSession.adopt` treats it as a layout transition:
generation bump + handoff, then the handoff delivers the same response without
another network request. No new state or API is introduced.

## Data flow after the fix

```text
list row (page = list page)  --click-->  ArticleListParam(page = 1)
read.php?page=1 -> data.__PAGE=1, data.__R__ROWS_PAGE=20 (or server size)
  -> ReadThreadLegacyMapper -> ThreadData.__PAGE/__R__ROWS_PAGE
  -> ArticlePagingInfo.normal -> effectivePage=1, pageSize=server size, total=real total
  -> ArticleReaderSession/adapter -> tabs 1..total, selected 1
```

Deep link `page=99` on a 5-page thread:

```text
server renders page 5, __PAGE=5
  -> effectivePage=5, totalPages=max(5, computed)=5
  -> reader shows page 5 of 5 instead of one tab "99"
```

## Risks and rollback

- Risk: trusting `__PAGE`/`__R__ROWS_PAGE` could be wrong if a proxy rewrites
  them. They come from the same JSON object that already supplies `__ROWS` and
  rows; no new request or trust boundary is added.
- Risk: page-size identity changes cache layout ids. Mitigated by local-only
  failure and re-caching; documented above.
- Rollback: revert `ArticlePage.kt`, `ThreadData.java`,
  `ReadThreadLegacyMapper.kt`, the legacy replay call and tests. No server,
  database or file migration is written. Owned cache pages whose layout id
  reflects a non-20 size would then fail to replay under the old parser; clear
  the local thread cache if a clean rollback is required.
