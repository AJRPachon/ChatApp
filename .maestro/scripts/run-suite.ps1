<#
.SYNOPSIS
  Runs the single-device Maestro suite (or a subset) on one device.
.EXAMPLE
  ./run-suite.ps1                                   # whole suite, es, emulator-5554
  ./run-suite.ps1 -Locale en                        # same suite with the app in English
  ./run-suite.ps1 -IncludeTags smoke                # CI-like smoke subset
  ./run-suite.ps1 -Path flows/02_auth               # one section
  ./run-suite.ps1 -Path flows/02_auth/login_ok.yaml # one flow
  ./run-suite.ps1 -IncludeTags destructive          # destructive flows (needs disposable accounts)
#>
param(
    [string]$Device = 'emulator-5554',
    [ValidateSet('es', 'en')][string]$Locale = 'es',
    # One or more flow files/folders, comma-separated (e.g. flows/08_newchat_invitations,flows/09_groups).
    [string[]]$Path = @(),
    [string]$IncludeTags = '',
    [string]$ExcludeTags = '',
    [string]$RunId = '',
    [hashtable]$ExtraEnv = @{},
    # Real phone: never types the emulator PIN or changes system settings (language), and skips
    # flows tagged emulator-only (they would upload the owner's photos/camera/location/contacts).
    [switch]$Physical,
    # With -Physical: also run the personal-data flows (gallery/file pickers, camera, location,
    # contact, Google Drive backup). The owner allowed it on 2026-11-24. The fixtures are pushed
    # to the phone so the pickers take them instead of the owner's files, and removed afterwards.
    [switch]$AllowPersonalData
)
. "$PSScriptRoot/common.ps1"

if (-not $RunId) { $RunId = New-RunId }
Stop-MobileMcpAgent -Device $Device
if ($Physical) {
    $sys = (Invoke-Adb $Device shell getprop persist.sys.locale).Trim()
    if (-not $sys.StartsWith($Locale)) { throw "[$Device] el dispositivo está en '$sys'; cámbialo a mano a '$Locale' (no se toca un móvil real)." }
    if ((Invoke-Adb $Device shell dumpsys window) -match 'isKeyguardShowing=true') { throw "[$Device] desbloquea el móvil antes de ejecutar." }
    $baseExclude = @('multidevice', 'destructive', 'known-bug', 'emulator-only')
    if (-not $AllowPersonalData) { $baseExclude += 'personal-data' }
    $ExcludeTags = ($baseExclude + ($ExcludeTags -split ',' | Where-Object { $_ })) -join ','
} else {
    Unlock-Device -Device $Device
    & "$PSScriptRoot/set-locale.ps1" -Device $Device -Locale $Locale
}

$extra = @{ RUN_ID = $RunId; LOCALE = $Locale }
foreach ($k in $ExtraEnv.Keys) { $extra[$k] = $ExtraEnv[$k] }
$envArgs = Get-MaestroEnvArgs -Extra $extra

$outDir = Join-Path $script:MaestroRoot "out/$RunId-$Locale"
New-Item -ItemType Directory -Force $outDir | Out-Null
# Device facts for the report (the device may be gone when the report is regenerated later).
$appVer = ((@(Invoke-Adb $Device shell dumpsys package $script:AppId) -match 'versionName=') | Select-Object -First 1) -replace '.*versionName=', ''
[pscustomobject]@{
    serial  = $Device
    model   = ((Invoke-Adb $Device shell getprop ro.product.model) -join '').Trim()
    android = ((Invoke-Adb $Device shell getprop ro.build.version.release) -join '').Trim()
    appVer  = "$appVer".Trim()
    kind    = $(if ($Physical) { 'móvil físico' } else { 'emulador' })
} | ConvertTo-Json | Set-Content (Join-Path $outDir 'run-meta.json') -Encoding UTF8

