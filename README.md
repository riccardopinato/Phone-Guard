# Phone Guard

Phone Guard is a standalone native Android anti-theft / anti-tampering utility with five protection modes: Table, Pocket, Bag, Charging and Journey.

**Version truth: 0.9.8 / versionCode 5**

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
- **Charging** — power-disconnect protection for public or unattended charging.
- **Journey** — time-bounded Pocket or Bag protection.

All modes run through one authoritative `ProtectionForegroundService`.

## Reliability invariants

- local-first / offline-first;
- capability support must match a reachable detector path;
- Pocket/Bag calibration requires live evidence from required sensors;
- one foreground-service owner;
- Journey expiry never disarms OWNER_CHECK or TRIGGERED;
- within one boot, Journey duration is governed by monotonic elapsed time so wall-clock changes cannot alter the session;
- wake-lock use is capability-aware; when a CPU lease is required it is bounded and renewed rather than acquired indefinitely;
- no UI may claim protection without a real active runtime state;
- force-stop, OEM background restrictions and hardware limitations are never represented as guaranteed-survival cases.

## Alarm escalation

Alarm outputs are owned by one `AlarmOutputController`:

- siren;
- vibration;
- configurable LED strobe, 0.5–3.0 Hz;
- alarm-stream target volume;
- warning screen and temporary brightness.

The alarm-volume lease is acquired **only when alarm outputs actually start**. Merely arming or monitoring protection does not change the user's alarm volume. Restoration is ownership-aware and does not overwrite a manual user volume change.

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
- ACTION_USER_PRESENT is never authentication.

## Data

No account, proprietary backend, analytics, ads, location collection, audio recording, photo capture or video capture exists in the current source tree. Sensor processing and event history are local.

PIN verifier data, runtime protection state, alarm-volume lease and event history are excluded from Android backup/device transfer.

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

0.9.8 is the pre-1.0 commercial-expansion candidate. It adds Charging Guard, configurable grace before alarm, quick presets, per-mode generated sound profiles, Quick Settings / widget / launcher quick-arm surfaces, guided charging auto-arm and structured trigger evidence.

Software can be accepted only when the exact candidate SHA passes all automated gates and the signed INTERNAL lane. Physical Pixel/Samsung/Xiaomi certification remains a separate evidence gate. Google Play Console work is intentionally deferred until the product is otherwise ready.

Phone Guard must not be labelled fully CERTIFIED until physical-device evidence is complete.
