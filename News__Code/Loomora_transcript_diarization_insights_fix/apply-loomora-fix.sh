#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="${1:-.}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOURCE_ROOT="$SCRIPT_DIR/Loomora-fix"

if [[ ! -f "$PROJECT_ROOT/settings.gradle.kts" || ! -d "$PROJECT_ROOT/core/offlineai" ]]; then
  echo "Không tìm thấy project Loomora tại: $PROJECT_ROOT" >&2
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_ROOT="$PROJECT_ROOT/.loomora-backup-$STAMP"
mkdir -p "$BACKUP_ROOT"

while IFS= read -r -d '' source; do
  relative="${source#$SOURCE_ROOT/}"
  destination="$PROJECT_ROOT/$relative"
  backup="$BACKUP_ROOT/$relative"
  if [[ -f "$destination" ]]; then
    mkdir -p "$(dirname "$backup")"
    cp "$destination" "$backup"
  fi
  mkdir -p "$(dirname "$destination")"
  cp "$source" "$destination"
done < <(find "$SOURCE_ROOT" -type f -print0)

echo "Đã áp dụng bản sửa Loomora."
echo "Bản sao lưu: $BACKUP_ROOT"
echo "Tiếp theo chạy: ./gradlew clean assembleDebug"
