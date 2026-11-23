<#
.SYNOPSIS
  Runs multi-device scenarios (multidevice/<scenario>/NN_A_*.yaml, NN_B_*.yaml) across two emulators.
.DESCRIPTION
  Maestro drives one device per run and can't synchronise two, so each scenario is a sequence of
  steps executed in filename order: steps named *_A_* run on -DeviceA (signed in as QA, @claudeqa),
  *_B_* on -DeviceB (QA2, @claudeqa2). All steps share one RUN_ID, so a message A sends in step 01
  can be asserted by B in step 02. A step that must leave something "in progress" for the other
  device (a ringing call, a typing indicator) simply ends without stopping the app.
  A scenario may contain setup.ps1 / teardown.ps1, invoked with -RunId (e.g. disposable accounts).
.EXAMPLE
  ./run-multidevice.ps1                          # every scenario
  ./run-multidevice.ps1 -Scenario realtime_messaging
#>
param(
    [string]$Scenario = '',
    [string]$DeviceA = 'emulator-5554',
    [string]$DeviceB = 'emulator-5556',
    [ValidateSet('es', 'en')][string]$Locale = 'es',
    [string]$RunId = ''
)
. "$PSScriptRoot/common.ps1"

if (-not $RunId) { $RunId = New-RunId }
foreach ($d in @($DeviceA, $DeviceB)) { Unlock-Device -Device $d; Stop-MobileMcpAgent -Device $d }
& "$PSScriptRoot/set-locale.ps1" -Device $DeviceA -Locale $Locale
& "$PSScriptRoot/set-locale.ps1" -Device $DeviceB -Locale $Locale

$scenarioRoot = Join-Path $script:MaestroRoot 'multidevice'
$scenarios = if ($Scenario) { @(Get-Item (Join-Path $scenarioRoot $Scenario)) } else { Get-ChildItem $scenarioRoot -Directory | Sort-Object Name }
$failed = @()

foreach ($sc in $scenarios) {
    Write-Host "`n=== $($sc.Name) (RUN_ID=$RunId) ==="
    $outDir = Join-Path $script:MaestroRoot "out/$RunId-multidevice/$($sc.Name)"
    New-Item -ItemType Directory -Force $outDir | Out-Null
    $scenarioEnv = @{ RUN_ID = $RunId; LOCALE = $Locale }

    $setup = Join-Path $sc.FullName 'setup.ps1'
    if (Test-Path $setup) {
        $extra = & $setup -RunId $RunId
        if ($extra -is [hashtable]) { foreach ($k in $extra.Keys) { $scenarioEnv[$k] = $extra[$k] } }
    }

    $ok = $true
    foreach ($step in Get-ChildItem $sc.FullName -Filter '*.yaml' | Sort-Object Name) {
        $device = if ($step.Name -match '^\d+_A_') { $DeviceA } elseif ($step.Name -match '^\d+_B_') { $DeviceB } else { $null }
        if (-not $device) { continue }  # helper files
        Write-Host "--> [$device] $($step.Name)"
        $envArgs = Get-MaestroEnvArgs -Extra $scenarioEnv
        & (Get-MaestroExe) --device $device test @envArgs --test-output-dir $outDir $step.FullName
        if ($LASTEXITCODE -ne 0) { $ok = $false; break }
    }

    $teardown = Join-Path $sc.FullName 'teardown.ps1'
    if (Test-Path $teardown) { & $teardown -RunId $RunId }
    if (-not $ok) { $failed += $sc.Name }
}

if ($failed.Count -gt 0) {
    Write-Host "`nFALLAN: $($failed -join ', ')"
    exit 1
}
Write-Host "`nTodos los escenarios OK"
