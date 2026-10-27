# Journal - toph (Part 2)

> Continuation from `journal-1.md` (archived at ~2000 lines)
> Started: 2026-10-01

---



## Session 75: 精简非必要测试并准备 6.2.1 发布
<!-- trellis-session: v=2 fp=291d19674c367dbd -->

**Date**: 2026-10-01
**Task**: 精简非必要测试并准备 6.2.1 发布
**Branch**: `main`

### Summary

完成两轮测试精简：删除8项测试与2个文件，减少232行测试代码；553项JVM测试、32项Python测试及13模块lint通过。正式版6.2.1发布说明及标签校验通过，归档后推送main和正式标签触发CI。

### Git Commits

| Hash | Message |
|------|---------|
| `e5e23287` | test: remove obsolete and redundant tests for 6.2.1 |

### Status

[OK] **Completed**

### Next Steps

- Push main and annotated 6.2.1 tag; CI publication is not polled under project policy.


## Session 76: Compatibility reader display parity and 6.2.4 release
<!-- trellis-session: v=2 fp=d57e0b02f2dd0958 -->

**Date**: 2026-10-02
**Task**: Compatibility reader display parity and 6.2.4 release
**Branch**: `fix/compat-reader-display-parity`

### Summary

Implemented default-style support count, attachments and nested comments in compatibility reading; preserved main-post identity/actions, source text, default output and shared IP-location flow. Independent review fixed alterinfo fallback and transient display-state serialization. Final gate passed 562 tests and 13 lint reports with zero Error/Fatal. User authorized commit/push/finish-work and stable 6.2.4; notes validated; main/tag push follows bookkeeping.

### Git Commits

| Hash | Message |
|------|---------|
| `2e2a0a70` | fix: align compatibility reader with default display |

### Status

[OK] **Completed**


## Session 77: Restore floor favorites and release 6.2.5
<!-- trellis-session: v=2 fp=69f6cc222438ce8c -->

**Date**: 2026-10-03
**Task**: Restore floor favorites and release 6.2.5
**Branch**: `fix/restore-floor-favorite`

### Summary

Researched issue #10 and original favorite behavior, then restored both original floor favorite menu entries and the existing clicked-row tid/pid call. Preserved navigation and server APIs. Full gate passed 562 JVM tests and 13 lint reports with zero Error/Fatal; Debug packaging passed. User approved commit, archive and stable patch 6.2.5. Release notes and versionCode 60205000/tag verification passed; main/tag push follows bookkeeping without CI polling per project policy.

### Git Commits

| Hash | Message |
|------|---------|
| `090dce71` | fix: restore floor favorite action |

### Status

[OK] **Completed**


## Session 78: Reader tab desync when leaving the last page
<!-- trellis-session: v=2 fp=b0bc3e9599bfafa8 -->

**Date**: 2026-10-24
**Task**: Reader tab desync when leaving the last page
**Branch**: `fix/reader-tab-desync`

### Summary

Diagnosed via decompiled ViewPager 1.0.0 / RecyclerTabLayout 1.5.0: a foreground page result adopted during the settle rewrote the reader selection because the outgoing fragment pauses only at the end of the scroll, and the non-animated jump back dispatched (position, 0f), which crosses no tab threshold. Gated adopt's currentPage write on key.page == state.currentPage and moved state-driven jumps through TabLayoutEx.setCurrentItem. Two fail-first reader tests (expected 6 but was 7 / 13); full Debug gate 576 tests and 13 lint reports clean; visual half left unverified per opt-in ADB policy. Specs updated: reader selection ownership + page-tab sync rule.

### Git Commits

| Hash | Message |
|------|---------|
| `d6f90812` | fix: keep the reader selection when a late page result arrives |

### Status

[OK] **Completed**


## Session 79: 修复列表页页码泄漏为帖子页坐标 (#14)
<!-- trellis-session: v=2 fp=13818c6bc580ace5 -->

**Date**: 2026-10-27
**Task**: 修复列表页页码泄漏为帖子页坐标 (#14)
**Branch**: `main`

### Summary

普通读取采用响应 __PAGE/__R__ROWS_PAGE 作为有效页与页大小，列表入口固定第 1 页；越界请求不再折叠为单页。无 __PAGE 行为不变，旧版缓存回放保留存储页。全量 Debug 539 例与 lint 通过；机型手动确认未执行。

### Git Commits

| Hash | Message |
|------|---------|
| `b0fdf46c` | fix: 列表页页码不再泄漏为帖子页坐标 |

### Status

[OK] **Completed**
