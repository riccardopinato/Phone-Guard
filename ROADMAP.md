# ROADMAP.md — Phone Guard pre-1.0

Version truth: 0.9.7 / versionCode 9

## Foundation inherited from 0.9.2

Status: **DONE**
- Table/Pocket/Bag/Journey detector correctness;
- one authoritative foreground service;
- Journey safety and monotonic expiry;
- bounded capability-aware wake-locks;
- persistent alarm-volume lease and recovery;
- PBKDF2 + Android Keystore PIN verifier;
- persistent rate limiting and strong biometric owner proof;
- single alarm output owner;
- JVM/emulator/release gates;
- EN/IT/ES/FR/PT parity;
- signed INTERNAL lane.

## 0.9.3-A — Shared activation engine
Status: **IMPLEMENTED — exact-head validation required**
- `ProtectionActivationCoordinator`;
- one policy for app, tile, widget, shortcut and charging auto-arm;
- SecurityHealth, capability, charger and permission gates;
- no external quick surface can directly disarm.

## 0.9.3-B — Charging Guard
Status: **IMPLEMENTED — physical cable/OEM validation required**
- fifth `ProtectionMode.CHARGING`;
- arm only with external power connected;
- dynamic power disconnect monitoring while service is alive;
- owner grace before full alarm;
- same-session recovery;
- owner-approved unplug cleanly ends Charging protection;
- no sensor wake-lock needed.

## 0.9.3-C — Charging auto-arm
Status: **IMPLEMENTED — physical background/boot validation required**
- explicit opt-in;
- visible foreground standby;
- fresh charger connection arms Charging Guard;
- already-connected charger requires unplug/replug rather than silently claiming auto-arm;
- boot restores the opt-in standby;
- no manifest implicit power-receiver dependency.

## 0.9.3-D — Quick activation surfaces
Status: **IMPLEMENTED — launcher/device validation required**
- Quick Settings tile;
- home-screen widget;
- static launcher shortcut;
- last selected mode as the common quick-action contract;
- blocked quick activation falls back to opening/using the app rather than bypassing requirements.

## 0.9.3-E — Grace / false-positive UX
Status: **IMPLEMENTED — tuning pending physical matrix**
- configurable 0 / 2 / 5 / 8 second owner grace;
- Pocket/Bag owner-check uses the same product setting;
- Table/Charging use the shared pending-trigger path;
- immediate alarm remains available with 0 seconds.

## 0.9.3-F — Protection Presets
Status: **IMPLEMENTED**
- Café;
- Transit;
- Backpack;
- Public Charge;
- Travel;
- atomic reuse of existing mode/sensitivity/journey/alarm settings.

## 0.9.3-G — Per-mode alarm profiles
Status: **IMPLEMENTED**
- siren;
- vibration;
- flash;
- flash frequency;
- alarm target volume;
- warning screen;
- backward-compatible fallback to 0.9.2 global settings.

## 0.9.3-H — Protection Check
Status: **IMPLEMENTED — device truth still required**
- owner security;
- selected-mode support;
- sensor capability;
- notification state;
- flash/vibration configuration;
- volume/DND;
- battery-optimization status;
- external power when Charging;
- manual sound/vibration/flash checks.

## 0.9.3-I — Event Evidence 2.0
Status: **IMPLEMENTED**
- backward-compatible event JSON;
- optional structured evidence:
  mode, activation source, reason, confidence, detector state, sensor/power summary, output-health summary and grace window.

## 0.9.3-J — Automated regression
Status: **IN VALIDATION**
- Charging support policy;
- Charging no-wake-lock policy;
- Charging recovery;
- preset contract;
- per-mode settings persistence;
- manifest registration for Quick Tile and widget;
- existing 0.9.2 gates remain mandatory.

## 0.9.4-A — Accessory Guard
Status: **IMPLEMENTED — physical accessory/OEM validation required**
- sixth `ProtectionMode.ACCESSORY`;
- wired, USB, Bluetooth A2DP/BLE and hearing-aid audio-output presence through Android audio-device APIs;
- only devices present when protection begins are protected;
- accessory removal uses the shared owner-grace/alarm path;
- owner-approved intentional removal ends the session cleanly;
- persisted Accessory session recovery fails closed if the protected accessory is no longer present;
- no microphone recording and no Accessory monitoring CPU lease.

## 0.9.4-B — Smart Arm
Status: **IMPLEMENTED — context-quality validation required**
- deterministic, testable context resolver;
- priority: external power, connected audio accessory, Pocket evidence, Bag evidence, Table/fallback;
- short local sensor sample only;
- final mode always passes through `ProtectionActivationCoordinator`;
- no authentication, permission or recovery bypass.

