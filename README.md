# Phone Guard

Phone Guard is a standalone Android anti-theft / anti-tampering suite assembled from proven capabilities developed in:

- Tabletop Motion Alarm — reliability, table motion detection, alarm outputs, recovery, local event history, diagnostics.
- Pocket Guard / Pockara — pocket/bag sensor fusion, owner verification, Journey foundations, hardened PIN security.

## Factory baseline

- App Factory Core: ed6466e774a28e95f9f371cd56bbf7278428776a
- Tabletop donor snapshot: 9e67bcbd84e8add7d183349644d3f6f6bbe9ffe0
- Pocket Guard donor snapshot: b7b1c74de66e047c32083365b44561e42237b4fc

## Product modes

- Table
- Pocket
- Journey
- Bag is an internal/contextual protection profile rather than a primary top-level mode.

## Product invariants

- Native Android / Kotlin / Jetpack Compose.
- Local-first and offline-first.
- No required account or backend.
- A visible "Protected" state must correspond to a healthy runtime service.
- Table behavior is a regression-protected donor baseline.
- New protection modes must reach the same reliability bar before release.

## Alarm escalation

Phone Guard is designed to combine:
- siren;
- vibration;
- configurable LED flash frequency;
- configurable alarm-volume target;
- high-visibility red WARNING alarm screen;
- capability/health diagnostics and graceful Android/OEM fallbacks.

The repository is intentionally independent from the donor apps so Tabletop Motion Alarm, Pocket Guard/Pockara and Phone Guard can be sold and maintained as separate products.
