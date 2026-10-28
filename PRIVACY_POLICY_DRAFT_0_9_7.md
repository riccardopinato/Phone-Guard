# Phone Guard — Privacy policy DRAFT (0.9.7)
NOT published; add the genuine developer contact, distribution details and policy URL before Play submission.

## Scope
Phone Guard is a local Android anti-tampering alarm. No account or proprietary server is required for core functionality. App settings, incident events, owner authentication state and on-device sensor observations are used for local security features.

## Data processed on-device
Motion, proximity, light, rotation and, if the user selects Charging or Accessory, power/audio-output connection state are used to evaluate arming and triggers. The app does not record microphone content or capture photos/videos. A permission for CAMERA can be requested solely for Android torch/flash output. Optional activity context (walk/run/bicycle/vehicle/still) uses Google Play services Activity Recognition with separate consent; it is not GPS/location and has third-party SDK disclosures to evaluate.

A local event history can contain timestamps, protection mode, trigger reason, device sensor summary, alert output information and session boundaries. Events remain in app-local storage and can be deleted by the user. Sharing an incident text report explicitly hands that text to the receiving Android app chosen by the user.

The PIN itself is not stored in plaintext. The verifier uses PBKDF2 and Android Keystore. Sensitive data, event history and active session state are excluded from Android backup/transfer; some non-sensitive preferences may be backed up by Android.

## Network and third parties
No advertising SDK, product analytics, proprietary backend or tracking location feature is present in the audited source tree. Google Play services Activity Recognition is an optional dependency and may have separate data handling that must be reviewed for Play Data Safety; do not claim all SDK traffic is absent.

## Limitations and control
The user can disable optional Auto Guard and charging auto-arm. An ongoing authenticated protection session is not silently disarmed by disabling standby. Android and OEM restrictions can suspend monitoring. The app cannot prevent power-off, force-stop or physical theft.

## Before publication
- Identify developer/controller and working support/privacy contact (do not invent).
- Determine geographic scope, lawful bases and necessary statutory notices.
- Confirm current Play Data Safety responses against the final APK/AAB and all SDKs.
- Host a public URL, document effective date, respond to user deletion/privacy requests.
