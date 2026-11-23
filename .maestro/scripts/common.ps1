# Shared helpers for the Maestro scripts. Dot-source it: . "$PSScriptRoot/common.ps1"

$script:MaestroRoot = Split-Path $PSScriptRoot -Parent
$script:AppId = 'com.ajrpachon.chatapp'

function Read-MaestroEnv {
    <# Reads .maestro/.env (KEY=VALUE per line, # comments) into a hashtable. #>
    $envFile = Join-Path $script:MaestroRoot '.env'
    $vars = @{}
    if (-not (Test-Path $envFile)) {
        throw "Falta $envFile — copia .env.example a .env y rellénalo."
    }
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $idx = $trimmed.IndexOf('=')
        if ($idx -lt 1) { continue }
        $key = $trimmed.Substring(0, $idx).Trim()
        $value = $trimmed.Substring($idx + 1).Trim()
        if ($value -ne '') { $vars[$key] = $value }
    }
    return $vars
}

function Get-MaestroEnvArgs {
    <# Builds `-e KEY=VALUE` arguments from .env plus $Extra. Secrets for the Admin API are never passed to flows. #>
    param([hashtable]$Extra = @{})
    $vars = Read-MaestroEnv
    foreach ($k in $Extra.Keys) { $vars[$k] = $Extra[$k] }
    $args = @()
    foreach ($k in ($vars.Keys | Sort-Object)) {
        if ($k -in @('SUPABASE_SERVICE_ROLE_KEY', 'SUPABASE_URL')) { continue }
        $args += @('-e', "$k=$($vars[$k])")
    }
    return $args
}

function Get-MaestroExe {
    <# `maestro` from PATH, else the default install location (~/.maestro/bin). #>
    $cmd = Get-Command maestro -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($name in @('maestro.bat', 'maestro.cmd', 'maestro')) {
        $candidate = Join-Path $env:USERPROFILE ".maestro/bin/$name"
        if (Test-Path $candidate) { return $candidate }
    }
    throw 'No se encuentra la CLI de Maestro (instálala: https://maestro.dev).'
}

function New-RunId {
    return (Get-Date -Format 'yyyyMMddHHmmss')
}

function Invoke-Adb {
    # Plain (non-advanced) function on purpose: flags like `-a` must reach adb untouched instead of
    # being parsed as PowerShell parameters. Usage: Invoke-Adb <device> <adb args...>
    $device = $args[0]
    $rest = @($args | Select-Object -Skip 1)
    & adb -s $device @rest
}

function Unlock-Device {
    <# Wakes the device and enters the QA PIN if the keyguard is showing (emulators boot locked once a PIN is set). #>
    param([string]$Device, [string]$Pin = '1234')
    $kg = (Invoke-Adb $Device shell dumpsys window) -match 'isKeyguardShowing=true'
    if (-not $kg) { return }
    Invoke-Adb $Device shell input keyevent KEYCODE_WAKEUP
    Start-Sleep -Seconds 1
    Invoke-Adb $Device shell input swipe 670 2600 670 900 300
    Start-Sleep -Seconds 1
    Invoke-Adb $Device shell input text $Pin
    Invoke-Adb $Device shell input keyevent KEYCODE_ENTER
    Start-Sleep -Seconds 2
}

function Stop-MobileMcpAgent {
    <# mobile-mcp leaves an app_process server (com.mobilenext.mobilecli.DeviceServer) holding the
       single UiAutomation connection; while it lives, Maestro's driver times out on start-up. #>
    param([string]$Device)
    $line = (Invoke-Adb $Device shell "ps -A -o PID,ARGS") | Where-Object { $_ -match 'mobilecli\.DeviceServer' }
    foreach ($l in @($line)) {
        $agentPid = ($l.Trim() -split '\s+')[0]
        Invoke-Adb $Device shell kill $agentPid | Out-Null
        Write-Host "[$Device] parado el agente de mobile-mcp (pid $agentPid)"
    }
}
