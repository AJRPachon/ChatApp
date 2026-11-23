<#
.SYNOPSIS
  One-time (idempotent) preparation of the QA emulators for the Maestro suite.
.DESCRIPTION
  - Disables animations and keeps the screen on.
  - Sets a device PIN (1234) so the app-lock toggle can be enabled (it requires a device credential).
  - Pushes the media fixtures in .maestro/assets to /sdcard/Download and indexes them so the
    system photo picker / document picker can see them.
  - Optionally installs the debug APK.
.EXAMPLE
  ./setup-emulators.ps1 -Devices emulator-5554,emulator-5556 -InstallApk
#>
param(
    [string[]]$Devices = @('emulator-5554', 'emulator-5556'),
    [switch]$InstallApk,
    [string]$Pin = '1234'
)
. "$PSScriptRoot/common.ps1"

$repoRoot = Split-Path $script:MaestroRoot -Parent
$apk = Join-Path $repoRoot 'app/build/outputs/apk/debug/app-debug.apk'
$assets = Join-Path $script:MaestroRoot 'assets'

foreach ($d in $Devices) {
    Write-Host "== $d =="
    Invoke-Adb $d wait-for-device
    Invoke-Adb $d shell settings put global window_animation_scale 0
    Invoke-Adb $d shell settings put global transition_animation_scale 0
    Invoke-Adb $d shell settings put global animator_duration_scale 0
    Invoke-Adb $d shell svc power stayon true
    Invoke-Adb $d shell settings put system screen_off_timeout 1800000

    # Device credential for the app-lock flows. Ignored if a PIN is already set.
    $pinOut = Invoke-Adb $d shell locksettings set-pin $Pin 2>&1
    Write-Host "  PIN: $pinOut"
    Unlock-Device -Device $d -Pin $Pin

    if (Test-Path $assets) {
        Get-ChildItem $assets -File | ForEach-Object {
            Invoke-Adb $d push $_.FullName "/sdcard/Download/$($_.Name)" | Out-Null
        }
        Invoke-Adb $d shell content call --method scan_volume --uri content://media --arg external_primary | Out-Null
        Write-Host '  assets copiados a /sdcard/Download'
    }

    if ($InstallApk) {
        if (-not (Test-Path $apk)) { throw "No existe $apk — ejecuta ./gradlew assembleDebug" }
        Invoke-Adb $d install -r -g $apk
    }
}
