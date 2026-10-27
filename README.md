# Phone Guard

Phone Guard is a standalone native Android anti-theft / anti-tampering utility with five protection modes: Table, Pocket, Bag, Charging and Journey.

**Version truth: 0.9.3 / versionCode 5**

## Factory baseline

- App Factory Core: `7b84e33df715cf2a48bda7c7495d0690819bff0f`
- Master: v26.6
- Golden Index: v23
- Tabletop donor: `riccardopinato/Tabletop-Motion-Alarm@9e67bcbd84e8add7d183349644d3f6f6bbe9ffe0`
- Pocket Guard / Pockara donor: `riccardopinato/Pocket-Guard@b7b1c74de66e047c32083365b44561e42237b4fc`
- Luminous Circle: pattern donor for hardened strobe / visual escalation; canonical source commit not available.

Donors are provenance only. Phone Guard has no runtime dependency on donor apps.

## Protection modes

- **Table** — calibrated motion protection for a phone left on a surface.
- **Pocket** — proximity + accelerometer protection, with optional light/rotation corroboration.
- **Bag** — accelerometer plus real light or gyroscope evidence.
- **Charging** — external-power protection for a phone left charging; cable/power removal enters owner verification and can trigger the alarm.
- **Journey** — time-bounded Pocket or Bag protection.

All modes run through one authoritative `ProtectionForegroundService`.

## 0.9.3 Market Value Pack

The pre-1.0 product surface now also includes:

- **Charging Guard** with real power-state monitoring;
- **opt-in Charging auto-arm standby** using the same visible foreground service;
- **Quick Settings tile**: tap while idle to arm the last selected mode; active protection always opens the app instead of bypassing owner authentication;
- **home-screen widget** and **static launcher shortcut** for quick arming;
- one shared **ProtectionActivationCoordinator** for app/tile/widget/shortcut/auto-charge entry points;
- configurable **pre-alarm owner grace**;
- deterministic **Protection Presets** for Café, Transit, Backpack, Public Charge and Travel;
- true **per-mode AlarmOutputProfile** settings;
- **Protection Check** diagnostics plus manual output tests;
- **structured event evidence** for mode/source/confidence/detector/sensors/output health.

## Reliability invariants

- local-first / offline-first;
- capability support must match a reachable detector path;
- Pocket/Bag calibration requires live evidence from required sensors;
- Charging requires real external power at manual arm time;
- Charging auto-arm is explicit opt-in and keeps a visible foreground standby instead of relying on an unreliable manifest power receiver;
- one foreground-service owner;
- every external activation surface uses the shared activation policy;
- Tile/widget/shortcut never bypass PIN or strong-biometric disarm;
- Journey expiry never disarms OWNER_CHECK or TRIGGERED;
- within one boot, Journey duration is governed by monotonic elapsed time so wall-clock changes cannot alter the session;
- wake-lock use is capability-aware; Charging monitoring and auto-arm standby do not hold the sensor CPU lease;
- no UI may claim protection without a real active runtime state;
- force-stop, OEM background restrictions and hardware limitations are never represented as guaranteed-survival cases.

## Alarm escalation

Alarm outputs are owned by one `AlarmOutputController` and may now be configured per protection mode:

- siren;
- vibration;
- configurable LED strobe, 0.5–3.0 Hz;
- alarm-stream target volume;
- warning screen and temporary brightness.

The alarm-volume lease is acquired **only when alarm outputs actually start**. Merely arming, monitoring or waiting in charging auto-arm standby does not change the user's alarm volume. Restoration is ownership-aware and does not overwrite a manual user volume change.

## Security

- PBKDF2-HMAC-SHA256, 210,000 iterations;
- random 16-byte salt;
- Android Keystore HMAC key;
- constant-time verifier comparison;
- no raw PIN persistence;
- persistent escalating rate limit;
- malformed PIN attempts participate in rate limiting;
- PIN verification is serialized to prevent concurrent-attempt races;
- BIOMETRIC_STRONG is optional owner proof;
- ACTION_USER_PRESENT is never authentication;
- quick surfaces can arm only after activation-policy checks and cannot directly disarm.

## Data

No account, proprietary backend, analytics, ads, location collection, audio recording, photo capture or video capture exists in the current source tree. Sensor/power processing and event history are local.

PIN verifier data, runtime protection state, alarm-volume lease and event history are excluded from Android backup/device transfer. Non-sensitive preferences include presets, grace, per-mode output configuration and the opt-in charging auto-arm preference.

## Automated validation

The exact source SHA is gated by:

- JVM unit tests;
- release lint;
- debug APK;
- androidTest APK;
- release APK;
- release AAB;
- emulator instrumentation;
- source/release-truth guard.

The signed INTERNAL lane additionally verifies the APK/AAB signing certificate and generates artifact hashes.

## Certification boundary

0.9.3 is the expanded pre-1.0 software candidate.

Automated gates do not certify physical Charging, Tile/widget launcher behavior, OEM background survival, real flash/volume/DND behavior or sensor false-positive/negative behavior. Those remain part of the exact-artifact physical matrix.

Phone Guard must not be labelled fully CERTIFIED until the physical Pixel/Samsung/Xiaomi evidence is complete.
