# ROADMAP.md — Phone Guard pre-1.0

Version truth: 0.9.8 / versionCode 5

## Step 1 — Detection correctness
Status: **DONE**
- capability truth aligned with reachable detector states;
- Pocket requires accelerometer + proximity;
- Bag requires accelerometer + light or gyroscope;
- extraction score normalized to available evidence;
- degraded paths regression-tested.

## Step 2 — Journey safety contract
Status: **DONE**
- OWNER_CHECK/TRIGGERED outrank Journey expiry;
- owner verification can resume the remaining Journey;
- active duration uses monotonic elapsed time within the same boot.

## Step 3 — System side-effect recovery
Status: **DONE**
- persistent alarm-volume lease;
- target volume applied only when alarm outputs start;
- user manual volume changes are not overwritten on release;
- startup/reboot stale-lease reconciliation.

## Step 4 — Security v2+
Status: **DONE**
- SecurityHealth states;
- PBKDF2 + Keystore HMAC;
- persistent escalating rate limit;
- malformed attempts are rate-limited;
- PIN verification serialized;
- ACTION_USER_PRESENT is not owner authentication;
- BIOMETRIC_STRONG optional owner proof.

## Step 5 — Alarm safety and truth
Status: **DONE**
- single alarm-output owner;
- sound/vibration/flash runtime health;
- DND/volume diagnostics;
- 0.5–3.0 Hz flash ceiling;
- warning-screen toggle truth.

## Step 6 — Automated reliability QA
Status: **DONE — exact SHA must remain green**
- JVM tests;
- release lint;
- debug/release APK;
- androidTest APK;
- release AAB;
- emulator instrumentation;
- source/release-truth CI guard.

## Step 7 — Product/UI/accessibility cleanup
Status: **DONE for software audit**
- donor-only settings removed;
- obsolete donor wake-lock removed;
- stable Phone Guard theme/branding;
- 48dp+ custom action targets;
- bare small text click targets replaced by Material text buttons;
- EN/IT/ES/FR/PT resource parity.

## Step 8 — Release engineering
Status: **DONE for software lane**
- signed INTERNAL APK/AAB workflow;
- certificate verification;
- artifact hashes;
- release identity documentation.

## Step 9 — Physical OEM certification
Status: **PENDING EXTERNAL EVIDENCE**
- Pixel;
- Samsung;
- Xiaomi/Redmi;
- false-positive/false-negative sensor tuning;
- screen-off/Doze/OEM background behavior;
- real flash/volume/DND/reboot behavior.

## Step 10 — Google Play
Status: **DEFERRED BY PRODUCT DECISION**
Play Console declarations, privacy-policy URL, Data Safety, Play App Signing and store assets will be handled after software/physical readiness.


## Step 8A — Commercial feature expansion
Status: **IMPLEMENTED — exact-head CI and physical validation remain separate gates**
- Charging Guard;
- configurable owner grace / pre-alarm;
- Protection Check mode readiness;
- structured event evidence;
- quick presets;
- per-mode generated sound profiles;
- Quick Settings tile;
- launcher shortcut;
- home-screen widget;
- charging auto-arm attempt with notification fallback when Android blocks background FGS start.

These additions reuse the existing protection service, security and alarm engines. Physical/OEM certification remains Step 9.
