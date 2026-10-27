# RELEASE_STATUS.md

**Version truth: 0.9.2 / versionCode 4**

This file separates software implementation from certification evidence.

## Software hardening implemented
- unified Table/Pocket/Bag/Journey runtime;
- capability-truth gating;
- live-sample calibration evidence;
- monotonic Journey expiry within one boot;
- persistent volume lease applied only at real alarm activation;
- capability-aware bounded wake-lock policy;
- PIN security health, serialized verification and persistent rate limiting;
- malformed PIN attempt throttling;
- strong biometric owner proof;
- output health and 3 Hz flash ceiling;
- EN/IT/ES/FR/PT resource parity;
- accessibility touch-target cleanup;
- source/release-truth CI guard;
- JVM + emulator instrumentation gates;
- signed INTERNAL workflow with certificate verification.

## Evidence model
Software acceptance requires the **exact candidate SHA** to pass Development checks and the signed INTERNAL workflow. GitHub Actions is the canonical execution evidence; this source file intentionally does not embed a run ID that would become stale when the file itself changes.

## Not physically certified
Pixel/Samsung/Xiaomi runtime evidence remains UNVERIFIED. This includes false-positive/negative tuning, Doze/OEM behavior and real-device output/recovery behavior.

## Google Play
Play Console/store work is intentionally deferred until the final publication phase.

Release state: **SOFTWARE RC — NOT PHYSICALLY CERTIFIED — PLAY DEFERRED**.
