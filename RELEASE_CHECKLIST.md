# RELEASE_CHECKLIST.md — Phone Guard

## Automated gate
- [ ] PR exact-head Development checks PASS.
- [ ] JVM unit tests PASS.
- [ ] release lint PASS.
- [ ] debug APK PASS.
- [ ] androidTest APK PASS.
- [ ] release APK PASS.
- [ ] release AAB PASS.
- [ ] emulator instrumentation PASS.

## Signed INTERNAL gate
- [ ] `Build signed INTERNAL` PASS from the exact release SHA.
- [ ] APK certificate equals `SIGNING_IDENTITY.md`.
- [ ] AAB certificate equals `SIGNING_IDENTITY.md`.
- [ ] `SHA256SUMS.txt` generated.
- [ ] `BUILD_IDENTITY.txt` records source SHA, workflow run and certificate.
- [ ] signed artifacts downloaded and smoke-installed on at least one real device.

## Security/reliability
- [x] one foreground-service owner.
- [x] capability truth blocks unreachable detector modes.
- [x] Journey expiry cannot disarm OWNER_CHECK/TRIGGERED.
- [x] PIN rate limiting is persistent.
- [x] invalid/missing Keystore state blocks new arming.
- [x] ACTION_USER_PRESENT is not owner authentication.
- [x] persistent alarm-volume lease with stale recovery.
- [x] flash ceiling is 3 Hz.
- [x] alarm outputs expose effective runtime health.

## Product truth
- [x] donor branding removed from user-facing surfaces.
- [x] Android resources available for en/it/es/fr/pt.
- [x] Android locale config declares supported languages.
- [x] multimode onboarding/privacy.
- [x] AGENTS / ROADMAP / donor provenance present.

## Manual release gates
- [ ] Google Play FGS declaration accepted.
- [ ] Data Safety submitted.
- [ ] public privacy-policy URL live.
- [ ] Play App Signing configured.
- [ ] Physical OEM matrix PASS.
- [ ] final versionCode/versionName bumped from RC to production.
- [ ] production release notes prepared.

Phone Guard must not be labelled CERTIFIED until every manual release gate above has evidence.
