# PHYSICAL_QA_MATRIX.md — Phone Guard

Candidate software line: **0.9.3 / versionCode 5**


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


## Charging Guard / quick surfaces — 0.9.3

Run on the same signed artifact used for release evidence.

| Scenario | Pixel | Samsung | Xiaomi/Redmi | Expected |
|---|---|---|---|---|
| Manual Charging arm while unplugged | ⬜ | ⬜ | ⬜ | blocked truthfully |
| Manual Charging arm while plugged | ⬜ | ⬜ | ⬜ | reaches ARMED |
| Cable removal + 0s grace | ⬜ | ⬜ | ⬜ | immediate alarm |
| Cable removal + 2/5/8s grace | ⬜ | ⬜ | ⬜ | OWNER_CHECK then alarm |
| Owner verifies intentional unplug | ⬜ | ⬜ | ⬜ | Charging session ends cleanly |
| Auto-arm enabled while unplugged | ⬜ | ⬜ | ⬜ | visible standby, next plug arms |
| Auto-arm enabled while already plugged | ⬜ | ⬜ | ⬜ | requires fresh unplug/replug |
| Auto-arm after screen-off/Doze | ⬜ | ⬜ | ⬜ | fresh plug still handled or limitation surfaced |
| Auto-arm after reboot | ⬜ | ⬜ | ⬜ | explicit preference restores visible standby |
| Quick Tile from unlocked device | ⬜ | ⬜ | ⬜ | arms last mode if all gates pass |
| Quick Tile while protection active | ⬜ | ⬜ | ⬜ | opens app, never direct-disarms |
| Quick Tile from lock screen | ⬜ | ⬜ | ⬜ | secure behavior, no auth bypass |
| Widget arm | ⬜ | ⬜ | ⬜ | same activation gates as app |
| Static shortcut arm | ⬜ | ⬜ | ⬜ | same activation gates as app |
| Per-mode output profile | ⬜ | ⬜ | ⬜ | selected mode uses its own profile |
| Protection Check | ⬜ | ⬜ | ⬜ | report matches real device state |
| Event evidence after trigger | ⬜ | ⬜ | ⬜ | mode/source/detector/output truth recorded |

### False-positive / recovery additions
- [ ] unplug/replug bounce does not generate duplicate alarms;
- [ ] quick repeated power broadcasts do not create duplicate arming sessions;
- [ ] service/process interruption while Charging and still plugged restores monitor;
- [ ] service/process interruption while Charging and unplugged fails closed to alarm;
- [ ] disabling charging auto-arm stops standby when no active protection exists;
- [ ] disabling auto-arm never disarms an already active protection session.
