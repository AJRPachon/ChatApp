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

function Get-UiNodes {
    <# Flat list of {Text, Id, Class, Bounds, Checked} parsed from a uiautomator dump of $Device. #>
    param([string]$Device)
    $dump = ''
    for ($try = 0; $try -lt 4 -and -not $dump.Contains('<node'); $try++) {
        # uiautomator sometimes returns "null root node" while a window is still animating in.
        Invoke-Adb $Device shell rm -f /sdcard/qa_ui.xml 2>$null | Out-Null
        Invoke-Adb $Device shell uiautomator dump /sdcard/qa_ui.xml 2>$null | Out-Null
        $dump = ((Invoke-Adb $Device shell cat /sdcard/qa_ui.xml 2>$null) -join '')
        if (-not $dump.Contains('<node')) { Start-Sleep -Seconds 1 }
    }
    $nodes = @()
    foreach ($m in [regex]::Matches($dump, '<node [^>]*>')) {
        $attrs = @{}
        foreach ($a in [regex]::Matches($m.Value, '([\w-]+)="([^"]*)"')) { $attrs[$a.Groups[1].Value] = [System.Net.WebUtility]::HtmlDecode($a.Groups[2].Value) }
        $nodes += [pscustomobject]@{ Text = $attrs['text']; Id = $attrs['resource-id']; Class = $attrs['class']; Bounds = $attrs['bounds']; Checked = $attrs['checked'] }
    }
    return $nodes
}

function Get-NodeRect($node) {
    $m = [regex]::Match([string]$node.Bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    return @([int]$m.Groups[1].Value, [int]$m.Groups[2].Value, [int]$m.Groups[3].Value, [int]$m.Groups[4].Value)
}

function Disable-GboardCorrections {
    <# Turns off Gboard auto-correction, auto-capitalisation, smart compose and word suggestions:
       with them on, `inputText` + Enter could commit a *suggestion* instead of the typed text
       ("claudeqa" became "claudeqa2"), and words followed by a space got autocorrected. #>
    param([string]$Device)
    Invoke-Adb $Device shell am start -a android.settings.INPUT_METHOD_SETTINGS | Out-Null
    Start-Sleep -Seconds 2
    $g = Get-UiNodes $Device | Where-Object { $_.Text -eq 'Gboard' } | Select-Object -First 1
    if (-not $g) { Write-Host "[$Device] Gboard no encontrado"; return }
    $r = Get-NodeRect $g; Invoke-Adb $Device shell input tap ([int](($r[0] + $r[2]) / 2)) ([int](($r[1] + $r[3]) / 2))
    Start-Sleep -Seconds 2
    $c = Get-UiNodes $Device | Where-Object { $_.Text -in @('Correcciones y sugerencias', 'Text correction') } | Select-Object -First 1
    if (-not $c) { Write-Host "[$Device] menu de correcciones no encontrado"; return }
    $r = Get-NodeRect $c; Invoke-Adb $Device shell input tap ([int](($r[0] + $r[2]) / 2)) ([int](($r[1] + $r[3]) / 2))
    Start-Sleep -Seconds 2
    $titles = @('Corrección automática', 'Auto-correction', 'Mayúsculas automáticas', 'Auto-capitalization',
        'Redacción inteligente', 'Smart Compose', 'Sugerencias de palabras', 'Show suggestion strip', 'Word suggestions')
    $done = @()
    for ($pass = 0; $pass -lt 2; $pass++) {
        $nodes = Get-UiNodes $Device
        $switches = $nodes | Where-Object { $_.Class -eq 'android.widget.Switch' }
        foreach ($t in ($nodes | Where-Object { $_.Text -in $titles })) {
            $tr = Get-NodeRect $t
            $sw = $switches | Where-Object { $sr = Get-NodeRect $_; $sr[1] -le ($tr[3] + 60) -and $sr[3] -ge ($tr[1] - 60) } | Select-Object -First 1
            if ($sw -and $sw.Checked -eq 'true' -and $t.Text -notin $done) {
                $done += $t.Text
                $sr = Get-NodeRect $sw
                Invoke-Adb $Device shell input tap ([int](($sr[0] + $sr[2]) / 2)) ([int](($sr[1] + $sr[3]) / 2))
                Start-Sleep -Milliseconds 400
                Write-Host "[$Device] Gboard: desactivado '$($t.Text)'"
            }
        }
        Invoke-Adb $Device shell input swipe 670 2400 670 1200 300
        Start-Sleep -Milliseconds 800
    }
    Invoke-Adb $Device shell input keyevent KEYCODE_HOME | Out-Null
}
