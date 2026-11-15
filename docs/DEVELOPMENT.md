# Development

## Architecture and patch targets

The twelve public bytecode patches share the app target
in [Compatibility.kt](../patches/src/main/kotlin/io/github/xxlc/patches/grok/Compatibility.kt):
`ai.x.grok` **1.2.56-release.00**, versionCode **11256200**, with `.apkm` input.
Sources are in [patches/grok](../patches/src/main/kotlin/io/github/xxlc/patches/grok).

The method names and descriptors below come from `Fingerprints.kt` and the Kotlin
injections. Roles assigned to obfuscated classes describe their use by these
patches; they are inferred from the source, not original xAI class names or an
independent verification of the APK at runtime.

### Bypass license check

`LicensePatch.kt` makes the static method
`Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V`
return immediately. This Pairip class is not an obfuscated Grok class; its
fingerprint also checks for the string `LicenseClient`.

### Download all (post)

`PostDownloadAllPatch.kt` depends on the unnamed `PostActionFlagsPatch.kt`. This
infrastructure adds public boolean fields `b`, `c` and `d` to `Ls7/s0;` once, for
Download all, Copy prompt and Share file respectively. Each starts false.

| Modified method | Role and injected behavior |
| --- | --- |
| `Ls7/s0;->equals(Ljava/lang/Object;)Z` | Shared action equality: compare all three flags once, then continue the original status comparison. Unequal actions may retain equal hash codes, which respects the equality contract. |
| `Lu60/c0;->y(Lo7/pb;)Ljava/util/List;` (static) | Post menu builder: copy the menu list and insert an action with `b = true` after Download, or at the end if Download is absent. |
| `Lu60/j9;->j(Ls7/h1;)Lvb/a;` (static) | Menu label mapping: substitute the shared Download all string ID when the download action has `b = true`. |
| `Loa/r2;->k0(Ls7/h1;Lf8/u1;Lmd/g;)Ljava/lang/Object;` | Post action handler: when `b` is set, iterate the post's media and dispatch downloads for the other items; the current item continues through the original path. |

The post handler obtains the post through `Loa/r2;->f0()Lw7/h;` and its media
through `Lw7/h;->d()Ljava/util/List;`. It skips the current item by object identity
or equal media identifiers, and creates `Lmd/h;` coroutine callbacks with
discriminator `0x0d` for the remaining items. These methods and the callback
constructor are referenced by the injected code, not modified by this patch.

### Download all (library)

`LibraryDownloadAllPatch.kt` adds a separate button and includes
`extensions/extension.mpe` through `extendWith`.

| Modified method | Role and injected behavior |
| --- | --- |
| `Ltb/q1;->invoke()Ljava/lang/Object;` | Click dispatcher: handle discriminator `0x7f` before the original packed switch, set the extension flag and invoke the existing download action. The original Download branch clears the flag. |
| `Lab/r0;->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | Library button rendering: add Download all after the existing Download button, reusing the layout and enabled state. |
| `La2/e;->A(Ljava/lang/Object;)Ljava/lang/Object;` | Selection processing: when the action is Download and the extension flag is set, create a `Lo7/b1;` download request for each media item in each selected post, then advance to the next post. Clear the flag at the reset point. |

`Lo7/o0;->a` identifies the existing library download action. The patch uses
discriminator `0x7f`, which is outside this dispatcher's packed-switch range
`0x0..0x1c`; the injected branch handles it before the switch. `0x1d` is used by
`Lmd/h;` and other callbacks, not as a case of this switch. Neither `Lo7/o0;` nor
the `Lo7/b1;` request constructor is modified.

### Telemetry, upgrade prompts and delete confirmation

The following targets were inspected in the decoded 1.2.56 smali. Public patch
names are English and match the README exactly; descriptions remain Vietnamese.
All three expose the shared patch-time `enabled` option, defaulting to true even
when the patch is not selected by default. They need no extension or resource
dependency.

| Patch / selected by default | Class and method | Role and reason for choosing it |
| --- | --- | --- |
| Disable telemetry / enabled | `TelemetryPatch.kt`: `Lm/y0;->x(Ljava/lang/Double;Ljava/lang/String;Ljava/util/Map;)V` | Internal fire-and-forget analytics entry; anchored by `GrokAnalyticsImpl` and `trackEvent: `. Return immediately before queuing analytics work. Its void callers do not await a result. |
| Disable telemetry / enabled | `Lvd0/u;->g()Z` | Mixpanel opt-out getter; anchored by its call to `Lvd0/z;->e(Ljava/lang/String;)V`, which must also match `opt_out_` and `MixpanelAPI.PIdentity`. Return true before taking a monitor. Both `m` and `n` skip event submission when it returns true. |
| Disable telemetry / enabled | `Lfd0/c;->b(Lgd0/a;)V` | Hightouch/Segment payload dispatcher; anchored by `opt-out`, `Created payload %s.` and `Running payload %s.`. Return before payload creation and dispatch. Its Runnable callers do not receive a result or require a completion signal from this method. |
| Disable telemetry / enabled | `Lai/x/grok/GrokApplication;->b(Z)V` | Crashlytics collection setter; anchored by `firebase_crashlytics_collection_enabled`. Force `p0` false at entry and keep the original setter, preference update and balanced monitors. |
| Hide upgrade prompts / disabled | `HideUpsellPatch.kt`: `Ldg/w1;->invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | After the unique `Lo7/d5;->s:Lo7/c5;` read, set only `v10` to null. It becomes the animated target state; the `Lya/j;` content lambda returns Unit for null. The source state and server limits are untouched. |
| Hide upgrade prompts / disabled | `Lse/b;->q(Lae/d;Lse/h0;ZLse/w;Lse/s;Ljn/l0;I)V` | After `Lse/h0;->c:Z` is read into `v14`, force that local boolean false. The existing `if-ne v14, v13` (with `v13 = 1`) selects `v2 = 0`. The `SubscriptionCard` builder only shows the upsell when this flag is true and the management name is null. A non-null management name and the separate Usage item remain available. The title helper `Lse/b;->z(Ljn/l0;)Ljava/lang/String;` must match `grok_settings_upsell_title`. |
| Skip delete confirmation / disabled | `SkipDeleteConfirmPatch.kt`: `Lfb/f;->k(Ls7/m1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;Ljn/l0;I)V` | Anchored by `grok_image_delete_post_confirm_action` and the `Lfb/i;->b(...)V` dialog call. Inside the branch permitted by the composer execution check, invoke confirm `p1`, then dismiss `p2`, then jump to the original restart-group end. |

