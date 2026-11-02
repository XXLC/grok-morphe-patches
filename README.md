# XXLC Grok Patches

Patches by XXLC for Grok Android, compatible with Morphe Manager.

Supports only `ai.x.grok` **1.2.53-release.00**, versionCode **11253200**,
APKMirror **`.apkm`** input.

In Morphe Manager, add `https://github.com/XXLC/grok-morphe-patches` as a patch
source, or use [Add to Morphe Manager](https://morphe.software/add-source?github=XXLC/grok-morphe-patches).
The source requires a published release before Manager can download its bundle.

| Patch | Behavior | Default |
| --- | --- | --- |
| Bypass license check | Skip the Pairip license check. | Enabled |
| Download all (post) | Add a separate Download all menu item for every media item in the current Imagine post. | Enabled |
| Download all (library) | Add a separate Download all button for every media item in the selected Imagine posts. | Enabled |

The original Download actions remain available. The two download patches share
an internal resource patch with English and Vietnamese labels.

The behavior was verified through manual smali patches on this exact app version,
reported working on a phone. This Morphe implementation has been statically reviewed;
it has **not been built, applied, or tested on a device** until a release says otherwise.

Licensed under [GPLv3](LICENSE), subject to the retained [NOTICE](NOTICE).
