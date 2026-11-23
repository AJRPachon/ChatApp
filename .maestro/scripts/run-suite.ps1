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
    [hashtable]$ExtraEnv = @{}
)
. "$PSScriptRoot/common.ps1"

if (-not $RunId) { $RunId = New-RunId }
Unlock-Device -Device $Device
Stop-MobileMcpAgent -Device $Device
& "$PSScriptRoot/set-locale.ps1" -Device $Device -Locale $Locale

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
& (Get-MaestroExe) @maestroArgs
$code = $LASTEXITCODE
Write-Host "Resultados: $outDir"
exit $code
