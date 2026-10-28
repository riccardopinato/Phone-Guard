#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"
VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"/\1/p' "$GRADLE_FILE" | head -1)"
VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode = \([0-9][0-9]*\)/\1/p' "$GRADLE_FILE" | head -1)"

if [[ -z "$VERSION_NAME" || -z "$VERSION_CODE" ]]; then
  echo "::error::Unable to read versionName/versionCode from $GRADLE_FILE"
  exit 1
fi

EXPECTED="Version truth: ${VERSION_NAME} / versionCode ${VERSION_CODE}"
for file in README.md RELEASE_STATUS.md PLAY_CONSOLE_CHECKLIST.md; do
  if ! grep -Fq "$EXPECTED" "$file"; then
    echo "::error::$file is not aligned with Gradle. Expected line: $EXPECTED"
    exit 1
  fi
done

if [[ -e "app/src/main/java/com/riccardopinato/phoneguard/pocket/power/ReliabilityWakeLock.kt" ]]; then
  echo "::error::Obsolete donor ReliabilityWakeLock.kt still exists"
  exit 1
fi

if grep -R -n -F "PocketGuard:ProtectedSession" app/src/main/java >/tmp/phoneguard-donor-tag.txt; then
  cat /tmp/phoneguard-donor-tag.txt
  echo "::error::Obsolete PocketGuard wake-lock tag found"
  exit 1
fi

if grep -R -n -F "outputController.prepare" app/src/main/java >/tmp/phoneguard-volume-prepare.txt; then
  cat /tmp/phoneguard-volume-prepare.txt
  echo "::error::Alarm volume must not be applied during arming/monitoring"
  exit 1
fi

# Prevent localization gaps in safety-critical, store-facing UX.
python3 - <<'PY'
import xml.etree.ElementTree as ET
from pathlib import Path
required = {
    'home_status_unprotected', 'home_status_explanation', 'home_manual_arm',
    'home_open_auto_guard', 'home_smart_decision',
    'home_smart_reason_power', 'home_smart_reason_accessory',
    'home_smart_reason_pocket', 'home_smart_reason_bag',
    'home_smart_reason_table', 'home_smart_reason_fallback',
    'auto_guard_standby_disclaimer',
    'events_filter_all', 'events_filter_alarms', 'events_alarm_summary',
    'events_clear_confirm_title', 'events_clear_confirm_body', 'events_filter_empty',
    *(f'onboarding_{n}_text' for n in range(1,5)),
    *(f'onboarding_visual_{name}' for name in (
        'table','pocket','charging','motion','light','near',
        'pin','biometric','owner','siren','vibrate','flash'
    )),
}
for locale in ('values','values-it','values-es','values-fr','values-pt'):
    path=Path('app/src/main/res')/locale/'strings.xml'
    tree=ET.parse(path)
    labels={x.attrib.get('name'):''.join(x.itertext()).strip()
            for x in tree.getroot().findall('string')}
    missing=required-set(labels)
    blank={k for k in required if k in labels and not labels[k]}
    if missing or blank:
        raise SystemExit(f'{path}: missing={sorted(missing)}, blank={sorted(blank)}')
print('UX safety copy parity: 5 locales OK')
PY

echo "Source truth OK: $VERSION_NAME / versionCode $VERSION_CODE"
