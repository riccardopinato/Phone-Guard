# Phone Guard

Phone Guard is a standalone native Android anti-theft / anti-tampering suite assembled from proven internal donor capabilities while remaining commercially and technically independent from those donor apps.

## Factory baseline

- App Factory Core: `7b84e33df715cf2a48bda7c7495d0690819bff0f`
- Master: v26.6
- Golden Index: v23
- Tabletop donor: `riccardopinato/Tabletop-Motion-Alarm@9e67bcbd84e8add7d183349644d3f6f6bbe9ffe0`
- Pocket Guard / Pockara donor: `riccardopinato/Pocket-Guard@b7b1c74de66e047c32083365b44561e42237b4fc`
- Luminous Circle: pattern donor for hardened strobe / visual escalation; canonical source commit not available.

## Protection modes

- **Table** — calibrated motion protection inherited from the mature Tabletop reliability core.
- **Pocket** — proximity/light/motion/rotation sensor fusion with owner verification.
- **Bag** — bag/backpack storage and removal profile using the Pocket Guard fusion engine.
- **Journey** — time-bounded Pocket or Bag session with persistence, recovery and automatic expiry.

All modes run through **one authoritative `ProtectionForegroundService`**. There is no parallel Table-only or Pocket-only foreground service.

## Reliability invariants

- local-first / offline-first;
- no required account or backend;
- one foreground-service owner for an active protection session;
- persisted protection mode and recovery state;
- Table calibration recovery;
- Pocket/Bag calibration recovery;
- owner-check recovery fails closed to alarm;
- expired Journey sessions do not silently re-arm;
- adaptive Pocket/Bag sensor sampling;
- capability gating before arming;
- no UI "Protected" claim without a real active runtime state;
- Table behavior remains regression-protected.

## Alarm escalation

All protection modes share one alarm escalation layer:

- siren;
- vibration;
- configurable LED strobe (0.5–10 Hz);
- configurable alarm-stream target volume with previous-value restore;
- high-visibility red WARNING screen;
- temporary screen brightness escalation;
- PIN and optional strong biometric owner authentication.

Android/OEM/DND restrictions are treated as capability/health constraints rather than pretending an absolute override is always possible.

## Security and data

- local PIN verifier uses PBKDF2-HMAC-SHA256 plus an Android Keystore-backed HMAC key;
- raw PIN is never persisted;
- security DataStore is excluded from Android backup/device transfer;
- active protection session state is excluded from backup/device transfer;
- event history remains local and excluded from backup;
- non-sensitive Phone Guard settings may use Android backup.

## Validation

The branch is gated by GitHub Actions with:

- JVM unit tests;
- release lint;
- debug APK assembly;
- release APK assembly;
- release AAB assembly.

Physical-device/OEM certification remains a separate release gate.
