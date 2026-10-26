# Legacy Fragment Host System Back

## 1. Scope / Trigger

Apply to system-back changes in PostActivity and LauncherSubActivity, or to
FragmentBackCallback. Android 16 with target 36 no longer delivers the legacy
Activity interception used by these hosts. Shared code must also preserve the
maintainer's primary Android 15 experience and the API 29 installation floor.

## 2. Signatures

```kotlin
FragmentBackCallback(
    available: java.util.function.BooleanSupplier,
    consume: java.util.function.BooleanSupplier,
    fallback: Runnable,
) : OnBackPressedCallback(true)
```

Each Java host registers with
`getOnBackPressedDispatcher().addCallback(this, callback)` in onCreate.
The Activity lifecycle owns callback activation and removal.

## 3. Contracts

- A fragment is consumable only when nonnull, added and holding a current view.
  Never dereference a detached or destroyed view's toolbar through a stale
  fragment reference.
- PostActivity delegates to TopicPostFragment, whose ToolbarContainer closes
  the current emoticon/formatting panel before the Activity exits.
  LauncherSubActivity delegates only to its available BaseFragment; other
  fragments use the existing fallback.
- When unavailable or unconsumed, save enabled state, disable this callback,
  invoke the dispatcher exactly once, and restore the saved state in finally.
  Do not recurse into an enabled callback or call the replaced legacy Activity
  super.onBackPressed interception.
- Keep toolbar Up, direct finish, application SwipeBackHelper, Compose drawer
  BackHandler and Kotlin WebView history under their existing owners. This
  change does not add exit/draft confirmation or route every finish through
  the dispatcher.
- Do not add a custom predictive animation, panel-state framework, or duplicate
  window insets to perform this migration. The only app-wide back callback is
  the user-controlled predictive back setting below.

## 3a. Predictive back setting

Settings › 外观设置 › 预测返回手势 (`key_predictive_back`, default off). With
target 36 the system runs predictive back for every activity; on launchers whose
full-screen gesture does not feed the gesture progress continuously (HyperOS,
Xiaomi 13 Pro) the cross-activity close animation then plays in pieces.

- `PredictiveBack.install(activity)` registers `PageBackCallback` first in
  onCreate of BaseActivity (app and lib_base_ui), BaseComposeActivity,
  TemplateComposeActivity and AboutActivity, so it sits at the bottom of the
  dispatcher chain. FragmentBackCallback, BaseFragment, the drawer BackHandler
  and WebView history are added later and keep running first.
- While the setting is off the callback is enabled: the system treats back as
  app-handled, skips the predictive animation, and the callback finishes the
  activity, which plays the regular close transition once. While on, the
  callback is disabled and the system predictive back runs as before.
- The callback follows a change of the setting immediately (preference
  listener, kept alive by the callback, removed on destroy) and re-reads it on
  every resume, so a change applies to pages already open. The manifest flag `enableOnBackInvokedCallback` is not
  used because it cannot change at runtime.

## 4. Validation & Error Matrix

| Input | Required result |
| --- | --- |
| Available fragment consumes | No fallback; callback enabled state unchanged |
| Available fragment declines | Exactly one fallback while callback is disabled |
| Fragment/view unavailable | Do not call consumer; exactly one disabled fallback |
| Fallback throws | Restore original callback enabled state, propagate failure |
| Activity stops/destroys | Lifecycle registration governs callback ownership |

## 5. Good/Base/Bad Cases

- Good: a visible editor panel consumes back before leaving the post page.
- Base: an ordinary preference fragment does not consume and the host delegates.
- Bad: calling dispatcher while the same callback remains enabled, retaining a
  toolbar from a destroyed view, or changing toolbar Up semantics incidentally.

## 6. Tests Required

Run FragmentBackCallbackTest against the production callback with fake
availability/consumer/fallback functions, and PageBackCallbackTest for the
setting-driven enabled state, single finish and refresh on a changed setting. Assert consumer order, unavailable
view skipping, single fallback, disabled state during fallback, and restoration
on both success and exception. Compile both Java hosts and inspect lifecycle
registration. Keep reader/editor/draft/drawer regression suites intact.

Fake/JVM tests and source review do not establish Android lifecycle execution,
predictive gesture cancellation, keyboard layout or navigation animation.
Device checks remain opt-in under the Android quality policy; do not probe ADB.

## 7. Wrong vs Correct

Wrong: override Activity.onBackPressed for target 36, or call the dispatcher
from a still-enabled callback.

Correct: register a lifecycle-owned AndroidX callback, ask the current fragment
first, then disable/delegate-once/restore for the existing fallback.
