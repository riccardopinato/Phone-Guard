# RELEASE_STATUS.md

**Version truth: 0.9.4 / versionCode 6**

This file separates implemented software from certification evidence.

## 0.9.4 implemented source scope
- six modes: Table, Pocket, Bag, Charging, Accessory and Journey;
- all 0.9.3 Charging/quick-surface/grace/preset/per-mode-profile/Event Evidence capabilities retained;
- Accessory Guard using Android audio-device connection state, with no microphone recording;
- deterministic Smart Arm routed through the same centralized activation policy;
- Live Guard Test for each protection path without arming full alarm outputs;
- Sensitivity Advisor with explicit user-confirmed application;
- Protection Center with capability/security checks, OEM guidance, battery state, last-process-exit diagnostics and Android theft-protection guidance;
- custom alarm audio per mode with built-in siren fallback;
- per-mode silent/vibration/beep pre-alarm signal;
- per-mode 30s/1m/3m/until-disarmed output duration;
- alarm-output timeout never authenticates or disarms a triggered incident;
- Incident Sessions and local shareable text reports derived from existing event history;
- 0.9.3 data/settings remain backward compatible.

## Evidence model
Software acceptance requires the **exact candidate SHA** to pass Development checks and the signed INTERNAL workflow. GitHub Actions is the canonical automated evidence. This file intentionally does not embed a workflow run ID that becomes stale when source changes.

## Physical boundary
The following remain **NOT VERIFIED** until tested on the exact signed artifact:
- real Table/Pocket/Bag/Journey detector behavior and false-positive/negative tuning;
- Charging cable connect/disconnect, owner grace and charging auto-arm behavior;
- wired/USB/Bluetooth Accessory Guard detection and disconnect behavior;
- Smart Arm decisions in representative physical contexts;
- Live Guard Test recognition for every mode;
- Sensitivity Advisor recommendations against real sensor noise;
- OEM background behavior and Protection Center guidance on Pixel/Samsung/Xiaomi/Redmi;
- Quick Settings tile, widget and shortcut behavior;
- custom audio URI persistence/playback across reboot and provider changes;
- pre-alarm vibration/beep and alarm-output duration timing;
- real sound/vibration/flash/volume/DND behavior and recovery.

## Google Play
Play Console/store work remains intentionally deferred until physical readiness. Foreground-service reviewer evidence and Data Safety must describe Accessory audio-device-state monitoring and Smart Arm local sensor sampling accurately.

Release state: **0.9.4 SOFTWARE CANDIDATE — PHYSICAL CERTIFICATION PENDING — PLAY DEFERRED**.