Register counts are guarded at patch time: 14, 3, 12 and 5 for the four telemetry
targets; 50 and 25 for the two UI edits; 14 for deletion. The opt-out preference
reader and upsell title helper are also resolved and checked (5 and 16 registers),
without modifying them. No `.locals` changes are needed. All newly emitted
non-range register operands are at most `v14`.

The Imagine banner edit preserves its existing labels and changes `v10` only
between the field read and its copy to `v23`. The Settings edit preserves the
existing `cond_f` and `goto_a` paths: it changes only `v14` before the comparison,
leaving the management name in `v10`, original parameters and usage data intact.
Tracing `q` through `Ld2/c;`, `Lse/f0;`, `Lse/b;->t`, `u`, and `Lse/i0;` leads to
the title and button helpers using `grok_settings_upsell_*` resources. Do not
confuse `CursorAccountUnificationCard` with this upsell.

Deletion retains the original skip-recomposition path. The inserted calls use
`p1 = v10` and `p2 = v11`; no scratch register is needed, and the invocation
results are intentionally ignored. The original `p0..p4` remain live for the
restart callback. The patch checks the unique group start (`i0`), execution gate
(`W`) and end (`v`), and requires the original `goto` after the dialog call to
target that end. `addInstructionsWithLabels` and an `ExternalLabel` preserve
that target, including the original restart callback registration.

These are source-reviewed changes, not device-tested behavior. Telemetry coverage
is limited to the identified paths: SDK startup, old queues and other telemetry
are not proven blocked, and Crashlytics can initialize before the app calls its
setter. No authentication, Castle, Pairip, payment, push, chat or media API path
is changed by these three patches. The risky Compose changes are opt-in;
`Lu60/i0;` is deliberately not modified.

Imagine image/video deletion is permanent on the server, with no Undo. Resource
inspection found permanent-deletion descriptions; the Undo labels found concern
bots or drawing strokes, not recovery of deleted Imagine media. The patch does
not implement recovery. It runs callbacks during composition: repeated
recomposition before dismissal could invoke them again. Device testing must
cover first composition, skipped and repeated recompositions, dismissal,
subscription management, usage rendering and the null banner state.

### Shared resources and extension

Both download patches depend on `DownloadResourcesPatch.kt`, an unnamed internal
resource patch that does not appear as a separate choice in Morphe Manager. It
adds `grok_imagine_post_download_all` to `res/values/strings.xml` as `Download all`
and to `res/values-vi/strings.xml` as `Tải tất cả`. `PublicXmlManager` allocates the
public string ID used by the bytecode injections.

