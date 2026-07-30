#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /path/to/Loomora" >&2
  exit 1
fi

PROJECT_ROOT="$(cd "$1" && pwd)"
PATCH_ROOT="$(cd "$(dirname "$0")/patch" && pwd)"

if [[ ! -f "$PROJECT_ROOT/settings.gradle.kts" ]]; then
  echo "Project root does not look like Loomora: $PROJECT_ROOT" >&2
  exit 1
fi

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_ROOT="$PROJECT_ROOT/.loomora-task-upgrade-backup-$TIMESTAMP"
mkdir -p "$BACKUP_ROOT"

count=0
while IFS= read -r -d '' source; do
  relative="${source#"$PATCH_ROOT/"}"
  destination="$PROJECT_ROOT/$relative"
  backup="$BACKUP_ROOT/$relative"
  if [[ -f "$destination" ]]; then
    mkdir -p "$(dirname "$backup")"
    cp "$destination" "$backup"
  fi
  mkdir -p "$(dirname "$destination")"
  cp "$source" "$destination"
  echo "Applied: $relative"
  count=$((count + 1))
done < <(find "$PATCH_ROOT" -type f -print0)

echo
echo "Applied $count files."
echo "Backup: $BACKUP_ROOT"
echo "Next: ./gradlew clean :core:database:testDebugUnitTest :core:offlineai:testDebugUnitTest :app:assembleDebug"
