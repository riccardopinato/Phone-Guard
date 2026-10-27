# DONOR_PROVENANCE.md

Phone Guard is a standalone product. Donor code is copied/adapted into this repository; there are no runtime dependencies on donor apps.

- Tabletop Motion Alarm snapshot: `riccardopinato/Tabletop-Motion-Alarm@9e67bcbd84e8add7d183349644d3f6f6bbe9ffe0`
  - reliability authority, Table detector, foreground service patterns, event history, diagnostics, alarm outputs.
- Pocket Guard / Pockara snapshot: `riccardopinato/Pocket-Guard@b7b1c74de66e047c32083365b44561e42237b4fc`
  - Pocket/Bag sensor fusion, calibration, owner-check state machine, Journey policy, Keystore hardening patterns.
- Luminous Circle uploaded snapshot inspected 2026-10-27.
  - hardened torch/strobe executor, generation-token cancellation and visual escalation patterns.
  - no canonical Git commit was available for the uploaded snapshot.

Phone Guard-specific hardening supersedes donor behavior where the contracts differ.

## 0.9.3 Market Value Pack

Clean-room / official-API implementation:
- Android power connection/disconnection broadcasts: Charging Guard and opt-in charging auto-arm standby.
- Android TileService: Quick Settings entry surface.
- Android AppWidgetProvider and static shortcuts: quick activation surfaces.
- All entry surfaces use Phone Guard's own activation coordinator and security/capability truth.

External repositories inspected only as product/pattern references:
- `pikadexofc/charger-unplug-alarm` — charger removal product pattern; no source copied.
- `Lissy93/anti-theft-charge` — public charging anti-theft product pattern; no source copied.
- `flxholle/QuickTiles` — Quick Settings pattern reference; GPL code not incorporated.

Reusable Phone Guard-owned engines created in this release:
- `ProtectionActivationCoordinator`;
- `ProtectionPresetEngine`;
- `ProtectionCheckEngine`;
- `ChargingStateMonitor`;
- per-mode `AlarmOutputProfile`;
- structured `AlarmEventEvidence`.



## 0.9.4 Market Value Pack 2

Clean-room / official-API implementation:
- Android `AudioManager`, `AudioDeviceInfo` and `AudioDeviceCallback`: Accessory Guard audio-device presence/removal;
- existing Phone Guard sensor controller plus deterministic policy: Smart Arm and Sensitivity Advisor;
- Android dynamic power broadcast handling: Live Charging Guard test;
- Android `ApplicationExitInfo`: last-process-exit diagnostics;
- Android Storage Access Framework document URI: custom alarm audio;
- existing Phone Guard event evidence: derived Incident Sessions and local share reports.

External repositories/services inspected only as product or reliability references:
- `urbandroid-team/dont-kill-my-app` — OEM background-reliability problem/reference; no content or source copied into runtime code;
- `tharunbirla/IntruderSelfie` — product reference from audit only; no source or selfie feature incorporated;
- `drb101005/Blue-Trigger` — Bluetooth-disconnect product/pattern reference; no source copied and no dependency introduced.

Reusable Phone Guard-owned engines created or extended:
- `AccessoryStateMonitor`;
- `SmartArmDecisionEngine` / `SmartArmContextResolver`;
- `LiveProtectionTestEngine`;
- `SensitivityRecommendationEngine` / `SensitivityAdvisor`;
- `ProtectionReliabilityDiagnostics`;
- advanced `AlarmOutputProfile`;
- `IncidentSessionEngine`.

No third-party runtime dependency was added for the 0.9.4 feature pack.
