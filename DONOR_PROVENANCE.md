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
