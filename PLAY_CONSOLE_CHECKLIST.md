# PLAY_CONSOLE_CHECKLIST.md — Phone Guard

**Version truth: 0.9.7 / versionCode 9**

Status: **DEFERRED BY PRODUCT DECISION**. This file preserves the final publication work but it is not a substitute for current provider-policy verification at release time.

## Implemented source prerequisites
- [x] targetSdk 37 / compileSdk 37;
- [x] one authoritative `ProtectionForegroundService`;
- [x] `FOREGROUND_SERVICE`;
- [x] `FOREGROUND_SERVICE_SPECIAL_USE`;
- [x] `android:foregroundServiceType="specialUse"`;
- [x] special-use subtype property describes sensor/power/audio-accessory-state anti-theft monitoring, short user-initiated Smart Arm sampling and opt-in charging plus Adaptive Auto Guard standby;
- [x] normal protection is user initiated;
- [x] charging auto-arm is explicit user opt-in with a persistent visible foreground notification;
- [x] Adaptive Auto Guard is explicit opt-in, foreground-notified, local, and does not use location;
- [x] optional ACTIVITY_RECOGNITION permission is requested only for transport context and does not imply GPS or location collection;
- [x] CAMERA used only for torch/flash;
- [x] POST_NOTIFICATIONS for visible protection/alarm/standby notification;
- [x] WAKE_LOCK for capability-dependent sensor protection only;
- [x] VIBRATE;
- [x] MODIFY_AUDIO_SETTINGS with reversible alarm-only lease;
- [x] USE_BIOMETRIC;
- [x] RECEIVE_BOOT_COMPLETED for stale side-effect reconciliation and explicit charging/Adaptive Auto Guard preference restoration;
- [x] Quick Settings tile uses system `BIND_QUICK_SETTINGS_TILE`;
- [x] widget/shortcut add no new sensitive runtime permission;
- [x] Accessory Guard observes audio-output connection state only and does not request RECORD_AUDIO;
- [x] custom alarm sound uses a user-selected Android document URI and does not upload media;
- [x] no account/backend/analytics/ads/location/audio-recording/photo/video capture;
- [x] sensitive/runtime/event/lease data excluded from backup.

## Publication checks to re-verify immediately before submission
- [ ] special-use foreground-service declaration remains allowed for this exact anti-theft use case;
- [ ] reviewer video demonstrates normal protection and both opt-in standby modes;
- [ ] policy review covers Activity Recognition purpose, background processing and user consent;
- [ ] foreground-service description exactly matches the production manifest and UX, including Accessory state, Smart Arm sampling and Adaptive sensor/Activity Recognition standby;
- [ ] Data Safety matches the exact production source tree;
- [ ] public privacy-policy URL;
- [ ] screenshots include Charging Guard, Accessory Guard, Smart Arm, Protection Center and quick activation surfaces;
- [ ] content rating and target audience;
- [ ] Play App Signing;
- [ ] controlled upload certificate;
- [ ] Play app-signing and upload certificate SHA-256 values;
- [ ] current Android/Play policy review for boot-restored specialUse FGS.

## Signing decision
Use Play App Signing with a Google-generated app-signing key unique to Phone Guard. Keep the controlled INTERNAL/upload identity separate.

Official references in `SIGNING_IDENTITY.md` must be re-verified immediately before publication because Play requirements can change.


## 0.9.7 provider declaration draft package
- See PLAY_FGS_REVIEW_DRAFT_0_9_7.md, STORE_LISTING_DRAFT_0_9_7.md and PRIVACY_POLICY_DRAFT_0_9_7.md.
- These are draft text, not published settings, official reviewer approval or a public privacy-policy URL.
- Latest official specialUse rules: https://developer.android.com/develop/background-work/services/fgs/service-types; Play Console FGS review: https://support.google.com/googleplay/android-developer/answer/13392821?hl=en
