# Project memory

## 2026-09-09

- Current source baseline at first review was version 1.0.2, commit 866a9b8. The app uses CameraX, on-device ML Kit, eight fixed 5x7 sheet layouts, and direct Epson L15150 printing. Preview/Save/Share are neutral; direct printing applies established size and color corrections.
- Kirk chose the same-Wi-Fi workflow after discussing internet printing. Automatic printer connection should preserve that simple local workflow.
- Keep finalized APK preparation separate from Git/release publication. Kirk tests on the phone first and explicitly authorizes publication afterward. Preserve the existing signing identity for updates.
- Automatic printer checks must also observe Wi-Fi availability while the app is visible. Emulator testing showed that a foreground-only check can finish before Wi-Fi recovers; observing network changes resolved that timing gap.
- Kirk explicitly authorized a new signing identity for v1.0.3 and automatic future signing without password entry. Keep the original key/releases intact. The automatic key uses a generated credential protected by Windows DPAPI and restricted local permissions; missing state must fail rather than silently rotate the key. The first installation requires uninstalling the old signing identity.

## 2026-09-14

- Kirk confirmed the v1.0.3 phone test was all good, including his prior framing concern, and explicitly authorized Git updates and APK publication. This is the new approved baseline; keep the automatic signing identity for future updates.
