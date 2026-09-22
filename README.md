# Rene'R ID Print

[![Android quality checks](https://github.com/tatselkrik/rene-r-id-print/actions/workflows/android-quality.yml/badge.svg)](https://github.com/tatselkrik/rene-r-id-print/actions/workflows/android-quality.yml)

Rene'R ID Print is a custom Android app built specifically for Rene'R's in-house ID-photo service. It guides the operator through capturing one person, choosing one of eight maximum-use, even-numbered combinations of 2×2-inch, passport-size, and 1×1-inch photos, and producing an accurately sized 5×7-inch sheet. The finished sheet can be previewed, saved, shared, or printed directly to an Epson EcoTank L15150 over local Wi-Fi. The app is designed for this business and is not available on Google Play. Signed installers are available through GitHub Releases.

## Version 1.0.4 — Photo adjustments

- Added Brightness, Contrast, Vibrance, Saturation, and Temperature (cooler/warmer) below the compact **Background** and **Auto** toggle row. There is no Exposure control.
- **Auto** provides bounded brightness/contrast correction and disables manual sliders while enabled. Switching Auto clears manual adjustments; turning it off returns all sliders to zero. Each new capture starts neutral with Auto off. There is no Reset button.
- The compact review screen places the crop images first, then Background/Auto, then Brightness and Contrast. The remaining sliders and checks are available by scrolling.
- Adjustments use the original working image each time, so edits do not accumulate. Both crop previews, the sheet preview, Save, Share, and Print use the same adjusted image. Layout waits until processing finishes.
- Transparent replacement backgrounds remain white. Direct printing still adds the established L15150/RC Woven color and size corrections.
- Version 1.0.4 reuses the v1.0.3 signing identity and installs as an update. Kirk confirmed all physical checks passed on September 22, 2026, including printing the adjusted settings, and approved publication.

The superseded early project-context document has been removed. Current behavior is documented here; project constraints and signing instructions remain in `AGENTS.md`, `memory.md`, and `GIT_AND_RELEASE_GUIDE.md`. Earlier source remains in Git history.

## Version 1.0.3

Version 1.0.3 passed local build, 43 unit tests, release lint, signing/integrity checks, and emulator checks. Kirk confirmed successful phone testing and approved release on September 14, 2026.

Version 1.0.3 uses a new signing identity. Uninstall the older signed app before installing v1.0.3, saving any photos that need to be kept first. Uninstalling clears the app's private settings and cache. Future builds reuse the new key automatically on the configured Windows account without a password prompt; see `GIT_AND_RELEASE_GUIDE.md`.

- Opening or returning to the app checks the saved printer automatically. Wi-Fi connection changes also trigger a check while the app is visible. If its address changed, discovery searches for the L15150 and verifies the saved certificate before updating the address.
- With no saved printer, the app can pair automatically when discovery finds exactly one L15150. Multiple matching printers require selection by address in Printer Setup. Initial pairing trusts the certificate presented on the local network; subsequent automatic connections must match it.
- Camera capture remains available while the printer is being checked or is offline. The camera and preview display connection status. Print retries connection if no printer is ready.
- Only encrypted IPPS printing is permitted. Legacy unencrypted connections need one explicit secure setup. Changed certificates require an explicit reconnect in Printer Setup and are never silently replaced by automatic discovery.
- Face and eye positions must fit inside the square crop. Layouts containing passport photos also require the detected face and both eyes to fit inside the narrower passport crop. These checks apply to Print, Save, and Share. They detect clipping; they do not certify passport compliance or account for all hair/head-covering boundaries.
- The existing eight layouts, size corrections, color behavior, application identifier, and Android dependency versions are retained.

## Version 1.0.2 baseline

Implemented in this milestone:

- Portrait-only guided rear-camera capture using CameraX.
- On-device ML Kit checks for exactly one face, both eye landmarks, obvious closed eyes, and head angle.
- Optional on-device **White Background + Lighting** processing that uses a soft person mask to whiten only the background, feather hair and shoulder edges, and gently lift shadows on the person while protecting highlights. The Automatic Check screen can switch back to the untouched original for comparison.
- A square camera guide that becomes the exact 2×2 source crop.
- A 35:45 crop that keeps the square's full height and trims only the left and right sides.
- Resolution and camera-guide mapping checks before sheet creation.
- A **Choose Combination** screen with eight tested millimetre-based layouts. Every nonzero quantity is even, and the default is 4 × 2×2-inch, 2 × 35×45 mm, and 4 × 1×1-inch photos.
- Short black corner cutting guides instead of full borders around each photo.
- A portrait 5×7 preview driven by the same geometry as the print generator.
- A directly rendered 3000×4200 JPEG with 600 dpi metadata (exactly 5×7 inches).
- **Save** and **Share** actions on the preview; Share opens Android's panel for Quick Share and other installed apps.
- Direct one-tap encrypted IPPS printing to a saved L15150 using the printer's advertised JPEG format.
- Direct jobs are locked to Cassette 1 and request matte photographic media for RC Woven matte photo paper.
- The confirmed L15150/RC Woven print-size correction is built into normal printing; the temporary calibration screen has been removed.
- Unit tests for copy count, physical dimensions, page bounds, exact square framing, and the passport side crop.

## App walkthrough

The normal workflow is guided capture, automatic checking, combination selection, and print-ready output. The walkthrough images below show the earlier interface, before the v1.0.4 adjustment controls.

<table>
  <tr>
    <th>Take Photo</th>
    <th>Automatic Check</th>
    <th>Choose Combination</th>
  </tr>
  <tr>
    <td><img src="docs/images/take-photo.jpg" alt="Guided Take Photo screen" width="280"></td>
    <td><img src="docs/images/automatic-check.gif" alt="Automatic Check workflow" width="280"></td>
    <td><img src="docs/images/layout.gif" alt="Maximum-use layout selector" width="280"></td>
  </tr>
  <tr>
    <th>Printer Setup</th>
    <th>Save</th>
    <th>Share</th>
  </tr>
  <tr>
    <td><img src="docs/images/printer-setup.jpg" alt="Direct Printer Setup screen" width="280"></td>
    <td><img src="docs/images/save.jpg" alt="Saving the generated JPEG" width="280"></td>
    <td><img src="docs/images/share.jpg" alt="Sharing the generated sheet" width="280"></td>
  </tr>
</table>

## Run it

1. Install the latest stable Android Studio from the official Android Developers website. Use the recommended Windows `.exe` installer and the **Standard** setup.
2. A separate Java installation is normally unnecessary. Android Studio includes its own JetBrains Runtime/JDK. If prompted for the project's Gradle JDK, choose `GRADLE_LOCAL_JAVA_HOME` or Android Studio's embedded JDK; this project requires JDK 17 or newer.
3. In **Tools → SDK Manager → SDK Platforms**, install **Android SDK Platform 36.1**.
4. In **SDK Tools**, install the latest **Android SDK Build-Tools 36.x**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools**. The Android Emulator is optional because this project should be tested on the real Samsung phone.
5. Open this folder using **File → Open**. Do not create a second empty project. Choose **Trust Project** if asked, then allow Gradle sync and dependency downloads to finish.
6. On the Samsung phone, enable Developer options and USB debugging. Connect it using a data-capable USB cable and accept the phone's debugging authorization prompt. On Windows, install Samsung's Android USB driver only if Android Studio cannot detect the phone.
7. Select the Samsung phone in Android Studio's device menu, choose the `app` run configuration, and click **Run**. Grant camera permission on first launch.
8. Connect the phone and L15150 to the same local network, normally the same Wi-Fi. Open the app and wait for **Printer ready**. If automatic discovery needs help, open **Printer Setup** and connect by address once. The app remembers the printer's certificate and address and checks it again when opened or brought back to the foreground. Local printing does not require internet access.

Android Studio, internet access during the first Gradle sync, the Samsung phone, a USB data cable, and the printer are the only essentials. Kotlin, Compose, CameraX, ML Kit, Gradle, and JUnit are project dependencies; Gradle downloads them automatically. Photoshop, Python, Node.js, a database, and a cloud service are not required.

The project uses Gradle 9.1.0, Android Gradle Plugin 9.0.1, compile SDK 36.1, min SDK 23, Compose BOM 2026.06.00, CameraX 1.6.1, the bundled ML Kit face detector 16.1.7, and bundled ML Kit selfie segmentation 16.0.0-beta6.

Version 1.0.2 retains Version 1.0.1's color behavior: Automatic Check, Preview, Save, and Share remain color-neutral, while direct printing applies the confirmed warmer photo tone (red 106%, green 101.5%, blue 94%) for the L15150 and RC Woven matte paper. The person mask is stored with real transparency, and every exported photo cell is explicitly filled white before the person is drawn; the white page/background and black cutting guides therefore remain neutral.

## Version 1 print workflow

After one-time setup, the operator chooses a combination, and **Print** on the preview renders that exact millimetre layout directly to a 3000x4200 JPEG and sends it to the saved L15150. There is no separate print-setup page or printing app. Direct printing requests:

- 5×7-inch portrait photo paper
- Exact 5:7 image proportions at 600 dpi
- Fill one 5x7 sheet; no printing-app resizing
- High or Best quality
- Paper source: Cassette 1
- Paper stock: RC Woven matte photo paper

The layout includes safe white margins. Because the direct JPEG has the same exact 5:7 aspect ratio as the paper, the printer's fill setting does not distort the layout. A fixed correction of approximately 106.0% horizontally and 105.9% vertically is coded for the L15150 with RC Woven matte paper from the repeatable measurements received so far.

## Selectable geometry

All layout coordinates are stored in millimetres in `SheetLayout.kt` and converted to screen units or JPEG pixels only at rendering time. Version 1.0.2 offers these maximum-use even combinations:

| 2×2 | Passport | 1×1 |
| ---: | ---: | ---: |
| 6 | 0 | 0 |
| 4 | 2 | 4 |
| 4 | 0 | 8 |
| 2 | 6 | 0 |
| 2 | 4 | 6 |
| 2 | 2 | 8 |
| 0 | 8 | 4 |
| 0 | 6 | 8 |

Every photo keeps its exact finished dimensions. Horizontally separated photos retain at least 3 mm and vertically separated photos retain at least 2 mm. Every preset stays inside the 5×7 page after the confirmed L15150/RC Woven print-size correction.

## Deliberately unresolved

These requirements need a decision or physical test before production use:

- Country or issuing authority for any additional 35×45 mm composition rules beyond the fixed center side-crop.
- Manual **Adjust** controls for exceptional hair, head coverings, glasses, or landmark failures.
- Whether Wi-Fi Direct or remote printing is needed for genuinely separate networks; the current app targets the bridged home LAN.
- Automatic photo deletion policy. Captures and temporary generated print files currently stay only in the app cache and are not uploaded. JPEGs explicitly saved by the operator go to the location they choose.

## Version 1.0.2 finalization

The selector and all eight layouts passed automated geometry, copy-count, cutting-gap, corrected-page-bound, build, lint, APK-signature, installation, and connected-phone checks. The approved release is merged into `main` and marked by the `v1.0.2` Git tag. Version 1.0.1 remains available under the `v1.0.1` tag.

## Version history

### Version 1.0.2 — Flexible sheet combinations

- Added eight maximum-use layouts with even-numbered combinations of 2×2-inch, passport-size, and 1×1-inch photos.
- Added the **Choose Combination** screen and kept every photo at its exact printed dimensions.
- Retained Version 1.0.1's white-background, lighting, and print-warmth behavior.

### Version 1.0.1 — Photo finishing

- Added optional on-device **White Background + Lighting** processing.
- Added gentle subject-shadow lifting while protecting highlights and soft hair and shoulder edges.
- Added the warmer color treatment for direct printing without changing Automatic Check, Preview, Save, or Share.
- Fixed exported sheets so processed photo backgrounds remain white instead of turning black.

### Version 1.0.0 — Initial release

- Introduced guided rear-camera capture, automatic face checks, exact ID-photo crops, and the original fixed 5×7-inch layout.
- Added accurate previewing, JPEG saving and sharing, printer discovery, and direct Wi-Fi printing to the Epson EcoTank L15150.
- Added the Rene'R ID Print name, business icon, signed Android installer, and private distribution workflow.
