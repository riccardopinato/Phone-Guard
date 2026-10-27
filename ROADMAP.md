# ROADMAP.md — Phone Guard pre-1.0

Version truth: 0.9.3 / versionCode 5

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

## 1.0 physical certification
Status: **PENDING EXTERNAL EVIDENCE**
- Pixel;
- Samsung;
- Xiaomi/Redmi;
- Table/Pocket/Bag false-positive/negative tuning;
- Charging connect/disconnect and grace;
- Charging auto-arm foreground standby, reboot and OEM behavior;
- Tile/widget/shortcut entry paths;
- per-mode outputs;
- screen-off/Doze/background behavior;
- real flash/volume/DND/reboot behavior.

## Google Play
Status: **DEFERRED BY PRODUCT DECISION**
Play Console declarations, foreground-service reviewer evidence, privacy-policy URL, Data Safety, Play App Signing and store assets remain final publication work.
