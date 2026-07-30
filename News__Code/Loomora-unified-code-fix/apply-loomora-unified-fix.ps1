$ErrorActionPreference = "Stop"
$PackageRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
python "$PackageRoot\tools\apply_unified_fix.py" --repo (Get-Location)
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "Chạy kiểm tra:"
Write-Host ".\gradlew.bat :core:offlineai:testDebugUnitTest :core:audio:testDebugUnitTest"
Write-Host ".\gradlew.bat clean check testDebugUnitTest :app:assembleDebug"
