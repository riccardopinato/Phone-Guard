# ROADMAP.md — Phone Guard pre-1.0

Version truth: 0.9.4 / versionCode 6

## Foundation inherited from 0.9.2

Status: **DONE**
- Table/Pocket/Bag/Journey detector correctness;
- one authoritative foreground service;
- Journey safety and monotonic expiry;
- bounded capability-aware wake-locks;
- persistent alarm-volume lease and recovery;
- PBKDF2 + Android Keystore PIN verifier;
- persistent rate limiting and strong biometric owner proof;
- single alarm output owner;
- JVM/emulator/release gates;
- EN/IT/ES/FR/PT parity;
- signed INTERNAL lane.

## 0.9.3-A — Shared activation engine
Status: **IMPLEMENTED — exact-head validation required**
- `ProtectionActivationCoordinator`;
- one policy for app, tile, widget, shortcut and charging auto-arm;
- SecurityHealth, capability, charger and permission gates;
- no external quick surface can directly disarm.

## 0.9.3-B — Charging Guard
Status: **IMPLEMENTED — physical cable/OEM validation required**
- fifth `ProtectionMode.CHARGING`;
- arm only with external power connected;
- dynamic power disconnect monitoring while service is alive;
- owner grace before full alarm;
- same-session recovery;
- owner-approved unplug cleanly ends Charging protection;
- no sensor wake-lock needed.

## 0.9.3-C — Charging auto-arm
Status: **IMPLEMENTED — physical background/boot validation required**
- explicit opt-in;
- visible foreground standby;
- fresh charger connection arms Charging Guard;
- already-connected charger requires unplug/replug rather than silently claiming auto-arm;
- boot restores the opt-in standby;
- no manifest implicit power-receiver dependency.

## 0.9.3-D — Quick activation surfaces
Status: **IMPLEMENTED — launcher/device validation required**
- Quick Settings tile;
- home-screen widget;
- static launcher shortcut;
- last selected mode as the common quick-action contract;
- blocked quick activation falls back to opening/using the app rather than bypassing requirements.

## 0.9.3-E — Grace / false-positive UX
Status: **IMPLEMENTED — tuning pending physical matrix**
- configurable 0 / 2 / 5 / 8 second owner grace;
- Pocket/Bag owner-check uses the same product setting;
- Table/Charging use the shared pending-trigger path;
- immediate alarm remains available with 0 seconds.

## 0.9.3-F — Protection Presets
Status: **IMPLEMENTED**
- Café;
- Transit;
- Backpack;
- Public Charge;
- Travel;
- atomic reuse of existing mode/sensitivity/journey/alarm settings.

## 0.9.3-G — Per-mode alarm profiles
Status: **IMPLEMENTED**
- siren;
- vibration;
- flash;
- flash frequency;
- alarm target volume;
- warning screen;
- backward-compatible fallback to 0.9.2 global settings.

## 0.9.3-H — Protection Check
Status: **IMPLEMENTED — device truth still required**
- owner security;
- selected-mode support;
- sensor capability;
- notification state;
- flash/vibration configuration;
- volume/DND;
- battery-optimization status;
- external power when Charging;
- manual sound/vibration/flash checks.

## 0.9.3-I — Event Evidence 2.0
Status: **IMPLEMENTED**
- backward-compatible event JSON;
- optional structured evidence:
  mode, activation source, reason, confidence, detector state, sensor/power summary, output-health summary and grace window.

## 0.9.3-J — Automated regression
Status: **IN VALIDATION**
- Charging support policy;
- Charging no-wake-lock policy;
- Charging recovery;
- preset contract;
- per-mode settings persistence;
- manifest registration for Quick Tile and widget;
- existing 0.9.2 gates remain mandatory.

## 0.9.4-A — Accessory Guard
Status: **IMPLEMENTED — physical accessory/OEM validation required**
- sixth `ProtectionMode.ACCESSORY`;
- wired, USB, Bluetooth A2DP/BLE and hearing-aid audio-output presence through Android audio-device APIs;
- only devices present when protection begins are protected;
- accessory removal uses the shared owner-grace/alarm path;
- owner-approved intentional removal ends the session cleanly;
- persisted Accessory session recovery fails closed if the protected accessory is no longer present;
- no microphone recording and no Accessory monitoring CPU lease.

## 0.9.4-B — Smart Arm
Status: **IMPLEMENTED — context-quality validation required**
- deterministic, testable context resolver;
- priority: external power, connected audio accessory, Pocket evidence, Bag evidence, Table/fallback;
- short local sensor sample only;
- final mode always passes through `ProtectionActivationCoordinator`;
- no authentication, permission or recovery bypass.

## 0.9.4-C — Live Guard Test + Sensitivity Advisor
Status: **IMPLEMENTED — physical detector validation required**
- real, non-alarming trigger tests for Table/Pocket/Bag/Charging/Accessory/Journey;
- timeout means test failure rather than synthetic success;
- local motion/rotation noise analysis;
- Table and Pocket/Bag sensitivity recommendation;
- recommendation is never applied without explicit user action.

## 0.9.4-D — Protection Center
Status: **IMPLEMENTED — OEM guidance requires physical review**
- evolves Protection Check without removing existing checks;
- Accessory readiness;
- manufacturer-specific reliability guidance;
- Android `ApplicationExitInfo` last-exit diagnostics where supported;
- battery and Android security settings entry points;
- complementary Android Theft Protection / Remote Lock / Find Hub guidance;
- no claim that Phone Guard controls OEM/system theft features.

## 0.9.4-E — Advanced per-mode alarm profile
Status: **IMPLEMENTED**
- custom Android document audio URI with built-in-siren fallback;
- pre-alarm style: silent / vibration / beep;
- output duration: 30s / 1m / 3m / until disarmed;
- duration expiry stops outputs only; TRIGGERED and owner authentication remain authoritative;
- profile JSON remains backward compatible with 0.9.3.

## 0.9.4-F — Incident Sessions
Status: **IMPLEMENTED**
- derives incidents from existing backward-compatible event history;
- groups arming/armed/pre-alarm/trigger/terminal events;
- shows trigger reason, mode, source, output evidence and grace when present;
- shareable local plaintext report;
- no location/account data introduced.

## 0.9.4-G — Regression / release freeze
Status: **IN VALIDATION**
- Smart Arm pure-decision tests;
- sensitivity recommendation thresholds;
- Incident Session grouping/report;
- Accessory recovery and wake policy;
- advanced per-mode profile persistence;
- EN/IT/ES/FR/PT string-key parity;
- full development CI and signed INTERNAL exact-SHA gates remain mandatory.

## 1.0 physical certification
Status: **PENDING EXTERNAL EVIDENCE**
- Pixel;
- Samsung;
- Xiaomi/Redmi;
- Table/Pocket/Bag false-positive/negative tuning;
- Charging connect/disconnect and grace;
- Accessory wired/USB/Bluetooth connect/disconnect and grace;
- Smart Arm real-context decisions;
- Live Guard Test trigger recognition;
- Sensitivity Advisor real-noise recommendations;
- Charging auto-arm foreground standby, reboot and OEM behavior;
- Tile/widget/shortcut entry paths;
- per-mode outputs;
- screen-off/Doze/background behavior;
- real flash/volume/DND/reboot behavior.

## Google Play
Status: **DEFERRED BY PRODUCT DECISION**
Play Console declarations, foreground-service reviewer evidence, privacy-policy URL, Data Safety, Play App Signing and store assets remain final publication work.
