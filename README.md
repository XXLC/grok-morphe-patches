# XXLC Grok Patches

Patches by XXLC for Grok Android, compatible with Morphe Manager.

## Supported app

Supports only `ai.x.grok` **1.2.56-release.00**, versionCode **11256200**,
APKMirror **`.apkm`** input.

## Install with Morphe Manager

In Morphe Manager, add `https://github.com/XXLC/grok-morphe-patches` as a patch
source for stable releases, or use [Add to Morphe Manager](https://morphe.software/add-source?github=XXLC/grok-morphe-patches).
For prereleases, add `https://github.com/XXLC/grok-morphe-patches/tree/dev` instead.
Select the supported app file and apply the patches.

## Patches

| Patch | Behavior | Selected by default |
| --- | --- | --- |
| Bypass license check | Skip the Pairip license check. | Enabled |
| Download all (post) | Add a separate menu item to download all media in the current Imagine post. | Enabled |
| Download all (library) | Add a separate button to download all media in the selected Imagine posts. | Enabled |
| Download best video quality | Select the best video URL already supplied by the server, preserving caption/music overlays and falling back to the original URL when needed. Images are unchanged. Not tested on a device. | Enabled |
| Download file names | Use `Grok_yyyyMMdd_HHmmss_SSS_sequence` names for downloaded images/videos, preserving extensions and existing deduplication. Not tested on a device. | Enabled |
| Download folders | Use `Download/Grok`, `Pictures/Grok` and `Movies/Grok` according to the existing save path. Before Android 10, legacy saves use `Download/Grok`. Includes ordinary files handled by the shared saver. Not tested on a device. | Enabled |
| Copy prompt | Add a post menu item that copies the current media prompt; hide it when the prompt is empty. No seed is available in the 1.2.56 model. Not tested on a device. | Enabled |
| Share file | Add a shortcut to the existing share flow: open its destination sheet, then choose file sharing. This takes two steps and preserves the original Share availability. Not tested on a device. | Enabled |
| Keep screen on | Prevent automatic screen timeout while Grok's main Activity is resumed, including chat. Uses more battery. Not tested on a device. | Disabled |
| Disable telemetry | Stop internal analytics events, Mixpanel tracking and Hightouch payload submission; force Crashlytics collection off. Not tested on a device. | Enabled |
| Hide upgrade prompts | Hide the Imagine limit banner and Settings upsell card, preserving usage data and subscription management. Not tested on a device. | Disabled |
| Skip delete confirmation | Immediately confirm Imagine image/video deletion and close the dialog. Deletion is permanent on the server, with no Undo. Not tested on a device. | Disabled |

The original Download actions remain available. Download all, Copy prompt and
Share file use shared internal infrastructure. Selecting a patch applies it:
all `enabled` options default to true, including patches not selected by default.
Set `enabled` to false to skip a patch's edits while keeping it selected. Shared
dependencies may still run. These are patch-time choices; there is no additional
settings screen in the app. Library Download all shows the number of requests
prepared for download, not completed downloads.

## Verification

- Grok 1.2.56-release.00 has not been tested on a device. Static source checks
  have been performed; a CI build and validation of the patched bytecode are pending.

## Known limitations

- All new patches and the 1.2.56 changes have not been tested on a device.
- Selecting Copy prompt, Share file or Download all (post) runs the unnamed
  `PostActionFlagsPatch.kt` dependency. It changes equality comparisons for the
  app's Download actions to include the three injected action flags, even when
  a selected public patch's `enabled` option is false.
- The download queue toast may not appear on some Android versions. Notify uses
  the library handler's Context first; if it is null, it tries two reflective
  application lookups that Android may restrict. Notification failures are
  silently ignored and do not stop download processing.
- X sign-in does not work with a re-signed APK because of server-side checks by
  X/xAI. Use another sign-in method. These patches do not spoof application
  signatures or attestation and do not include an X sign-in bypass.
- Disable telemetry targets the identified event paths and the app's Crashlytics
  collection setter. It is not a network firewall: SDK initialization, previously
  queued events and other tracking paths have not been ruled out. Crashlytics may
  initialize before the app calls its setter.
- Hide upgrade prompts changes Compose rendering and does not unlock paid
  features or alter server limits. Other upgrade surfaces remain available.
- Skip delete confirmation invokes the existing callbacks during composition.
  Recomposition could repeat the callbacks if dismissal has not removed the
  dialog; this behavior still needs device testing. Leave it disabled unless
  permanent deletion without confirmation is intended.
- Best video quality selects only among server-provided URLs; it cannot create
  a higher-quality source. Images expose a single URL, so this patch offers no
  higher-quality image variant and performs no image processing.
- Copy prompt reads the current media prompt; copying seeds is unavailable in
  this model. Select all has not been implemented for the Imagine action bar.
- Filename sequence numbers are unique within a running process. Existing image
  deduplication remains, but uniqueness across app restarts is not guaranteed for
  the video saver on Android versions before 10.

## Disclaimer

- Not affiliated with, endorsed by, or sponsored by xAI or the Morphe project.
  "Grok" is a trademark of its owner.
- Provided "as is", without warranty of any kind. Use at your own risk.
- This repository contains only patch source code. It does not contain or distribute
  APKs or any xAI or Google assets.
- The Pairip license check bypass is intended only for personal compatibility with a
  lawfully obtained copy of the app. Modifying the app may violate its terms of
  service; you are responsible for how you use these patches.

## Development

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for maintenance and release instructions.

## License

Licensed under [GPLv3](LICENSE), subject to the retained [NOTICE](NOTICE).
