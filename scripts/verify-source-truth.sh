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

echo "Source truth OK: $VERSION_NAME / versionCode $VERSION_CODE"