## 0.9.4-C — Live Guard Test + Sensitivity Advisor
Status: **IMPLEMENTED — physical detector validation required**
- real, non-alarming trigger tests for Table/Pocket/Bag/Charging/Accessory/Journey;
- timeout means test failure rather than synthetic success;
- local motion/rotation noise analysis;
- Table and Pocket/Bag sensitivity recommendation;
- recommendation is never applied without explicit user action.

## 0.9.4-D — Protection Center
Status: **IMPLEMENTED — OEM guidance requires physical review**
- evolves Protection Check without removing existing checks;
- Accessory readiness;
- manufacturer-specific reliability guidance;
- Android `ApplicationExitInfo` last-exit diagnostics where supported;
- battery and Android security settings entry points;
- complementary Android Theft Protection / Remote Lock / Find Hub guidance;
- no claim that Phone Guard controls OEM/system theft features.

## 0.9.4-E — Advanced per-mode alarm profile
Status: **IMPLEMENTED**
- custom Android document audio URI with built-in-siren fallback;
- pre-alarm style: silent / vibration / beep;
- output duration: 30s / 1m / 3m / until disarmed;
- duration expiry stops outputs only; TRIGGERED and owner authentication remain authoritative;
- profile JSON remains backward compatible with 0.9.3.

## 0.9.4-F — Incident Sessions
Status: **IMPLEMENTED**
- derives incidents from existing backward-compatible event history;
- groups arming/armed/pre-alarm/trigger/terminal events;
- shows trigger reason, mode, source, output evidence and grace when present;
- shareable local plaintext report;
- no location/account data introduced.

## 0.9.4-G — Regression / release freeze
Status: **IN VALIDATION**
- Smart Arm pure-decision tests;
- sensitivity recommendation thresholds;
- Incident Session grouping/report;
- Accessory recovery and wake policy;
- advanced per-mode profile persistence;
- EN/IT/ES/FR/PT string-key parity;
- full development CI and signed INTERNAL exact-SHA gates remain mandatory.

## 0.9.5 — Adaptive Auto Guard (next priority)
Status: **SOURCE IMPLEMENTED IN FEATURE PR #9 — VALIDATION AND PHYSICAL CERTIFICATION PENDING**
Priority: **P0 product investment**, after the already shipped 0.9.4 software candidate. This is a feature-branch software implementation, NOT a physical certification claim or a 0.9.4 capability.

### Product goal and definition of done
User explicitly enables a visible, local-only, low-power auto-arm standby. While protection is **not already armed**, the app observes screen-off/inactivity and compatible physical context. After a user-configured idle delay or strong evidence of a pocket/bag insertion, it may arm an already supported Table/Pocket/Bag mode via the existing `ProtectionActivationCoordinator`. Motion classification can support the decision but is not proof of pocket placement. No automatic disarm, authentication bypass, or unverified protection claim is permitted.

Default proposal for product testing:
- Auto Guard **OFF by default**, explicit informed opt-in and foreground notification while active.
- General idle delay: **3 minutes** default, selectable **1/3/5/10/15 minutes**.
- Fast pocket candidate: **repeated stable windows** of concordant evidence (tunable via physical QA), not a guaranteed arming deadline.
- Table: must be stable, supported and successfully calibrated; inactivity alone never overrides mode capability gates.
- GPS/location: **not requested or used in the baseline**. No continuous GPS, location history, account, microphone or cloud processing.
- User may separately enable/disable pocket, table and travel-assisted classification; offer a visible pause/stop and clear explanation of why arming did or did not happen.

### 0.9.5-A — Auto Guard contract / lifecycle / opt-in (P0, prerequisite)
Status: **SOURCE IMPLEMENTED — PHYSICAL/OEM QA PENDING**
- Extend the **existing single** `ProtectionForegroundService`; do not create a second independent anti-theft background service.
- Introduce explicit states: `OFF`, `STANDBY`, `CANDIDATE`, `ARMED` (existing protection remains authoritative), `PAUSED`, `BLOCKED`; distinguish detection confidence from successfully armed state in UI, notification and events.
- An owner-enabled visible standby is required before background sensing. Reconcile it with existing charging auto-arm standby without duplicate monitors or competing activation sessions.
- Auto Guard setting OFF or a deliberate pause stops auto-arm evaluation; **never disarms an already active protected session**. After owner-authenticated manual disarm, suppress immediate rearm until a fresh qualifying screen-off/placement cycle (plus configurable pause/cooldown).
- Screen unlock/use cancels a pending standby/candidate decision; it does **not** authenticate, silence, or disarm `OWNER_CHECK`/`TRIGGERED`.
- Use elapsed-realtime-based deadlines within a boot, generation IDs and idempotent callbacks. Ignore stale callbacks after cancellation/restart; do not rely on wall-clock time for security.
- On reboot/process recovery, restore only the user-approved standby when OS policy permits; never assume a pre-reboot pending timer is valid or advertise monitoring when runtime is unavailable. Force-stop/OEM suppression cannot be bypassed.
- Define exact event evidence (source=AUTO_GUARD, reason, detector snapshot, confidence band, settings/permission state, outcome) with local privacy-preserving, bounded retention.

