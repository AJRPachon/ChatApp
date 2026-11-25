<#
.SYNOPSIS
  Runs the single-device suite split across two emulators at the same time (about half the time).
.DESCRIPTION
  Both emulators sign in as the same QA account, so the split is by section, keeping together the
  flows that share state and would break each other if they ran at the same time:
   - A: everything that uses the 1:1 chat with QA2 or changes its row (archive, mute, "last
     message" assertions, broadcasts into it): 03, 05, 06, 07, 13 and the chat lifecycle flows.
   - B: launch, auth, status, invitations, groups, profile (it renames QA, so the flow that checks
     QA's name on Profile stays here), search.
  Each half is a normal run-suite.ps1 run with its own RUN_ID (and its own disposable accounts
  with -IncludeDestructive); afterwards both are merged into one report.
.EXAMPLE
  ./run-suite-parallel.ps1
  ./run-suite-parallel.ps1 -IncludeDestructive
#>
param(
    [string]$DeviceA = 'emulator-5554',
    [string]$DeviceB = 'emulator-5556',
    [ValidateSet('es', 'en')][string]$Locale = 'es',
    [switch]$IncludeDestructive
)
. "$PSScriptRoot/common.ps1"

$shardA = @(
    'flows/03_conversations', 'flows/05_chat_send', 'flows/06_chat_message_actions', 'flows/07_chat_menu',
    'flows/13_debug_menu', 'flows/12_lifecycle/background_resume.yaml', 'flows/12_lifecycle/rotation_keeps_state.yaml'
)
$shardB = @(
    'flows/01_launch', 'flows/02_auth', 'flows/04_status', 'flows/08_newchat_invitations', 'flows/09_groups',
    'flows/10_profile', 'flows/11_search', 'flows/12_lifecycle/rotation_profile_newchat.yaml'
)
# Safety net: a flow folder added later must be assigned to a shard, or it would silently not run.
$assigned = ($shardA + $shardB) | ForEach-Object { ($_ -split '/')[1] } | Sort-Object -Unique
$missing = Get-ChildItem (Join-Path $script:MaestroRoot 'flows') -Directory |
    Where-Object { $_.Name -notin $assigned -and $_.Name -notmatch '^00_' } | ForEach-Object Name
if ($missing) { throw "Carpetas de flows sin asignar a A ni B en run-suite-parallel.ps1: $($missing -join ', ')" }

$runA = New-RunId
$runB = ([int64]$runA + 1).ToString()
$logDir = Join-Path $script:MaestroRoot "out/$runA-parallel"
New-Item -ItemType Directory -Force $logDir | Out-Null

function Start-Shard([string]$Device, [string]$RunId, [string[]]$Paths, [bool]$Destructive, [string]$Name) {
    $args = @('-NoProfile', '-File', (Join-Path $PSScriptRoot 'run-suite.ps1'), '-Device', $Device, '-Locale', $Locale,
        '-RunId', $RunId, '-Path', ($Paths -join ','),
        # Explicit paths skip config.yaml, so its excludeTags must be passed here.
        '-ExcludeTags', 'multidevice,known-bug,requires-google,flaky')
    if ($Destructive) { $args += '-IncludeDestructive' }
    Start-Process powershell -ArgumentList $args -NoNewWindow -PassThru `
        -RedirectStandardOutput (Join-Path $logDir "$Name.log") -RedirectStandardError (Join-Path $logDir "$Name.err")
}

$started = Get-Date
Write-Host "A ($DeviceA, RUN_ID=$runA): $($shardA -join ', ')"
Write-Host "B ($DeviceB, RUN_ID=$runB): $($shardB -join ', ')"
# Only B has destructive flows (02_auth, 08, 10_profile).
$pA = Start-Shard $DeviceA $runA $shardA $false 'A'
$pB = Start-Shard $DeviceB $runB $shardB ([bool]$IncludeDestructive) 'B'
$pA.WaitForExit(); $pB.WaitForExit()
$elapsed = (Get-Date) - $started

foreach ($n in 'A', 'B') {
    Write-Host "`n--- $n ---"
    Get-Content (Join-Path $logDir "$n.log") | Select-String -Pattern '^\[Failed\]|Flows? (Passed|Failed)' | ForEach-Object { $_.Line }
}
$dirs = @("$runA-$Locale", "$runB-$Locale") | ForEach-Object { Join-Path $script:MaestroRoot "out/$_" } | Where-Object { Test-Path (Join-Path $_ 'report.xml') }
$report = Join-Path $script:MaestroRoot "out/$runA-parallel.html"
if ($dirs) {
    & "$PSScriptRoot/make-report.ps1" -RunDirs ($dirs -join ',') -Output $report -Title "Informe E2E · $DeviceA + $DeviceB en paralelo ($Locale)"
}
Write-Host ("`nTiempo total: {0:hh\:mm\:ss}. Logs: {1}" -f $elapsed, $logDir)
if ($pA.ExitCode -ne 0 -or $pB.ExitCode -ne 0) { exit 1 }
