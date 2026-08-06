# Rene'R ID Print

Version 1.0.2 test candidate of the native Android app for capturing one person, choosing one of eight maximum-use even-number combinations, producing a 5×7-inch ID-photo sheet, previewing it accurately, and printing directly to an Epson EcoTank L15150 over local Wi-Fi. Version 1.0.1 remains the finalized stable release until this candidate passes phone and paper testing.

The product requirements in `ID_Photo_Printing_Automation_Project_Context.md` remain the source of truth.

## Version 1.0.2 test-candidate status

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

## Run it

1. Install the latest stable Android Studio from the official Android Developers website. Use the recommended Windows `.exe` installer and the **Standard** setup.
2. A separate Java installation is normally unnecessary. Android Studio includes its own JetBrains Runtime/JDK. If prompted for the project's Gradle JDK, choose `GRADLE_LOCAL_JAVA_HOME` or Android Studio's embedded JDK; this project requires JDK 17 or newer.
3. In **Tools → SDK Manager → SDK Platforms**, install **Android SDK Platform 36.1**.
4. In **SDK Tools**, install the latest **Android SDK Build-Tools 36.x**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools**. The Android Emulator is optional because this project should be tested on the real Samsung phone.
5. Open this folder using **File → Open**. Do not create a second empty project. Choose **Trust Project** if asked, then allow Gradle sync and dependency downloads to finish.
6. On the Samsung phone, enable Developer options and USB debugging. Connect it using a data-capable USB cable and accept the phone's debugging authorization prompt. On Windows, install Samsung's Android USB driver only if Android Studio cannot detect the phone.
7. Select the Samsung phone in Android Studio's device menu, choose the `app` run configuration, and click **Run**. Grant camera permission on first launch.
8. Connect the phone and L15150 to the same bridged home LAN. They may use different access points or repeater SSIDs as long as client isolation and repeater router/NAT mode do not separate them. In the app, open **Printer setup** and connect once. The app securely remembers the printer's certificate and IP address.

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

## Home network

The phone and printer do not need the same Wi-Fi name or the same repeater. Direct printing works across repeater hops when all repeaters operate as bridges/access points on one LAN and the phone can reach the printer's saved address (`10.0.0.44`). Automatic discovery may fail when repeaters block multicast, but the saved manual IP can still work. Use access-point/bridge mode, disable guest/client/privacy isolation, and reserve the printer's IP address in the main router so it does not change.

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

## Version 1.0.2 testing

The selector, preview, save/share output, and direct print output must be checked on the Samsung phone before the candidate is merged into `main`, tagged, signed, and finalized as Version 1.0.2. Version 1.0.1 remains available under the `v1.0.1` Git tag.

## Reference PSD

`image placement.psd` is retained as historical input only. Its flattened canvas is 2100×1500 pixels (landscape), it does not match the confirmed 3/3/4 portrait layout, and it contains rotated copies. It must not be used as print geometry.