`StringResources.kt` defines the shared `XxlcString(name, en, vi)` catalog. It also
reserves labels for copy prompt, copy seed, share file, select all, queued
downloads and copied notifications; these entries do not enable those features.
The resource patch writes both locales and exposes `stringId(name)` after its
execution. Keep Download all first in the catalog to preserve its allocation
order; existing public IDs are reused. The numbered `%1$d` placeholder is valid
Android formatting and remains unchanged in XML. Only the dollar sign in the
Kotlin source is escaped; `formatted="false"` is unnecessary.

[DownloadAllState.java](../extensions/extension/src/main/java/io/github/xxlc/extension/grok/DownloadAllState.java)
contains the static boolean `all` used by the library patch. It is an in-memory
flag, not a persisted preference. The post patch uses the action's `b` field
instead of this extension flag.

`Notify.queued(context, count)` is an extension utility for a short English or
Vietnamese toast on the main looper. The library handler supplies its public
`Lw8/h;->l:Landroid/content/Context;` field through v9, which the original code
immediately overwrites after loading the processor into v6. Neither register
counts nor coroutine state change. If that Context is null, the utility tries
`ActivityThread.currentApplication()`, then `AppGlobals.getInitialApplication()`
through reflection, with a separate try/catch for each attempt. It silently
ignores notification failures; the toast may not appear on some Android versions.
Until resource IDs are passed into
the extension, it formats its own text using the default locale. Library Download
all calls it before clearing its flag, using the size of the prepared request
list, guarded by the original Download action and a positive size. This is a
queue-preparation count, not a completion or successful-download count. The post
loop has no added notification: an exact count would require changing its loop
or introducing additional state.

### Video quality, file organization and post actions

The following additions target only 1.2.56 and have not been built, applied to an
APK or tested on a device as part of this change. The table shows whether each
public patch is selected by default; every `enabledOption` defaults to true.
Resource and flag dependencies may still run when a selected public patch has its
option disabled.

| Patch / selected by default | Class and method | Role, reason and risk |
| --- | --- | --- |
| Download best video quality / enabled | `BestVideoQualityPatch.kt`: `Lw8/u0;->j(Ls7/c0;Ls7/l2;Lgo0/c;)Ljava/lang/Object;` | Immediately before `Ls7/i3;->b()`, call `Lv60/kd;->j(Lw7/s;Ls7/t3;String;String)String` with `Ls7/t3;->e` (BestPossible), the original caption and music fields. A non-null URL goes to the existing `Lt8/d;` constructor; null uses the original call. Only server-provided video variants are selected. There is one image URL, so no higher-quality image variant or image-processing change is implemented. |
| Download file names / enabled | `DownloadNamesPatch.kt`: `Lac/j;->c()String`, `Lac/k;-><clinit>()V` | Replace the video prefix and formatter constants; append a shared atomic sequence to the formatted timestamp. Names are `Grok_yyyyMMdd_HHmmss_SSS_sequence.mp4`. The sequence prevents simultaneous saves colliding within a process; it resets on app restart. |
| Download file names / enabled | `Lhm0/c;->d(String,Uri,String,Context)String`, `->e(File,String,String)String` | Rename the result of `URLUtil.guessFileName` before the existing MediaStore/filesystem deduplication. `DownloadNameHelper` preserves the extension and ignores non-image/non-video MIME types. Both modern and legacy media paths are covered; the video saver before Android 10 still lacks a cross-restart no-overwrite guarantee. |
| Download folders / enabled | `Lue0/s;->a(Context,String,File)Object` | Replace Download with Download/Grok. Both recovered callers are video saves (`La1/i;` and `Lab/c1;`). In `ue0/s.b`, the collection selector uses the first directory component: Download/Grok still selects MediaStore Downloads. The legacy branch already calls mkdirs. |
| Download folders / enabled | `Lhm0/a;->invokeSuspend(Object)Object`, `Lhm0/c;->g(Context,String,String)V`, `->f(String,String,Context,String)V` | Replace Movies/Pictures/Download constants with their Grok subdirectories; keep each existing collection. All three legacy task branches still use Download/Grok. Change mkdir to mkdirs in the legacy saver so nested directories can be created. The shared saver also moves ordinary files into Download/Grok; this wider scope is intentional and disclosed. |
| Copy prompt / enabled | `CopyPromptPatch.kt`, `PostMenuActions.kt`: `Lu60/c0;->y(Lo7/pb;)List`, `Lu60/j9;->j(Ls7/h1;)Lvb/a;`, `Loa/r2;->k0(Ls7/h1;Lf8/u1;Lmd/g;)Object` | Append action flag c only for a nonempty `pb.o`; resolve `grok_xxlc_copy_prompt` through the resource dependency. The handler checks that `h0()` is non-null before reading its latest `.o` and Context p, calls `ClipboardHelper`, then returns Unit; null falls through with the original v0 intact. The helper rejects null/blank text, posts clipboard/Toast work to the main looper and resolves the copied string by name. No seed field is available in this model. |
| Share file / enabled | `ShareFilePatch.kt`, `PostMenuActions.kt`: the same menu, label and handler methods | Append action flag d only when the original Share item in v24 exists; copy its enabled flag. Resolve `grok_xxlc_share_file` through the resource dependency. Before the existing handler dispatch, replace v0 with `Ls7/d1;->a`. The original branch emits `Ls7/g0;->a` through the unchanged media/continuation path; no new suspend call is introduced. This is a two-step shortcut to the existing destination sheet, whose available destinations remain app-controlled. |
| Keep screen on / disabled | `KeepScreenOnPatch.kt`: `Lai/x/grok/main/GrokActivity;->onResume()V`, `->onPause()V` | Add/clear window flag 0x80 after invoke-super and before the existing try block. Covers the entire resumed Activity, not just generation; consumes battery. Existing catch handlers and try labels stay attached to the original instructions. |
| Download all notification / enabled with library patch | `LibraryDownloadAllPatch.kt`: `La2/e;->A(Object)Object` | Insert a separate notification block at the reset boundary before clearing the all flag. v5 is the complete request ArrayList; v6 holds its size and v9 receives the handler's Context. Both scratch registers are immediately overwritten by the original processor/state assignments. Gate by v0 being the original Download action. No suspension or extra counter is added. |

