# Phone Guard 0.9.7 — Android/Play foreground service reviewer package
Status: DRAFT. Source-level checklist, NOT provider approval or device certification.

## Exact code/manifest declaration
Service: com.riccardopinato.phoneguard.service.ProtectionForegroundService
Manifest: FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE; android:foregroundServiceType="specialUse" and PROPERTY_SPECIAL_USE_FGS_SUBTYPE with an explicit sensor/power/accessory/opt-in standby description.
One foreground owner manages live Table, Pocket, Bag, Charging, Accessory and Journey sessions. Active user protection uses visible notification. Optional user-enabled charging auto-arm and Adaptive Auto Guard use visible foreground standby.
The engine does not use location/GPS or record audio/video/photos. CAMERA is for torch/flash only. Activity Recognition is optional for local motion context; analyze Google Play services SDK data behavior before completing Data Safety.

## Evidence videos to RECORD on real hardware (not available here)
A. Configure owner PIN. Select Table, arm, show persistent notification, trigger owner-check/alarm, authenticate and disarm. Show that active session is not automatically ended by unlock alone.
B. Explicitly enable charging auto-arm. Show notification and disconnected waiting state, connect external power and observe state transition.
C. Explicitly enable Auto Guard with screen off. Show visible waiting/checking standby, demonstrate genuine activation in a controlled situation, show opt-out and no automatic change to existing armed session.
D. Explicitly deny optional permission and show degraded or blocked capability, without false 'Protected' claim.
Do not stage claims of protection under force-stop, DND override, 99% accuracy, low battery impact or guaranteed OEM background survival.

## Policy decision items
- Review specialUse eligibility with Play Console; merely compiling the manifest does NOT imply approval.
- Review specialUse, background/boot start and standby versus user-initiated necessity under the current targetSDK.
- Review background execution, user-perceptible notification and termination affordances.
- Review ACTIVITY_RECOGNITION rationale and SDK's own data disclosures.
- Check Data Safety, privacy URL, screenshots, content rating, upload/app signing, and support contact.
- Make no declaration of zero data collection without auditing third-party SDK disclosures.

References: 
https://developer.android.com/develop/background-work/services/fgs/service-types
https://support.google.com/googleplay/android-developer/answer/13392821?hl=en
https://support.google.com/googleplay/android-developer/answer/18258653?hl=en
