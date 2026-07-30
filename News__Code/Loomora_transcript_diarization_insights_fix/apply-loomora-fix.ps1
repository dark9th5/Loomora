param(
    [Parameter(Mandatory = $false)]
    [string]$ProjectRoot = "."
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SourceRoot = Join-Path $ScriptDir "Loomora-fix"
$ProjectRoot = (Resolve-Path $ProjectRoot).Path

if (-not (Test-Path (Join-Path $ProjectRoot "settings.gradle.kts")) -or
    -not (Test-Path (Join-Path $ProjectRoot "core\offlineai"))) {
    throw "Không tìm thấy project Loomora tại: $ProjectRoot"
}

$Stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$BackupRoot = Join-Path $ProjectRoot ".loomora-backup-$Stamp"
New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null

Get-ChildItem -Path $SourceRoot -Recurse -File | ForEach-Object {
    $Relative = $_.FullName.Substring($SourceRoot.Length).TrimStart('\', '/')
    $Destination = Join-Path $ProjectRoot $Relative
    $Backup = Join-Path $BackupRoot $Relative

    if (Test-Path $Destination) {
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Backup) | Out-Null
        Copy-Item -Force $Destination $Backup
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Destination) | Out-Null
    Copy-Item -Force $_.FullName $Destination
}

Write-Host "Đã áp dụng bản sửa Loomora."
Write-Host "Bản sao lưu: $BackupRoot"
Write-Host "Tiếp theo chạy: .\gradlew.bat clean assembleDebug"