Register counts are unchanged: video j has 28 locals / 32 total registers;
menu y has 26 / 27; label j has 9 / 10; handler k0 has 18 / 22; action equals
has 3 / 5; the Activity methods each have 3 / 4. The video insertion uses dead
v13..v15, with v12 holding the video post; the enclosing handlers use the
preserved v0, v3 and v5..v9 instead. New menu blocks preserve v0, p0, v10 and v24
and use v1..v6 as scratch. Copy sets its own enabled flag; Share copies the
original item's enabled flag. Handler insertions use only dead v5/v6 before the
original media lookup. Label insertions either return with fully initialized
v3..v8 or fall through with p0 intact.

The shared flag patch is a dependency of all three post actions, so any one can
be selected alone. It changes equality once and supplies all three actual DEX
fields with ImmutableField. Each public patch creates only its own flagged
actions. Fingerprint match indices refer to original bytecode. Download all
therefore takes the Save resource ID from its fingerprint and finds that literal
again in the current mutable instructions before inserting its label block.
The extra actions find methods by signature and insert at entry or at a freshly
located mutable instruction. Each menu block makes its
own list copy and preserves inputs needed by the other blocks; Copy returns
early and Share becomes a non-download action before the Download all handler.
All six application orders of the three public patches were modeled at source
level; this does not replace executing Morphe Patcher against the APK.

The three retained post Download all smali blocks and all five original library
blocks are unchanged from the pre-change working tree. The old equals block is
replaced by the shared three-flag check; the library notification is a separate
additional block. Internal label names are unique across the patch sources.

Select all is deferred: `Lub/j.b` keeps the Imagine state, but its inspected
`y0.d` list contains `La8/b` MediaTag entries, not container IDs. `Lo7/h1`
supports set selection with the existing 50-item limit, but the loaded container
list, selection callback and a safe action-bar insertion have not all been verified.
The Download callback is not an interchangeable selection callback. No new
selection action, seed option, provider or permission is added.

### Patch-time choices

