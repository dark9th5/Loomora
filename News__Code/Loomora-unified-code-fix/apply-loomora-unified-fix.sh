#!/usr/bin/env bash
set -euo pipefail
PACKAGE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "$PACKAGE_ROOT/tools/apply_unified_fix.py" --repo "$(pwd)"

echo
echo "Chạy kiểm tra:"
echo "./gradlew :core:offlineai:testDebugUnitTest :core:audio:testDebugUnitTest"
echo "./gradlew clean check testDebugUnitTest :app:assembleDebug"