### 0.9.5-B — Inactivity Auto Rearm + Auto Table (P0)
Status: **SOURCE IMPLEMENTED — PHYSICAL/OEM QA PENDING**
- Screen-off and foreground-service lifecycle drive the monotonic inactivity timer; no broad Usage Access permission merely to infer non-use.
- Sample accelerometer/orientation briefly on demand or in bounded windows; require stable orientation, motion and Table detector readiness before proposing Table.
- Auto Table must follow the existing live-calibration contract. If placement/calibration evidence is missing, remain waiting or ask the owner; never silently claim ARMED.
- Timer expiry means **evaluate**, not unconditional arming. Active use, unsupported sensors, blocked security, low confidence and missing runtime prerequisites must prevent false protection claims.
- Expose timer, reason and status in Protection Center; respect notification permission and foreground-service restrictions.

### 0.9.5-C — Auto Pocket/Bag sensor-fusion classifier (P0)
Status: **SOURCE IMPLEMENTED — 99% UNPROVEN; PHYSICAL QA PENDING**
- Fuse ambient-light darkness, proximity, accelerometer gait/vibration, gyroscope/orientation and insertion/removal transitions **only where sensors are genuinely supported**.
- Example high-evidence pocket candidate: near-zero light + covered proximity + sustained motion consistent with carrying the device. These signals are correlated and **do not justify a claimed 99% accuracy** without measured data.
- Require stabilization, hysteresis, debouncing and repeated supporting windows; distinguish pocket, bag, covered-on-table, face-down, vehicle vibration and low-light edge cases.
- Explicit confidence bands: HIGH may request an automatic activation; MEDIUM extends observation; LOW/CONFLICTED remains STANDBY. Band thresholds must be validated on real hardware, not fabricated probabilities.
- Keep existing Pocket/Bag live-calibration and capability gates authoritative. A phone already covered without the necessary transition/evidence must not be automatically marked protected.
- No auto mode-switch while already ARMED; existing owner-check/alarm state always wins.

### 0.9.5-D — Mobility / journey context (P1)
Status: **SOURCE IMPLEMENTED (OPTIONAL GMS TRANSITIONS) — PHYSICAL QA PENDING**
- Prefer Android/Google Play services **Activity Recognition Transition API** for walking/running/cycling/in-vehicle/still **as optional supporting evidence** when available; request `ACTIVITY_RECOGNITION` only after explaining the value and obtaining user permission on applicable OS versions.
- Google Play services absence, permission denial and unavailable transition data must fall back to supported local sensors; never require account sign-in.
- Vehicle motion is evidence of transport, not proof of pocket vs bag. Combine with coverage/position before suggesting Pocket/Bag; retain user choice for Journey behavior.
- Evaluate passive hardware step counter/detector only when available and justified; do not add indiscriminate high-rate continuous polling.
- GPS/location remains an **optional later experiment** requiring a separate privacy, battery and Android background-location permission/policy review; excluded from 0.9.5 baseline and not a hidden dependency.

### 0.9.5-E — Power, UX and safety integration (P0)
Status: **PARTIALLY IMPLEMENTED — BATTERY, POLICY AND OEM REVIEW PENDING**
- Bounded duty-cycle sensing and event-driven resampling; unregister listeners promptly, track CPU/foreground runtime and measure standby battery overhead by OEM.
- Show: Auto Guard OFF / Waiting / Checking / Protected / Paused / Unavailable. A protection notification can only say Protected after the activation policy and detector actually reach ARMED.
- Explain auto-arm source and reason in Incident Sessions; do not log raw activity traces, routes, GPS coordinates or sensitive motion streams.
- Make Auto Guard/Charging standby arbitration explicit (single service and serialized activations). Preserve Charging/Accessory/Smart Arm priority and all owner-security invariants.
- Update EN/IT/ES/FR/PT strings and accessibility, with no mandatory third-party analytics/cloud dependency.
- Update `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` and Play reviewer evidence **only if implementation changes foreground behavior**, then independently verify Play policy acceptability; existing specialUse status is not blanket authorization.

