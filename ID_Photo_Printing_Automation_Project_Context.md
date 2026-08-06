# ID Photo Printing Automation - Project Context

## Purpose

Build a simple Android application for a Samsung phone that takes one person's photo, automatically crops and centers it, creates a fixed set of ID-photo sizes on one 5x7-inch photo sheet, shows an accurate preview, and prints to an Epson EcoTank L15150.

This document is the current source of truth for the project unless the user changes a requirement.

## Confirmed hardware and output

- Capture device: Samsung Android phone; exact model and Android version are not yet specified.
- Printer: Epson EcoTank L15150.
- Paper: 5x7-inch photo paper, portrait orientation.
- Paper source: Cassette 1, permanently reserved for this workflow.
- Paper stock: RC Woven matte photo paper. Direct jobs should request the printer's matte photographic media type.
- Input: one newly captured photograph of one person.
- Every printed sheet must contain exactly:
  - Three 2x2-inch square photos
  - Three 35x45 mm passport-size photos
  - Four 1x1-inch square photos
- The app must create the complete sheet automatically. The operator should not have to crop, resize, duplicate, or arrange the pictures manually.

## Intended user experience

The normal workflow should be:

1. Open the app.
2. Tap **Take Photo**.
3. Use an on-screen face guide while taking the picture.
4. The app checks the image, detects the face and eyes, and automatically crops and centers the person.
5. The app creates the fixed 5x7 layout.
6. The operator sees a print-accurate preview and may save the complete sheet as a JPEG or share it through Android's sharing panel (including Quick Share when available).
7. The operator taps **Print**.
8. The job is sent to the Epson L15150 using saved print settings.

The main experience should be only: **Take Photo → Automatic Check → Preview → Print**.

A secondary **Adjust** option may be provided for unusual hairstyles, head coverings, glasses, or failed face detection, but routine photos should require no manual adjustment.

## Automatic image processing

The app should retain the full-resolution camera image and derive two crops from it:

1. A square crop, reused for the 2x2-inch and 1x1-inch copies.
2. A 35:45 aspect-ratio crop for the 35x45 mm copies.

Both crops should be generated independently from the original image. The passport crop should not be derived from an already cropped square image.

Automatic positioning should use detected facial landmarks, especially the eyes, rather than simply centering the camera frame. The system should:

- Require exactly one face.
- Center the face horizontally using stable facial landmarks.
- Apply predetermined head-size, eye-line, chin, and headroom rules.
- Detect obvious blur with a fixed-size Laplacian sharpness score, plus insufficient resolution, poor framing, and optionally closed eyes. The current conservative blur threshold is 45 and the displayed result includes the score for future tuning.
- Request a retake when a reliable compliant crop cannot be produced.
- Avoid beautification, facial reshaping, or other identity-altering effects.
- Version 1.0.1 adds optional local **White Background + Lighting** processing. It uses a soft person-confidence mask to replace the detected background with white, feather uncertain hair and shoulder edges, and apply a mild highlight-protected shadow lift to the detected person. The same gain is applied to all RGB channels so it does not intentionally introduce a new color cast. The Automatic Check screen shows the result and allows comparison with the untouched original. This is lighting/background cleanup, not beautification or facial alteration.

The precise head-size and eye-position requirements still need to be defined. A 35x45 mm output size alone does not establish every passport authority's biometric composition rules.

## Fixed 5x7 sheet layout

The ten images fit on a portrait 5x7-inch sheet in three vertical columns:

- Left column: three 2x2-inch photos
- Middle column: three upright 35x45 mm photos
- Right column: four 1x1-inch photos

Recommended starting geometry:

- 2 mm vertical cutting gaps within each column
- 3 mm gaps between columns
- Total occupied width: 117.2 mm
- Approximate side margins on a 127 mm-wide sheet: 4.9 mm each
- Tallest column height: 156.4 mm
- Approximate top and bottom margins on a 177.8 mm-high sheet: 10.7 mm each

The preview should show the complete sheet, individual cut boundaries, and the exact number of copies. Crop marks may be placed in the gaps, without reducing the required finished dimensions.

## Print-file requirements

The locked millimetre geometry is the source of truth. The app renders that geometry directly to a 3000x4200 JPEG, adds 600 dpi metadata, and explicitly maps the exact 5:7 image to 5x7 media over IPPS. This avoids an uncontrolled printing-app resize. The L15150's driverless IPP service reports JPEG, PWG Raster, URF, and Epson ESC/P-R support but not PDF. The app no longer contains an iPrint or PDF fallback.

