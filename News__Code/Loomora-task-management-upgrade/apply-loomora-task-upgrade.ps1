param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectRoot
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path $ProjectRoot).Path
$PatchRoot = Join-Path $PSScriptRoot "patch"

if (-not (Test-Path (Join-Path $ProjectRoot "settings.gradle.kts"))) {
    throw "ProjectRoot không giống thư mục gốc Loomora: $ProjectRoot"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupRoot = Join-Path $ProjectRoot ".loomora-task-upgrade-backup-$timestamp"
New-Item -ItemType Directory -Path $BackupRoot -Force | Out-Null

$copied = 0
Get-ChildItem -Path $PatchRoot -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($PatchRoot.Length).TrimStart([char[]]@('\', '/'))
    $destination = Join-Path $ProjectRoot $relative
    $backupDestination = Join-Path $BackupRoot $relative

    if (Test-Path $destination) {
        New-Item -ItemType Directory -Path (Split-Path $backupDestination -Parent) -Force | Out-Null
        Copy-Item -Path $destination -Destination $backupDestination -Force
    }

    New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
    Copy-Item -Path $_.FullName -Destination $destination -Force
    $copied++
    Write-Host "Applied: $relative"
}

Write-Host ""
Write-Host "Đã áp dụng $copied file."
Write-Host "Backup file cũ: $BackupRoot"
Write-Host "Tiếp theo chạy: .\gradlew.bat clean :core:database:testDebugUnitTest :core:offlineai:testDebugUnitTest :app:assembleDebug"