Morphe Manager selects which patches to apply. The two Download all patches and
the additions above expose an `enabled` boolean option through `PatchOptions.kt`, using the
[`PatchBuilder.booleanOption` API in Morphe Patcher 2.1.0](https://github.com/MorpheApp/morphe-patcher/blob/v2.1.0/src/main/kotlin/app/morphe/patcher/patch/Option.kt).
Patch selection is the effective on/off switch: selecting a patch applies its
edits because every `enabled` option defaults to true. Setting the option to
false skips that patch's bytecode edits while retaining its selection. Shared
resource and action-flag dependencies and the library extension may still run or
be included when the patch is selected. Bypass license check uses Manager's
patch selection and its existing `default = true` declaration.

These are patch-time choices, remembered by Manager, not runtime settings stored
by this extension. There is no in-app settings screen. New risky patches should
declare `default = false` so they are not selected automatically; if they use the
helper, keep `enabledOption(default = true)` so selecting them applies their
edits. Manager persistence has not been tested as part of this source-only
infrastructure change.

## Supporting a new Grok version

Fingerprints pin the obfuscated classes, method signatures and instruction
patterns of **1.2.56-release.00**. They are not version-independent lookups.
Changing the supported version alone is insufficient.

Version-specific descriptors, member references and callback discriminators are
centralized in `GrokNames.kt`. Keep them there rather than embedding obfuscated
names in fingerprints or injections. A version update starts with this catalog
and `Compatibility.kt`, followed by register and control-flow checks; structural
changes in a new release can still require changes to the patches themselves.

1. Decode a lawfully obtained new app version with apktool and inspect its smali
   and resources, including the relevant splits when starting from `.apkm`.
2. Find equivalent methods using the strings, resource names, opcodes and
   references in `Fingerprints.kt`: for example `LicenseClient`, `VideoDownload`,
   `grok_media_action_save`, `listOfNotNull` and the library packed switch.
3. Recheck control flow, register allocation, fields, constructors and callback
   discriminators. Update fingerprints and every affected injected reference;
   obfuscated names can change between releases.
4. Update `Compatibility.kt` only after confirming the new package, version,
   versionCode and input format against the patch targets. Have CI build the
   bundle, then verify patching and behavior on a device, including both original
   Download actions and the two Download all actions.

`BytecodeChecks.kt` provides patch-time guards for total register counts, the
registers used at insertion points, and unique opcode/reference matches. The
patches also check selected return and branch instructions. These checks reject
some incompatible bytecode layouts; compilation alone does not exercise them
against an APK or prove runtime behavior.

Injected smali with internal labels must use `addInstructionsWithLabels`, or
`addInstructionsAtControlFlowLabel` when existing branch-target labels must be
preserved at the insertion point. Plain `addInstructions` only handles internal
labels correctly at index 0. Morphe Patcher does not verify DEX by default during
patching, so label errors may only surface on a device. After generating a new
APK, inspect the branch targets in every modified method.

## Build and validation

Use GitHub Actions for builds; do not run Gradle locally for this repository's
maintenance workflow. Local builds require an Android SDK and authenticated
access to GitHub Packages, which are not available in the documented local setup.
`settings.gradle.kts` resolves the Morphe plugin from GitHub Packages using
`GITHUB_ACTOR` / `GITHUB_TOKEN`, with `gpr.user` / `gpr.key` Gradle properties as
overrides. Keep credential values out of the repository and logs.

The [Release workflow](../.github/workflows/release.yml) uses Java 21 and Node.js.
It runs semantic-release on pushes or manual dispatch. When a release is created,
the release pipeline builds the bundle, generates the patch list, publishes the
release assets and attests the bundle. Otherwise, the workflow runs
`:patches:buildAndroid clean --no-daemon` to check compilation. Device testing is
still required for app behavior; the README records the existing verification and
known sign-in limitation.

## Releases

[.releaserc](../.releaserc) configures semantic-release:

- `main` publishes stable releases; `dev` publishes prereleases.
- Conventional Commits determine whether and how to release: `feat` produces a
  minor release, `fix` a patch release, and breaking changes a major release.
  Non-breaking `docs` and `chore` commits do not trigger a release on their own.
  The configuration also maps `bump`, `perf` and `build(Needs bump)` to patch
  releases. Use the type that describes the actual change.
- The release bot updates `patches-bundle.json`, `patches-list.json`,
  `CHANGELOG.md` and the version in `gradle.properties`. Do not edit these files
  manually. The next release must regenerate `patches-list.json` from the current
  sources to replace its older app compatibility metadata. The release
  configuration also stages `README.md`, but its generator
  leaves the hand-maintained README unchanged while patch-generation markers are
  absent.
- Stable releases are backmerged from `main` to `dev` by the release automation.

The [Open a PR to main workflow](../.github/workflows/open_pull_request.yml)
opens a PR from `dev` to `main` on pushes to `dev` when there are commits to merge
and no existing PR. Manual dispatch uses the selected branch as the source.
It requires **Settings > Actions > General > Workflow permissions > Allow GitHub
Actions to create and approve pull requests** to be enabled in the repository.
Its workflow permissions alone do not enable that repository setting.

Bundle signing is optional. The release workflow forwards the optional Actions
secret `MORPHE_SIGNING_KEY`, while `.releaserc` currently leaves
`signatureUrlTemplate` empty and no `morphe-signing.pub` file is committed. Never
commit a private signing key or its value. The presence of the workflow variable
does not establish that a published bundle is signed.
