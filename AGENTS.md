# AGENTS.md — Phone Guard

## Factory baseline
- App Factory Core commit: `7b84e33df715cf2a48bda7c7495d0690819bff0f`
- Master: v26.6
- Golden Index: v23
- Platform: Android native only — Kotlin + Jetpack Compose.
- Do not introduce iOS or Flutter into this repository.

## Product invariants
- One authoritative foreground service: `ProtectionForegroundService`.
- Modes: Table, Pocket, Bag, Journey.
- No UI may claim protection when runtime/capability health cannot support it.
- OWNER_CHECK and TRIGGERED always outrank Journey expiry.
- Alarm outputs are best-effort capabilities; diagnostics must report effective state.
- Local-first/offline-first. No account is required.
- PIN verifier, runtime session, volume lease and event history stay out of Android backup.
- Never weaken Table reliability to simplify Pocket/Bag/Journey.

## Security invariants
- PIN: PBKDF2-HMAC-SHA256 + Android Keystore HMAC.
- Never persist raw PIN.
- No automatic disarm from ACTION_USER_PRESENT.
- PIN rate limiting must remain persistent.
- Invalid/missing Keystore state blocks new arming.
- Biometric owner proof must be BIOMETRIC_STRONG.

## Validation
Before merge:
```
gradle :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease --no-daemon --stacktrace
```
PR CI must also pass Android instrumentation on the emulator job.

## Release truth
- A green emulator/CI run is not physical OEM certification.
- Physical Samsung/Xiaomi/Pixel evidence is required before marking a release CERTIFIED.
- Never claim force-stop, OEM background survival, DND bypass, flash or volume behavior is guaranteed when Android/OEM policy can prevent it.