$Path = @($Path | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$target = if ($Path) { @($Path | ForEach-Object { Join-Path $script:MaestroRoot $_ }) } else { @($script:MaestroRoot) }
$maestroArgs = @('--device', $Device, 'test') + $envArgs + @(
    '--format', 'junit',
    '--output', (Join-Path $outDir 'report.xml'),
    '--test-output-dir', $outDir
)
if ($IncludeTags) { $maestroArgs += @('--include-tags', $IncludeTags) }
if ($ExcludeTags) { $maestroArgs += @('--exclude-tags', $ExcludeTags) }
$maestroArgs += $target

Write-Host "RUN_ID=$RunId  locale=$Locale  device=$Device  target=$($target -join ' ')"
$savedAutofill = $null
$savedRotation = $null
if ($Physical) {
    # The phone's password manager offers to save the QA credentials into the owner's Google
    # account after every sign-in, in a sheet UI automation can't see. Turn the autofill service
    # off for the duration of the run only, and always put the original value back.
    $savedAutofill = ((Invoke-Adb $Device shell settings get secure autofill_service) | Out-String).Trim()
    Invoke-Adb $Device shell settings delete secure autofill_service | Out-Null
    Write-Host "[$Device] autocompletar desactivado temporalmente (era: $savedAutofill)"
    # Auto-rotate follows how the phone sits on the desk: in landscape the keyboard covers the
    # results. Lock portrait for the run and restore the owner's rotation settings afterwards.
    $savedRotation = @(
        ((Invoke-Adb $Device shell settings get system accelerometer_rotation) -join '').Trim(),
        ((Invoke-Adb $Device shell settings get system user_rotation) -join '').Trim()
    )
    Invoke-Adb $Device shell settings put system accelerometer_rotation 0 | Out-Null
    Invoke-Adb $Device shell settings put system user_rotation 0 | Out-Null
    Write-Host "[$Device] rotación fijada en vertical (era: auto=$($savedRotation[0]), user=$($savedRotation[1]))"
}
$pushedAssets = @(); $qaContactId = $null
if ($Physical -and $AllowPersonalData) {
    # Media fixtures as the newest items, so the system pickers select them and not the owner's photos.
    foreach ($a in (Get-ChildItem (Join-Path $script:MaestroRoot 'assets') -File)) {
        $dst = "/sdcard/Download/$($a.Name)"
        Invoke-Adb $Device push $a.FullName $dst | Out-Null
        Invoke-Adb $Device shell touch $dst | Out-Null
        $pushedAssets += $dst
    }
    Invoke-Adb $Device shell content call --method scan_volume --uri content://media --arg external_primary | Out-Null
    # Device-local (unsynced) "QA Contact", so the contact flow never shares a real person.
    if (-not ((Invoke-Adb $Device shell "content query --uri content://com.android.contacts/contacts --projection display_name") -match 'QA Contact')) {
        Invoke-Adb $Device shell "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n:" | Out-Null
        $qaContactId = ((Invoke-Adb $Device shell "content query --uri content://com.android.contacts/raw_contacts --projection _id --sort '_id DESC'") |
            Select-Object -First 1) -replace '.*_id=(\d+).*', '$1'
        Invoke-Adb $Device shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$qaContactId --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:'QA Contact'" | Out-Null
        Invoke-Adb $Device shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$qaContactId --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:+34600000000 --bind data2:i:2" | Out-Null
    }
    Write-Host "[$Device] datos personales permitidos: fixtures copiados ($($pushedAssets.Count)) y contacto QA listo"
}
try {
    & (Get-MaestroExe) @maestroArgs
    $code = $LASTEXITCODE
} finally {
    if ($Physical -and $savedAutofill -and $savedAutofill -ne 'null') {
        Invoke-Adb $Device shell settings put secure autofill_service $savedAutofill | Out-Null
        Write-Host "[$Device] autocompletar restaurado: $((Invoke-Adb $Device shell settings get secure autofill_service) -join '')"
    }
    if ($pushedAssets) {
        foreach ($p in $pushedAssets) { Invoke-Adb $Device shell rm -f $p | Out-Null }
        Invoke-Adb $Device shell content call --method scan_volume --uri content://media --arg external_primary | Out-Null
        Write-Host "[$Device] fixtures borrados del móvil"
    }
    if ($qaContactId -match '^\d+$') {
        Invoke-Adb $Device shell "content delete --uri 'content://com.android.contacts/raw_contacts?caller_is_syncadapter=true' --where '_id=$qaContactId'" | Out-Null
        Write-Host "[$Device] contacto QA borrado"
    }
    if ($Physical -and $savedRotation) {
        if ($savedRotation[1] -match '^\d+$') { Invoke-Adb $Device shell settings put system user_rotation $savedRotation[1] | Out-Null }
        if ($savedRotation[0] -match '^\d+$') { Invoke-Adb $Device shell settings put system accelerometer_rotation $savedRotation[0] | Out-Null }
        Write-Host "[$Device] rotación restaurada"
    }
}
Write-Host "Resultados: $outDir"
if (Test-Path (Join-Path $outDir 'report.xml')) {
    $label = if ($Physical) { 'móvil físico' } else { 'emulador' }
    & "$PSScriptRoot/make-report.ps1" -RunDir $outDir -Title "Informe E2E · $Device ($label, $Locale)"
}
exit $code
