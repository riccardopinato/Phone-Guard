# RELEASE_STATUS.md

**Version truth: 0.9.7 / versionCode 9**

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

## 0.9.5 new software scope (physical QA pending)
- local opt-in Adaptive Auto Guard standby using the existing single FGS owner;
- inactivity time thresholds 1/3/5/10/15 minutes, separate Pocket/Bag and Table controls;
- bounded local light/proximity/motion/rotation fusion; moving-bag gyro corroboration;
- optional Activity Recognition walking/running/cycling/vehicle/still permission and Play services fallback;
- screen-on cancellation of pending automatic arming (never of protected/triggered sessions);
- same activation coordinator, live detector calibration, owner authentication and recovery security invariants;
- per-process standby status in Profile, five language parity and decision regression tests.

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
- real sound/vibration/flash/volume/DND behavior and recovery;
- 0.9.5 Pocket/Bag/Table auto-arm precision, recall, false-alarm frequency and eight-hour ON/OFF battery measurements;
- OEM screen-off, vehicle, step, permission/reboot/Doze/force-stop limits and specialUse FGS review.

## Google Play
Play Console/store work remains intentionally deferred until physical readiness. Foreground-service reviewer evidence and Data Safety must describe Accessory audio-device-state monitoring, Smart Arm, Adaptive Auto Guard standby, and optional activity transitions accurately.

Release state: **0.9.7 SOFTWARE CANDIDATE — PHYSICAL CERTIFICATION PENDING — PLAY DEFERRED**.

## 0.9.6 audit remediation (source changes, physical QA pending)
- Fail-closed malformed motion, light, rotation and proximity observations in Auto Guard, with JVM regressions.
- Charging auto-arm reconciliation checks persisted protection before presenting shared standby; preparing notification while reconciling.
- Smart Arm moved ahead of mode selection; advanced controls collapsed by default but remain available, with five-language parity.
- First-run screen corrected to all six modes, including Accessory.
- No new runtime SDK, permission, backend or photo capture.
- OEM/battery accuracy and Play specialUse acceptance remain NOT VERIFIED; no certification claim.


## 0.9.7 — Design and software lane
- New Home priority: protection truth, Smart Arm primary, manual fallback and nearby Auto Guard status.
- Smart Arm permission guidance targets the resolved mode and retries after user grant; it never silently bypasses activation policy.
- Auto Guard dedicated screen with explicit consent and clear standby limitation; Profile settings retained for discoverability.
- Source candidate only pending exact-SHA CI and signed INTERNAL; OEM physical matrix and Play FGS approval remain outstanding.

## 0.9.7 additional software scope (exact-head gates pending)
- Pure Compose illustration-led onboarding with shorter five-language copy; no bitmap payload.
- Incident history: alarms-only filter, local trigger count, empty state and confirmation before permanent deletion.
- Profile Auto Guard control duplication removed: Home and Profile both link to the same opt-in configuration screen.
- Smart Arm context classification rejects malformed readings and requires explicit motion/gyro evidence before claiming Bag or stable Table; JVM regression tests.
- Five-locale safety copy gate included in source-truth check.
- Store copy, FGS reviewer pack, privacy draft created as DRAFT only. No Play Console changes, billing, photos or extra runtime permissions.
