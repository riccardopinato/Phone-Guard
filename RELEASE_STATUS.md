# RELEASE_STATUS.md

**Version truth: 0.9.3 / versionCode 5**

This file separates implementation from certification evidence.

## 0.9.3 implemented source scope
- five modes: Table, Pocket, Bag, Charging and Journey;
- Charging Guard with external-power disconnect detection;
- opt-in charging auto-arm foreground standby;
- centralized activation policy for app/tile/widget/shortcut/auto-charge;
- Quick Settings tile, home widget and launcher shortcut;
- configurable owner grace / pre-alarm;
- reusable preset engine;
- per-mode alarm output profiles;
- Protection Check diagnostics;
- structured, backward-compatible event evidence;
- existing 0.9.2 security, recovery, wake-lock and alarm-side-effect invariants preserved.

## Evidence model
Software acceptance still requires the **exact candidate SHA** to pass Development checks and the signed INTERNAL workflow. GitHub Actions is the canonical automated evidence. This file intentionally does not embed a workflow run ID that would become stale when source changes.

## Physical boundary
The following remain **NOT VERIFIED** until tested on the exact signed artifact:
- real cable connect/disconnect behavior;
- Charging grace timing;
- charging auto-arm standby after screen-off/reboot;
- OEM background behavior;
- Quick Settings tile behavior from lock screen;
- launcher widget/shortcut behavior;
- per-mode sound/vibration/flash/volume outputs;
- Pixel/Samsung/Xiaomi sensor false-positive/negative tuning;
- real DND and output recovery behavior.

## Google Play
Play Console/store work remains intentionally deferred until physical readiness.

Release state: **0.9.3 SOFTWARE CANDIDATE — PHYSICAL CERTIFICATION PENDING — PLAY DEFERRED**.
