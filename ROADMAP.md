# ROADMAP.md — Phone Guard v1 hardening

Baseline branch: `roadmap/v1-hardening`

## Step 1 — Detection Correctness Hardening
Status: IMPLEMENTED — pending final PR CI.
- Capability truth aligned with reachable detector states.
- Pocket: accelerometer + proximity.
- Bag: accelerometer + light or gyroscope.
- Evidence score normalized to sensors actually available.
- Calibration records sensor capability independently from measured noise.
- Degraded sensor tests added.

## Step 2 — Journey Safety Contract
Status: IMPLEMENTED — pending final PR CI.
- Journey expiry cannot disarm OWNER_CHECK or TRIGGERED.
- Alarm recovery outranks Journey expiry.
- Owner verification can resume/finish the remaining Journey timer.

## Step 3 — System Side-Effect Recovery
Status: IMPLEMENTED — pending final PR CI.
- Persistent alarm-volume lease.
- User manual volume changes are not overwritten on release.
- Startup and boot reconciliation.
- Stale/no-session recovery restores owned volume.

## Step 4 — Security v2
Status: IMPLEMENTED — pending final PR CI.
- SecurityHealth states.
- Persistent escalating PIN rate limit.
- Missing/corrupt Keystore verifier blocks arming.
- ACTION_USER_PRESENT no longer authenticates; it only extends owner-check time.
- Owner check default grace window: 8 s, subject to physical tuning.

## Step 5 — Alarm Safety & Truth
Status: IMPLEMENTED — pending final PR CI.
- Flash limited to 0.5–3.0 Hz.
- Alarm output runtime health.
- DND/volume diagnostics.
- Warning-screen toggle controls red pulse/brightness truthfully.

## Step 6 — Automated Reliability QA
Status: IMPLEMENTED — pending final PR CI.
- JVM gate retained.
- androidTest compilation added.
- Emulator CI added.
- Real Keystore, volume lease, FGS manifest, settings clamp and launch smoke tests.

## Step 7 — Product/UI Cleanup
Status: IN PROGRESS.
- Remove donor branding/comments.
- Phone Guard theme naming.
- Multimode onboarding/privacy.
- Centralized Android resources with en fallback + it/es/fr/pt.
- AGENTS/ROADMAP/provenance docs.

## Step 8 — Release Engineering & Play Readiness
Status: TODO.
- Rename INTERNAL workflow/artifacts.
- Signed INTERNAL artifact gate.
- Signing identity decision.
- Play FGS declaration evidence, Data Safety, release checklist.

## Step 9 — Physical Certification / RC
Status: BLOCKED ON PHYSICAL DEVICE EVIDENCE.
Required matrix: Pixel + Samsung + Xiaomi; screen off, process/service kill, Doze, OEM battery restrictions, Pocket/Bag false positives/negatives, Journey expiry boundary, flash, volume lease, DND, owner-check timing, warning UI.
