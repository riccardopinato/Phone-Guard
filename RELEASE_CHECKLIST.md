# RELEASE_CHECKLIST.md — Phone Guard

Version truth: 0.9.4 / versionCode 6

## Exact-SHA automated gate
The release SHA must show all of the following green:
- [ ] source/release-truth guard;
- [ ] JVM unit tests;
- [ ] release lint;
- [ ] debug APK;
- [ ] androidTest APK;
- [ ] release APK;
- [ ] release AAB;
- [ ] emulator instrumentation.

## Signed INTERNAL gate
For the exact release SHA:
- [ ] `Build signed INTERNAL` PASS;
- [ ] APK certificate equals `SIGNING_IDENTITY.md`;
- [ ] AAB certificate equals `SIGNING_IDENTITY.md`;
- [ ] `SHA256SUMS.txt` generated;
- [ ] `BUILD_IDENTITY.txt` records source SHA, workflow run and certificate;
- [ ] signed APK smoke-installed on at least one real device.

## Software invariants
- [x] one foreground-service owner;
- [x] capability truth blocks unreachable modes;
- [x] calibration requires live required-sensor evidence;
- [x] Journey expiry cannot disarm OWNER_CHECK/TRIGGERED;
- [x] Journey timing is monotonic within a boot;
- [x] PIN rate limiting is persistent;
- [x] malformed PIN attempts are throttled;
- [x] PIN verification is serialized;
- [x] invalid/missing Keystore state blocks arming;
- [x] ACTION_USER_PRESENT is not owner authentication;
- [x] alarm volume is not raised during ordinary monitoring;
- [x] persistent volume lease has ownership-aware recovery;
- [x] wake-lock use is capability-aware and bounded;
- [x] flash ceiling is 3 Hz;
- [x] alarm outputs expose effective runtime health;
- [x] EN/IT/ES/FR/PT have complete string-key parity;
- [x] obsolete donor wake-lock/settings removed;
- [x] custom primary/secondary choices meet 48dp+ touch targets;
- [x] small bare-text click targets replaced by Material buttons.

## External physical certification
- [ ] Pixel matrix PASS;
- [ ] Samsung matrix PASS;
- [ ] Xiaomi/Redmi matrix PASS;
- [ ] Pocket/Bag false-negative acceptance criteria PASS;
- [ ] false-positive soak tests PASS;
- [ ] screen-off/Doze/OEM battery behavior documented;
- [ ] real alarm outputs/recovery verified.

## Google Play — intentionally deferred
- [ ] FGS declaration;
- [ ] Data Safety;
- [ ] public privacy-policy URL;
- [ ] Play App Signing;
- [ ] store listing/screenshots/feature graphic;
- [ ] content rating/target audience;
- [ ] production release notes.

Phone Guard must not be labelled fully CERTIFIED until the physical gate is complete. Play publication is a subsequent final phase.


## 0.9.3 Market Value Pack gates
- [ ] Charging Guard cable removal verified on physical device.
- [ ] Charging owner-grace cancel and alarm paths verified.
- [ ] Charging auto-arm standby verified across unplug/replug, screen-off and reboot.
- [ ] Quick Settings tile arm path verified; active tile never bypasses authentication.
- [ ] Home widget and static shortcut verified.
- [ ] Protection Check reports real capability/permission state.
- [ ] Per-mode alarm profiles verified independently.
- [ ] Structured event evidence verified after real triggers.
- [ ] EN/IT/ES/FR/PT UI sanity checked.


## 0.9.4 Market Value Pack 2 gates
- [ ] Accessory Guard wired/USB/Bluetooth removal verified on physical devices.
- [ ] Accessory owner-grace cancel and alarm paths verified.
- [ ] Smart Arm context selection checked in charging/accessory/pocket/bag/table contexts.
- [ ] Smart Arm still obeys security/capability/permission/recovery gates.
- [ ] Live Guard Test validated for all six protection paths.
- [ ] Sensitivity Advisor recommendations checked against stable/noisy real environments.
- [ ] Protection Center OEM/last-exit/system-setting actions verified.
- [ ] custom alarm URI persistence and built-in fallback verified.
- [ ] pre-alarm silent/vibration/beep behavior verified.
- [ ] 30s/1m/3m output timeout stops outputs only and never disarms.
- [ ] Incident Sessions and shared local reports verified.
- [ ] EN/IT/ES/FR/PT key parity and UI sanity checked.
