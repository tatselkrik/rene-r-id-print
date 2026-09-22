# Project memory

## 2026-09-09

- Current source baseline at first review was version 1.0.2, commit 866a9b8. The app uses CameraX, on-device ML Kit, eight fixed 5x7 sheet layouts, and direct Epson L15150 printing. Preview/Save/Share are neutral; direct printing applies established size and color corrections.
- Kirk chose the same-Wi-Fi workflow after discussing internet printing. Automatic printer connection should preserve that simple local workflow.
- Keep finalized APK preparation separate from Git/release publication. Kirk tests on the phone first and explicitly authorizes publication afterward. Preserve the existing signing identity for updates.
- Automatic printer checks must also observe Wi-Fi availability while the app is visible. Emulator testing showed that a foreground-only check can finish before Wi-Fi recovers; observing network changes resolved that timing gap.
- Kirk explicitly authorized a new signing identity for v1.0.3 and automatic future signing without password entry. Keep the original key/releases intact. The automatic key uses a generated credential protected by Windows DPAPI and restricted local permissions; missing state must fail rather than silently rotate the key. The first installation requires uninstalling the old signing identity.

## 2026-09-14

- Kirk confirmed the v1.0.3 phone test was all good, including his prior framing concern, and explicitly authorized Git updates and APK publication. This is the new approved baseline; keep the automatic signing identity for future updates.

## 2026-09-21

- Kirk requested v1.0.4 with Brightness, Contrast, Vibrance, Saturation and Temperature controls, Auto and Reset below White Background + Lighting. Exposure is explicitly excluded. Prepare the APK locally and await phone-test approval before any Git commit or publication.
- Adjustments are non-destructive and shared by review, sheet preview and export/print. Auto starts off and uses conservative brightness/contrast only; switching it off restores manual settings. Reset restores neutral controls, and a new capture starts neutral. Keep alpha intact so replacement backgrounds stay white, and retain the separate printer calibration.
- Removed the obsolete early project-context document, which contained conflicting crop requirements. README and current code describe behavior; AGENTS.md and the release guide retain project/signing constraints. Earlier versions remain in Git history.

- Later on 2026-09-21, Kirk's phone test confirmed the first v1.0.4 candidate worked but its review screen required too much scrolling. Supersedes the earlier Reset/Auto interaction: remove Reset, clear manual adjustments whenever Auto changes, and return to neutral when Auto is off. Use compact unlabeled crop previews, Background left/Auto right on one row, followed immediately by Brightness and Contrast; remove explanatory text and numeric/end labels. Other sliders can require scrolling.

## 2026-09-22

- Kirk confirmed all physical checks passed for the compact v1.0.4 candidate, including printing with the adjusted settings, and authorized Git/release publication. This supersedes the pending phone-approval status above. Preserve this exact tested APK and the existing automatic signing identity.

## 2026-09-23

- Kirk confirmed v1.0.5 works and authorized publication after comparing A14 5G and S24 FE framing. The candidate replaces the raw-buffer/file transform and smaller-side selection with normalized guide mapping into the upright JPEG, relying on CameraX's existing shared viewport. Keep proportion-mismatch rejection and orientation/resolution tests. Phone results support the correction; the exact device-internal cause was not independently measured.
