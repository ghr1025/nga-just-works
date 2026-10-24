# Reader tab desync — execution plan

## Reviewed planning

- [x] Symptom, screenshot evidence and self-heal confirmation recorded in `prd.md`.
- [x] Library and call-chain evidence recorded in `research/pager-tab-desync.md`.
- [x] User approves `prd.md` + `design.md` at the 1.4 review gate.
- [x] Curate `implement.jsonl` / `check.jsonl`, then `task.py start`.

## Implementation order

1. **Session decision** — in `ArticleReaderSession.adopt`, add the
   `followsSelection` gate described in `design.md` (Change 1). Touch only the
   `currentPage` expression plus one comment; keep generation, source, page size,
   `paging`, anchor and handoff handling untouched.
2. **Tab sync** — in `ArticleTabFragment.renderReaderState`, replace the direct
   `mViewPager.setCurrentItem(selected, false)` with
   `mTabLayout.setCurrentItem(selected, false)` (Change 2). Do not remove
   `mApplyingState` or the `mCurrentPage` re-read below it.
3. **Regression tests** — add the two tests from `design.md` to
   `ArticleReaderSessionTest`, reusing its `session()`, `page(...)` helpers. Do not
   duplicate the existing foreground-alignment coverage.
4. **Diff review** — confirm only the two product hunks and the test file changed;
   none of the research artifacts, specs or unrelated untracked tasks are staged.

## Validation commands

Focused run after the session change (must fail before step 1, pass after):

```bash
./gradlew :nga_phone_base_3.0:testDebugUnitTest \
  --tests 'sp.phone.mvp.model.thread.ArticleReaderSessionTest' --console=plain
```

Combined repository Debug gate once, after review:

```bash
./gradlew testDebugUnitTest lintDebug :nga_phone_base_3.0:assembleDebug --continue --console=plain
```

Inspect the JVM test XML and every Android-module lint XML for failures,
errors and Fatals; do not rely on the process exit code alone.

## Review gates

- **Gate 1 (design conformance):** the `adopt` change alters only the selection
  write; a foreground result for a page the user still shows must still move
  `currentPage` to `effectivePage`.
- **Gate 2 (diff scope):** two product hunks plus tests. No prefetch, transport,
  cache, layout or library changes.
- **Gate 3 (full gate):** Debug tests + lint + assemble as above.

## Rollback points

- After step 1: if an existing reader test shows the gate blocks a legitimate
  transition, revert only that hunk and revisit `design.md` before retrying.
- After step 2: if tab behavior regresses (highlight follows the wrong item), revert
  to the plain pager call; the session fix still removes the content pull-back.
- After the full gate: revert both hunks and the tests together; no data migration
  or persisted state is involved.

## Status

Steps 1-4 are complete and the independent checker passed; see `check-report.md`.

## Phase 3 follow-ups

- [x] Spec: selection-ownership rule added to
  `thread-detail-compat-contract.md`; page-tab rule added to
  `frontend/component-guidelines.md`.
- [ ] Commit only this task's files after explicit confirmation. This is a shared
  worktree: re-check branch and index before staging, never rewrite history, and do not
  touch `.trellis/tasks/10-02-nga-client-api-comparison/`.
- [ ] Device check is opt-in. Ask the user to repeat the quick swipe; if they do not,
  record the visual half as unverified.
