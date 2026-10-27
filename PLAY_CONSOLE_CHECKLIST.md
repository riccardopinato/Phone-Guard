# PLAY_CONSOLE_CHECKLIST.md — Phone Guard

**Version truth: 0.9.2 / versionCode 4**

Status: **DEFERRED BY PRODUCT DECISION**. This file preserves the final publication work but it is not part of the current software-hardening gate.

## Implemented source prerequisites
- [x] targetSdk 37 / compileSdk 37;
- [x] one authoritative `ProtectionForegroundService`;
- [x] `FOREGROUND_SERVICE`;
- [x] `FOREGROUND_SERVICE_SPECIAL_USE`;
- [x] `android:foregroundServiceType="specialUse"`;
- [x] special-use subtype property;
- [x] user-initiated arming and user-controlled disarming;
- [x] CAMERA used only for torch/flash;
- [x] POST_NOTIFICATIONS for visible protection/alarm notification;
- [x] WAKE_LOCK for capability-dependent active protection;
- [x] VIBRATE;
- [x] MODIFY_AUDIO_SETTINGS with reversible alarm-only lease;
- [x] USE_BIOMETRIC;
- [x] RECEIVE_BOOT_COMPLETED only for stale side-effect reconciliation;
- [x] no account/backend/analytics/ads/location/audio-recording/photo/video capture in current source;
- [x] sensitive/runtime/event/lease data excluded from backup.

## Deferred Play Console actions
- [ ] submit foreground-service declaration and reviewer evidence/video;
- [ ] complete Data Safety from the exact production source tree;
- [ ] publish public privacy-policy URL;
- [ ] prepare store listing/screenshots/icon/feature graphic;
- [ ] complete content rating and target-audience forms;
- [ ] enroll in Play App Signing;
- [ ] register controlled upload certificate;
- [ ] record Play app-signing and upload certificate SHA-256 values.

## Signing decision
Use Play App Signing with a Google-generated app-signing key unique to Phone Guard. Keep the controlled INTERNAL/upload identity separate.

Official references are intentionally retained in `SIGNING_IDENTITY.md` and should be re-verified immediately before publication because Play requirements can change.