### 0.9.5-F — Automated + physical QA / rollout gate (P0)
Status: **AUTOMATED REGRESSIONS ADDED — REAL-DEVICE AND SIGNED RELEASE EVIDENCE PENDING**
- Unit tests for decision-table coverage, confidence/hysteresis, monotonic deadlines, debounce, cancellation, owner-confirmed disarm cooldown, charging-standby arbitration, permission denial, sensor absence and recovery.
- Instrumentation for screen-off/unlock, multiple events, lifecycle, notification state, no duplicate arming, persistence/reboot and `OWNER_CHECK`/`TRIGGERED` priority.
- Mandatory real-device matrix: Pixel, Samsung One UI, Xiaomi/Redmi HyperOS/MIUI; daylight/darkness; pockets (loose/tight); bags; upside-down/face-down tables; stationary, walking, running, bicycle, passenger vehicle; wake/Doze/30-min screen-off and reboot.
- Measure **precision, recall and false alarms separately** for Auto Pocket and Auto Table. Aspirational Pocket precision >=99% on a **predefined representative physical dataset**, not an assertion of current accuracy; record sample count, test protocol, confidence intervals and failure modes before advertising any percentage.
- Battery acceptance requires comparison of identical eight-hour standby scenarios with Auto Guard ON versus OFF on each OEM, with the measured delta and limitations documented; no pre-claimed power percentage.
- P0 blockers: unauthorized automatic activation, false Protected UI, bypassed PIN/biometric, alarm suppression, serious regression in existing modes, and unacceptably frequent false triggers.
- Preserve mandatory source-truth, JVM tests, lint, emulator instrumentation, release build, exact-head review and signed INTERNAL exact-SHA gates. A green CI is **not** physical certification.

### Delivery order / boundaries
1. **A → B:** safe foreground standby, time-based rearms and Table.
2. **C:** Pocket/Bag fusion with no 99% claim pending test evidence.
3. **D + E:** optional activity transitions, vehicle differentiation, battery/UX hardening.
4. **F:** three-OEM real-world tuning, software RC and signed INTERNAL candidate; then Play compliance/public 1.0 decision.

0.9.5 software source exists in PR #9; software tests, signed INTERNAL and physical OEM measurements determine release readiness. The older signed 0.9.4 artifacts remain unaffected.

### Platform references (design-time, not proof of implementation)
- Android sensor foreground-service/background restrictions: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview
- Activity Recognition Transition API and permission: https://developers.google.com/android/reference/com/google/android/gms/location/ActivityRecognitionClient
- Android 14+ foreground service types / `specialUse`: https://developer.android.com/about/versions/14/changes/fgs-types-required

## 1.0 physical certification
Status: **PENDING EXTERNAL EVIDENCE**
- 0.9.5 Adaptive Auto Guard requires its own signed exact-SHA physical acceptance before any public claim; the 0.9.4 matrix remains outstanding.
- Pixel;
- Samsung;
- Xiaomi/Redmi;
- Table/Pocket/Bag false-positive/negative tuning;
- Charging connect/disconnect and grace;
- Accessory wired/USB/Bluetooth connect/disconnect and grace;
- Smart Arm real-context decisions;
- Live Guard Test trigger recognition;
- Sensitivity Advisor real-noise recommendations;
- Charging auto-arm foreground standby, reboot and OEM behavior;
- Tile/widget/shortcut entry paths;
- per-mode outputs;
- screen-off/Doze/background behavior;
- real flash/volume/DND/reboot behavior.

## Google Play
Status: **DEFERRED BY PRODUCT DECISION**
Play Console declarations, foreground-service reviewer evidence, privacy-policy URL, Data Safety, Play App Signing and store assets remain final publication work.


## 0.9.6 — Audit P0/P1/P2 remediation
Status: SOURCE CHANGES IN PR, PHYSICAL QA PENDING.
- P0: malformed sensor rejection with tests; saved-session-first charging standby reconciliation.
- P1: Smart Arm higher on Home, advanced tuning behind disclosure; six-mode localized first-run copy.
- P2: monolithic service/UI bounded extraction risk documented; no unvalidated service-wide rewrite.
- OEM physical, eight-hour ON/OFF battery, false alert soak and Play-policy gates remain mandatory.


## 0.9.7 — autonomous non-physical roadmap
1. [in progress] Home visual hierarchy and Smart Arm permission funnel, context reasoning.
2. [in progress] Dedicated Auto Guard route and localized standby UX.
3. [planned] Illustration-led onboarding and incident history filters with safer deletion.
4. [planned] Store ASO/privacy preparation with accurate feature claims and no unapproved billing.
5. [required] Exact-source JVM, emulator, lint, signed INTERNAL, APK size and UI smoke checks.
6. [blocked, physical] Pixel/Samsung/Redmi sensor performance, 8h battery and Play specialUse approval.
