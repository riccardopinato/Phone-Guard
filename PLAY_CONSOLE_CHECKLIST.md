# PLAY_CONSOLE_CHECKLIST.md — Phone Guard

Target release candidate: `0.9.0` / versionCode `2`.

## Target API
- [x] `targetSdk = 37`, above the Google Play requirement for new apps/updates from 31 August 2026 (API 36+).
- [x] `compileSdk = 37`.

Reference:
https://support.google.com/googleplay/android-developer/answer/11926878

## Foreground Service declaration
Manifest implementation:
- [x] `FOREGROUND_SERVICE`
- [x] `FOREGROUND_SERVICE_SPECIAL_USE`
- [x] `android:foregroundServiceType="specialUse"`
- [x] `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
- [x] one authoritative `ProtectionForegroundService`
- [x] persistent user-visible notification
- [x] user-initiated arming and user-controlled disarming

Suggested Play Console special-use explanation:

> Phone Guard is a user-initiated anti-theft protection utility. While the user explicitly arms Table, Pocket, Bag or Journey protection, one visible foreground service continuously monitors on-device motion/proximity/light sensors so movement or removal can be detected while the screen is off or the app is not visible. Interrupting or deferring this monitoring would break the protection the user explicitly started. The user can stop the service by disarming protection in the app.

Manual Play action:
- [ ] Policy > App content > Foreground service types: submit the declaration and reviewer evidence.

References:
https://developer.android.com/develop/background-work/services/fgs/service-types
https://support.google.com/googleplay/android-developer/answer/13392821

## Permissions
- [x] CAMERA: torch/LED only; no image/video capture.
- [x] POST_NOTIFICATIONS: visible protection/alarm notification.
- [x] WAKE_LOCK: active user-armed sensor monitoring.
- [x] VIBRATE: configured alarm output.
- [x] MODIFY_AUDIO_SETTINGS: reversible alarm-volume lease.
- [x] USE_BIOMETRIC: optional strong owner verification.
- [x] RECEIVE_BOOT_COMPLETED: reconcile an orphaned volume lease after reboot; it does not auto-arm protection.

## Data Safety — current source tree
Current app behavior:
- no account/login;
- no proprietary backend;
- no analytics/ads SDK;
- no location permission;
- no audio recording;
- no photo/video capture;
- sensor evidence is processed locally;
- event history is local;
- PIN verifier, runtime session, event history and volume lease are excluded from Android backup.

Google Play states that user data accessed only on-device and not sent off-device is not considered collected for Data Safety.

Reference:
https://support.google.com/googleplay/android-developer/answer/10787469

Manual Play action:
- [ ] Complete Data Safety from the exact release source tree; re-audit if any analytics, ads, crash reporting, account or network SDK is later added.

## Privacy / store
- [x] In-app Info & Privacy explains sensors, camera/torch, backup, foreground service and limitations.
- [ ] Publish a public privacy-policy URL and add it to Play Console/store listing.
- [ ] Prepare screenshots/store description/icon/feature graphic.
- [ ] Complete content rating and target audience forms.

## Play App Signing
- [ ] Enroll the new Play app with a Google-generated app-signing key unique to Phone Guard.
- [ ] Register the controlled upload certificate used by the signed INTERNAL lane.
- [ ] Record Play app-signing and upload SHA-256 certificates after enrollment.
