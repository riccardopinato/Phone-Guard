# PHYSICAL_QA_MATRIX.md — Phone Guard

Candidate software line: **0.9.8 / versionCode 5**


Status: **UNVERIFIED** until measurements from real devices are recorded.

Minimum device matrix:
- Google Pixel (recent Android reference build)
- Samsung Galaxy (One UI)
- Xiaomi/Redmi (HyperOS/MIUI background-management family)

For every row record: device/model, Android/OEM version, build SHA, PASS/FAIL, notes, reproduction evidence.

| Test | Pixel | Samsung | Xiaomi | Required result |
|---|---|---|---|---|
| Table arm/calibration | UNVERIFIED | UNVERIFIED | UNVERIFIED | ARMED only after valid calibration |
| Table intentional lift trigger | UNVERIFIED | UNVERIFIED | UNVERIFIED | alarm reliably triggers |
| Table idle false-positive 30 min | UNVERIFIED | UNVERIFIED | UNVERIFIED | zero false alarms |
| Pocket stable insert | UNVERIFIED | UNVERIFIED | UNVERIFIED | POCKET_CONFIRMED |
| Pocket intentional removal | UNVERIFIED | UNVERIFIED | UNVERIFIED | OWNER_CHECK then alarm if unverified |
| Pocket normal owner removal | UNVERIFIED | UNVERIFIED | UNVERIFIED | owner can verify inside grace window |
| Bag light-capable profile | UNVERIFIED | UNVERIFIED | UNVERIFIED | detector reaches owner-check on removal |
| Bag gyro-capable profile | UNVERIFIED | UNVERIFIED | UNVERIFIED | detector reaches owner-check on removal |
| Unsupported sensor combination | UNVERIFIED | UNVERIFIED | UNVERIFIED | mode cannot be armed |
| Journey normal expiry | UNVERIFIED | UNVERIFIED | UNVERIFIED | auto-disarms only from non-critical state |
| Journey expiry during OWNER_CHECK | UNVERIFIED | UNVERIFIED | UNVERIFIED | does not disarm |
| Journey expiry during TRIGGERED | UNVERIFIED | UNVERIFIED | UNVERIFIED | does not disarm |
| Charging arm while connected | UNVERIFIED | UNVERIFIED | UNVERIFIED | reaches ARMED only with real power connected |
| Charging unplug trigger | UNVERIFIED | UNVERIFIED | UNVERIFIED | enters owner grace or alarm according to configured grace |
| Charging arm while disconnected | UNVERIFIED | UNVERIFIED | UNVERIFIED | refuses to arm; no false protected state |
| Charging process recovery with cable present | UNVERIFIED | UNVERIFIED | UNVERIFIED | charging monitor resumes |
| Charging process recovery with cable absent | UNVERIFIED | UNVERIFIED | UNVERIFIED | fails closed to alarm as possible disconnect |
| Pre-alarm 0 s | UNVERIFIED | UNVERIFIED | UNVERIFIED | qualifying trigger goes directly to alarm |
| Pre-alarm 2/5/8 s | UNVERIFIED | UNVERIFIED | UNVERIFIED | owner verification cancels before alarm; timeout escalates |
| Quick Settings tile from unlocked device | UNVERIFIED | UNVERIFIED | UNVERIFIED | arms last selected mode without bypassing PIN readiness |
| Quick Settings tile while protection active | UNVERIFIED | UNVERIFIED | UNVERIFIED | opens app; does not directly disarm |
| Home widget quick arm | UNVERIFIED | UNVERIFIED | UNVERIFIED | user tap arms last selected mode or opens app on blocked preconditions |
| Launcher shortcut quick arm | UNVERIFIED | UNVERIFIED | UNVERIFIED | user action arms last selected mode or opens app on blocked preconditions |
| Charging auto-arm while app process is alive | UNVERIFIED | UNVERIFIED | UNVERIFIED | runtime power receiver attempts direct arm; truthful tap-to-arm fallback when background FGS start is blocked |
| Charging connect after app process was killed | UNVERIFIED | UNVERIFIED | UNVERIFIED | no guarantee is claimed; feature resumes after app process is available again |
| Charging auto-arm with notification denied | UNVERIFIED | UNVERIFIED | UNVERIFIED | no false notification claim; app remains safe and disarmed if OS blocks start |
| Protection Check | UNVERIFIED | UNVERIFIED | UNVERIFIED | readiness matches actual sensor/power capabilities |
| Presets | UNVERIFIED | UNVERIFIED | UNVERIFIED | each preset applies intended mode/grace/sensitivity/profile atomically |
| Sound profiles SIREN/WARNING/BEEP | UNVERIFIED | UNVERIFIED | UNVERIFIED | each generated profile is audible and stop/release is clean |
| Trigger evidence history | UNVERIFIED | UNVERIFIED | UNVERIFIED | mode/reason/confidence/output health are locally recorded without sensitive capture |
| screen off 30 min | UNVERIFIED | UNVERIFIED | UNVERIFIED | monitoring remains healthy |
| Doze / idle | UNVERIFIED | UNVERIFIED | UNVERIFIED | expected protection behavior documented |
| app process killed by system | UNVERIFIED | UNVERIFIED | UNVERIFIED | persisted session restores safely |
| force-stop | UNVERIFIED | UNVERIFIED | UNVERIFIED | limitation disclosed; no false protected state |
| reboot with orphan volume lease | UNVERIFIED | UNVERIFIED | UNVERIFIED | owned volume restored |
| manual volume change while armed | UNVERIFIED | UNVERIFIED | UNVERIFIED | Phone Guard does not overwrite user change on release |
| alarm target volume apply/restore | UNVERIFIED | UNVERIFIED | UNVERIFIED | reversible on supported OEM |
| DND enabled | UNVERIFIED | UNVERIFIED | UNVERIFIED | effective limitation correctly reported |
| flash 0.5 Hz | UNVERIFIED | UNVERIFIED | UNVERIFIED | stable on/off and clean stop |
| flash 1 Hz | UNVERIFIED | UNVERIFIED | UNVERIFIED | stable on/off and clean stop |
| flash 2 Hz | UNVERIFIED | UNVERIFIED | UNVERIFIED | stable on/off and clean stop |
| flash 3 Hz | UNVERIFIED | UNVERIFIED | UNVERIFIED | stable on/off and clean stop |
| camera permission denied | UNVERIFIED | UNVERIFIED | UNVERIFIED | alarm continues; flash health unavailable |
| notification permission denied | UNVERIFIED | UNVERIFIED | UNVERIFIED | product truth/OS behavior documented |
| PIN failures 1–4 | UNVERIFIED | UNVERIFIED | UNVERIFIED | no lockout |
| PIN failure 5+ | UNVERIFIED | UNVERIFIED | UNVERIFIED | escalating lockout |
| strong biometric owner proof | UNVERIFIED | UNVERIFIED | UNVERIFIED | owner-check/disarm succeeds |
| ACTION_USER_PRESENT only | UNVERIFIED | UNVERIFIED | UNVERIFIED | does not authenticate |
| warning screen enabled | UNVERIFIED | UNVERIFIED | UNVERIFIED | red pulse + temporary brightness only while visible |
| warning screen disabled | UNVERIFIED | UNVERIFIED | UNVERIFIED | normal alarm UI, no forced brightness |

Certification rule:
- any P0 reliability/security failure blocks RC certification;
- false negatives in the advertised supported Pocket/Bag matrix block certification;
- OEM-specific limitations may be accepted only when surfaced truthfully in UI/docs and not represented as guaranteed capability.
