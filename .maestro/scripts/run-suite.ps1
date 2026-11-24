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
    [string]$Path = '',
    [string]$IncludeTags = '',
    [string]$ExcludeTags = '',
    [string]$RunId = '',
    [hashtable]$ExtraEnv = @{},
    # Real phone: never types the emulator PIN or changes system settings (language), and skips
    # flows tagged emulator-only (they would upload the owner's photos/camera/location/contacts).
    [switch]$Physical
)
. "$PSScriptRoot/common.ps1"

if (-not $RunId) { $RunId = New-RunId }
Stop-MobileMcpAgent -Device $Device
if ($Physical) {
    $sys = (Invoke-Adb $Device shell getprop persist.sys.locale).Trim()
    if (-not $sys.StartsWith($Locale)) { throw "[$Device] el dispositivo está en '$sys'; cámbialo a mano a '$Locale' (no se toca un móvil real)." }
    if ((Invoke-Adb $Device shell dumpsys window) -match 'isKeyguardShowing=true') { throw "[$Device] desbloquea el móvil antes de ejecutar." }
    $ExcludeTags = (@('multidevice', 'destructive', 'known-bug', 'emulator-only') + ($ExcludeTags -split ',' | Where-Object { $_ })) -join ','
} else {
    Unlock-Device -Device $Device
    & "$PSScriptRoot/set-locale.ps1" -Device $Device -Locale $Locale
}

$extra = @{ RUN_ID = $RunId; LOCALE = $Locale }
foreach ($k in $ExtraEnv.Keys) { $extra[$k] = $ExtraEnv[$k] }
$envArgs = Get-MaestroEnvArgs -Extra $extra

$outDir = Join-Path $script:MaestroRoot "out/$RunId-$Locale"
New-Item -ItemType Directory -Force $outDir | Out-Null

$target = if ($Path) { Join-Path $script:MaestroRoot $Path } else { $script:MaestroRoot }
$maestroArgs = @('--device', $Device, 'test') + $envArgs + @(
    '--format', 'junit',
    '--output', (Join-Path $outDir 'report.xml'),
    '--test-output-dir', $outDir
)
if ($IncludeTags) { $maestroArgs += @('--include-tags', $IncludeTags) }
if ($ExcludeTags) { $maestroArgs += @('--exclude-tags', $ExcludeTags) }
$maestroArgs += $target

Write-Host "RUN_ID=$RunId  locale=$Locale  device=$Device  target=$target"
$savedAutofill = $null
if ($Physical) {
    # The phone's password manager offers to save the QA credentials into the owner's Google
    # account after every sign-in, in a sheet UI automation can't see. Turn the autofill service
    # off for the duration of the run only, and always put the original value back.
    $savedAutofill = ((Invoke-Adb $Device shell settings get secure autofill_service) | Out-String).Trim()
    Invoke-Adb $Device shell settings delete secure autofill_service | Out-Null
    Write-Host "[$Device] autocompletar desactivado temporalmente (era: $savedAutofill)"
}
try {
    & (Get-MaestroExe) @maestroArgs
    $code = $LASTEXITCODE
} finally {
    if ($Physical -and $savedAutofill -and $savedAutofill -ne 'null') {
        Invoke-Adb $Device shell settings put secure autofill_service $savedAutofill | Out-Null
        Write-Host "[$Device] autocompletar restaurado: $((Invoke-Adb $Device shell settings get secure autofill_service) -join '')"
    }
}
Write-Host "Resultados: $outDir"
if (Test-Path (Join-Path $outDir 'report.xml')) {
    $label = if ($Physical) { 'móvil físico' } else { 'emulador' }
    & "$PSScriptRoot/make-report.ps1" -RunDir $outDir -Title "Informe E2E · $Device ($label, $Locale)"
}
exit $code