- Direct JPEG size: 3000x4200 pixels with 600 dpi metadata, exactly 5x7 inches
- 2x2 photos: exactly 2x2 inches
- Passport photos: exactly 35x45 mm
- Small photos: exactly 1x1 inch
- Print scale: fixed L15150/RC Woven correction coded from the repeatable measurements (approximately 106.0 percent horizontal and 105.9 percent vertical)
- Direct JPEG scaling: fill the exact 5:7 media with an exact 5:7 image
- Borderless expansion: disabled
- Orientation: portrait
- Paper type: matched to the actual glossy or photo paper in use
- Quality: High or Best

Borderless printing is not preferred because the L15150 can enlarge the page slightly in borderless mode, changing the physical photo dimensions. The layout intentionally provides safe white margins.

The corrected output was confirmed on August 5, 2026. The fixed L15150/RC Woven correction remains in normal printing, and the temporary calibration controls and calibration-sheet generator have been removed.

Automatic Check, Preview, Save, and Share remain color-neutral in the finalized Version 1.0.1 release. Direct printing alone applies the confirmed warmer tone to the photo subject (red 106 percent, green 101.5 percent, blue 94 percent). The person mask uses a genuinely alpha-enabled bitmap, and every photo cell is explicitly filled white before the person is drawn. This prevents transparent background pixels from flattening to black while keeping the pure-white page/background and black cutting guides outside the warm treatment. The complete result was tested successfully on the L15150 with RC Woven matte paper before release.

## Printing behavior

The implementation uses one printing route. Pressing **Print** directly on the preview renders the millimetre layout to a high-resolution 5x7 JPEG and sends it immediately to the saved L15150 over local Wi-Fi using encrypted IPPS. The app discovers the printer automatically or accepts its IP address, verifies JPEG and 5x7 support, pins the printer's certificate on first connection, and remembers the connection. There is no additional print-setup page, calibration screen, or iPrint fallback.

### Calibration measurements received on August 5, 2026

The earlier calibration was consistently small:

- 2x2-inch box measured approximately 1.875x1.875 inches.
- 35x45 mm box measured approximately 33x42.5 mm.
- 1x1-inch box measured approximately 0.95x0.95 inches.

The app codes the average correction from these measurements: approximately 106.0 percent horizontally and 105.9 percent vertically. The corrected output was subsequently confirmed, so this correction remains automatic and the temporary calibration feature has been removed.

### Home repeater network

Direct printing uses the saved unicast address (`10.0.0.44`), so the phone may be attached to a different repeater or SSID when every repeater bridges into the same LAN. Discovery multicast may not cross every repeater hop. Repeater router/NAT mode, guest networks, privacy/client isolation, or a changed DHCP address can prevent printing. Prefer access-point/bridge mode, disable isolation, and reserve the printer address on the main router. Genuinely separate networks require routing/VPN or a remote-print service; the current app intentionally implements local-LAN IPPS only.

## Recommended implementation direction

- Native Android application, preferably Kotlin.
- Guided camera capture with a square that exactly defines the 2x2 crop, plus on-device face and landmark checks.
- A deterministic 35:45 center crop that keeps the square's full height and trims only its left and right sides.
- One locked layout template for the first version.
- Direct 600 dpi JPEG generation from the shared millimetre geometry for preview export and the L15150's direct IPPS service.
- Local processing where practical so customer ID photos are not uploaded unnecessarily.
- Remember the selected L15150 and print configuration after initial setup.

## Initial acceptance criteria

The first usable prototype succeeds when it can:

- Capture a clear photograph on the Samsung phone.
- Detect and check the one face framed by the operator inside the camera square.
- Preserve that square exactly for 2x2 and produce the 35:45 crop by trimming only its left and right sides.
- Generate exactly 3 large square, 3 passport-size, and 4 small square copies.
- Display an accurate portrait 5x7 preview.
- Print on the L15150 without uncontrolled printing-app scaling.
- Produce cut photos that measure within the agreed physical tolerance using the confirmed built-in correction.
- Reject or flag images that cannot be cropped reliably.

## Open questions for the next project session

1. What Samsung phone model and Android version will be used?
2. Which country's or organization's 35x45 mm composition rules must be followed?
3. Will the repeaters remain one bridged LAN, or is Wi-Fi Direct/VPN support eventually required for isolated networks?
4. Should customer photos be automatically deleted after printing, and if so, after what period?

## Suggested next step

Preserve Version 1.0.1 as the stable fixed-layout release. The next optional Version 1.0.2 feature is a client-order quantity selector that safely rearranges the three photo sizes while retaining the confirmed print correction, cutting gaps, and direct-print workflow.
