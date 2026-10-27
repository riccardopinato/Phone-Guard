# AGENTS.md — Phone Guard

## Factory baseline
- App Factory Core commit: `7b84e33df715cf2a48bda7c7495d0690819bff0f`
- Master: v26.6
- Golden Index: v23
- Platform: Android native only — Kotlin + Jetpack Compose.
- Do not introduce iOS or Flutter into this repository.

## Product invariants
- One authoritative foreground service: `ProtectionForegroundService`.
- Modes: Table, Pocket, Bag, Charging, Accessory, Journey.
- No UI may claim protection when runtime/capability health cannot support it.
- Pocket/Bag calibration requires live evidence from required sensors; hardware presence alone is insufficient.
- Charging requires real external power at arm time and reacts to a real power-disconnect broadcast while its service is active.
- Accessory Guard requires a real supported audio output at arm time, protects the devices present at arming, and must not request microphone recording.
- Smart Arm is deterministic context resolution only; it must route the selected mode through the same activation policy and never become an authentication path.
- Alarm-output duration may stop outputs but must never disarm, clear TRIGGERED, or satisfy owner authentication.
- All entry surfaces (app, Smart Arm, tile, widget, shortcut, charging auto-arm) must route through one activation policy; none may bypass SecurityHealth, capability or permission gates.
- Tile/widget/shortcut must never bypass PIN/strong-biometric disarm.
- Charging auto-arm is opt-in foreground standby with a visible notification; do not replace it with an unreliable implicit manifest power receiver.

- Adaptive Auto Guard 0.9.5 is implemented as software source in feature PR #9, not part of the previous signed 0.9.4 release and NOT yet physically certified. It may arm only with explicit user opt-in, visible single-service standby, supported sensor evidence, and a successful `ProtectionActivationCoordinator` decision.
- Automatic inactivity timeout is an evaluation trigger, never unconditional arming. Auto Table/Pocket/Bag must satisfy the same live-calibration and capability requirements as their manual paths. Light+proximity+movement is correlated evidence, not a measured 99% probability.
- Auto Guard OFF/PAUSED cannot silently restart; owner-authenticated manual disarm must not immediately rearm without a new qualifying context cycle. No context/screen event may authenticate or disarm OWNER_CHECK/TRIGGERED or mode-switch an active protection.
- Auto Guard and Charging auto-arm must share the existing foreground-service owner, serialized activation policy, and honest runtime/notification state. Foreground sensing and reboot recovery obey Android/OEM limits; force-stop survival is never guaranteed.
- Activity Recognition Transition is optional supporting evidence with explicit permission/fallback. Baseline Auto Guard requests no location permission and does not require GPS, Google Play services, account, microphone or network. All inference is local and power-bounded.
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
