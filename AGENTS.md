# AGENTS.md — Phone Guard

## Factory baseline
- App Factory Core commit: `7b84e33df715cf2a48bda7c7495d0690819bff0f`
- Master: v26.6
- Golden Index: v23
- Platform: Android native only — Kotlin + Jetpack Compose.
- Do not introduce iOS or Flutter into this repository.

## Product invariants
- One authoritative foreground service: `ProtectionForegroundService`.
- Modes: Table, Pocket, Bag, Charging, Journey.
- No UI may claim protection when runtime/capability health cannot support it.
- Pocket/Bag calibration requires live evidence from required sensors; hardware presence alone is insufficient.
- Charging requires real external power at arm time and reacts to a real power-disconnect broadcast while its service is active.
- All entry surfaces (app, tile, widget, shortcut, charging auto-arm) must route through one activation policy; none may bypass SecurityHealth, capability or permission gates.
- Tile/widget/shortcut must never bypass PIN/strong-biometric disarm.
- Charging auto-arm is opt-in foreground standby with a visible notification; do not replace it with an unreliable implicit manifest power receiver.
- OWNER_CHECK and TRIGGERED always outrank Journey expiry.
- Journey uses elapsed-realtime deadline semantics within a boot; wall clock is display/fallback state only.
- Alarm outputs are best-effort capabilities; diagnostics must report effective state.
- Alarm-stream volume must not be raised during ordinary arming/monitoring; the lease begins only when alarm outputs start.
- Wake-lock use must be capability-aware. A required CPU lease must be bounded/renewed and released on every terminal path.
- Local-first/offline-first. No account is required.
- PIN verifier, runtime session, volume lease and event history stay out of Android backup.
- Never weaken Table reliability to simplify Pocket/Bag/Journey.

## Security invariants
- PIN: PBKDF2-HMAC-SHA256 + Android Keystore HMAC.
- Never persist raw PIN.
- PIN verification attempts are serialized.
- Malformed attempts count toward the same persistent rate limit.
- No automatic disarm from ACTION_USER_PRESENT.
- Invalid/missing Keystore state blocks new arming.
- Biometric owner proof must be BIOMETRIC_STRONG.

## Source truth
`scripts/verify-source-truth.sh` is a mandatory CI gate. It prevents release-version drift and regression to obsolete donor wake-lock or pre-trigger volume behavior.

## Validation
Before merge:
```
bash scripts/verify-source-truth.sh
gradle :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:bundleRelease --no-daemon --stacktrace
```

PR CI must also pass Android instrumentation.

## Release truth
- A green emulator/CI run is not physical OEM certification.
- Physical Samsung/Xiaomi/Pixel evidence is required before marking a release CERTIFIED.
- Google Play manual work is a separate final phase.
- Never claim force-stop, OEM background survival, DND bypass, flash or volume behavior is guaranteed when Android/OEM policy can prevent it.
