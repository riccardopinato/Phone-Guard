# PLAY_CONSOLE_CHECKLIST.md — Phone Guard

**Version truth: 0.9.3 / versionCode 5**

Status: **DEFERRED BY PRODUCT DECISION**. This file preserves the final publication work but it is not a substitute for current provider-policy verification at release time.

## Implemented source prerequisites
- [x] targetSdk 37 / compileSdk 37;
- [x] one authoritative `ProtectionForegroundService`;
- [x] `FOREGROUND_SERVICE`;
- [x] `FOREGROUND_SERVICE_SPECIAL_USE`;
- [x] `android:foregroundServiceType="specialUse"`;
- [x] special-use subtype property describes sensor/power anti-theft monitoring and opt-in charging standby;
- [x] normal protection is user initiated;
- [x] charging auto-arm is explicit user opt-in with a persistent visible foreground notification;
- [x] CAMERA used only for torch/flash;
- [x] POST_NOTIFICATIONS for visible protection/alarm/standby notification;
- [x] WAKE_LOCK for capability-dependent sensor protection only;
- [x] VIBRATE;
- [x] MODIFY_AUDIO_SETTINGS with reversible alarm-only lease;
- [x] USE_BIOMETRIC;
- [x] RECEIVE_BOOT_COMPLETED for stale side-effect reconciliation and explicit charging-auto-arm preference restoration;
- [x] Quick Settings tile uses system `BIND_QUICK_SETTINGS_TILE`;
- [x] widget/shortcut add no new sensitive runtime permission;
- [x] no account/backend/analytics/ads/location/audio-recording/photo/video capture;
- [x] sensitive/runtime/event/lease data excluded from backup.

## Publication checks to re-verify immediately before submission
- [ ] special-use foreground-service declaration remains allowed for this exact anti-theft use case;
- [ ] reviewer video demonstrates normal protection and charging auto-arm standby;
- [ ] foreground-service description exactly matches the production manifest and UX;
- [ ] Data Safety matches the exact production source tree;
- [ ] public privacy-policy URL;
- [ ] screenshots include Charging Guard, Protection Check and quick activation surfaces;
- [ ] content rating and target audience;
- [ ] Play App Signing;
- [ ] controlled upload certificate;
- [ ] Play app-signing and upload certificate SHA-256 values;
- [ ] current Android/Play policy review for boot-restored specialUse FGS.

## Signing decision
Use Play App Signing with a Google-generated app-signing key unique to Phone Guard. Keep the controlled INTERNAL/upload identity separate.

Official references in `SIGNING_IDENTITY.md` must be re-verified immediately before publication because Play requirements can change.
