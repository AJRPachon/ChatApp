<#
.SYNOPSIS
  One-time (idempotent) preparation of the QA emulators for the Maestro suite.
.DESCRIPTION
  - Keeps animations on (see below) and the screen awake.
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
    # Animations stay ON: the status viewer's auto-advance timer is an animation, so
    # animator_duration_scale 0 makes every story finish instantly and the viewer close itself.
    Invoke-Adb $d shell settings put global window_animation_scale 1
    Invoke-Adb $d shell settings put global transition_animation_scale 1
    Invoke-Adb $d shell settings put global animator_duration_scale 1
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

    # Device contact "QA Contact" for the contact-picker flows (attach contact, new chat contacts).
    $existing = Invoke-Adb $d shell content query --uri content://com.android.contacts/data --projection data1 --where "data1='QA Contact'"
    if (-not ($existing -match 'QA Contact')) {
        Invoke-Adb $d shell content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n: | Out-Null
        $rawId = ((Invoke-Adb $d shell content query --uri content://com.android.contacts/raw_contacts --projection _id --sort '_id DESC') |
            Select-Object -First 1) -replace '.*_id=(\d+).*', '$1'
        Invoke-Adb $d shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$rawId --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:'QA Contact' | Out-Null
        Invoke-Adb $d shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$rawId --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:'+34600000000' --bind data2:i:2 | Out-Null
        Write-Host '  contacto "QA Contact" creado'
    }

    if ($InstallApk) {
        if (-not (Test-Path $apk)) { throw "No existe $apk — ejecuta ./gradlew assembleDebug" }
        Invoke-Adb $d install -r -g $apk
    }
}
