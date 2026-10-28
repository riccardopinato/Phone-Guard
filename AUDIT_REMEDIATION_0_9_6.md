# Phone Guard P0/P1/P2 audit remediation — 0.9.6

Audit source: 0.9.5 main @ 0939be0d12bb944863e5dfb5a6913af210a51a93. App Factory baseline: 7b84e33df715, Master v26.6, Golden Index v23.

## Implemented in this PR (CI validation pending)
- P0: malformed / negative motion, light, proximity and gyroscope samples are rejected before Auto Guard selection. Stabilizer discards invalid time and evidence score. JVM tests cover counterexamples.
- P0: charging auto-arm reconciles persisted protection before entering standby; initial foreground notification reads Preparing, not Protected.
- P1: Smart Arm appears higher in the Home layout. Technical controls remain available but are collapsed behind localized advanced disclosure. No feature is removed.
- P1: accurate six-mode onboarding in EN/IT/ES/FR/PT, including Accessory.
- P2: reviewed very large ProtectionForegroundService and AlarmScreen for future bounded extraction; retained existing owner/state machine to avoid a risky rewrite without physical baseline.

## Open external P0/P1 gates (cannot be marked fixed by CI)
- Pixel, Samsung and Xiaomi/Redmi physical matrix: six modes, owner grace, charging and audio-device disconnect, PIN/biometric, alarm outputs, flash/volume/DND, force-stop and reboot/Doze.
- Quantitative false-alarms / misses / classification precision and recall; battery ON/OFF paired eight-hour test per OEM.
- Signed APK exact-artifact size and visual/on-device QA; v0.9.5 2.73 MB is reference supplied by owner and is not verified for v0.9.6.
- Google Play specialUse foreground-service declaration and optional Activity Recognition policy; Play approval, Data Safety and public store listing.

## P2 candidates not silently introduced
- Intruder Selfie changes CAMERA from torch-only to image capture and requires explicit product decision, storage lifecycle, disclosure and privacy review.
- Trusted Bluetooth devices require connection spoofing/threat-model and false disconnect QA. Existing Accessory Guard must remain safe.
- Broad module extraction is deferred until runtime/physical evidence allows behavioral regression detection.

## Evidence truth
Implemented source != CI GREEN != certified on-device != Play accepted.

## P1 product-truth update
- Disarmed hardware status explicitly says NOT PROTECTED, regardless of sensor presence.
- Five-locale privacy/foreground-service explanation now discloses opt-in standby after a manual disarm.
