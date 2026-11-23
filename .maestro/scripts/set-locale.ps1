<#
.SYNOPSIS
  Ensures a device's *system* language is es or en before a Maestro run.
.DESCRIPTION
  Maestro's `clearState` (pm clear) wipes Android 13+ per-app languages, so the suite relies on
  the system language. Google Play emulator images aren't rootable (no setprop) and ignore
  `-change-locale`, so this drives Settings > Languages over adb + uiautomator: adds the language
  if missing, drags it to the top of the preferred list and confirms "Change system language".
  Idempotent: does nothing when the system language already matches.
.EXAMPLE
  ./set-locale.ps1 -Device emulator-5554 -Locale en
#>
param(
    [string]$Device = 'emulator-5554',
    [ValidateSet('es', 'en')][string]$Locale = 'es'
)
. "$PSScriptRoot/common.ps1"

$targetName = @{ es = 'Español (Estados Unidos)'; en = 'English (United States)' }[$Locale]

function Get-SystemLocale { (Invoke-Adb $Device shell getprop persist.sys.locale).Trim() }

function Get-UiNodes {
    # Flat PSCustomObjects parsed with a regex: piping XmlElement objects would make PowerShell
    # enumerate their child nodes instead of the elements themselves.
    Invoke-Adb $Device shell uiautomator dump /sdcard/qa_ui.xml | Out-Null
    $dump = (Invoke-Adb $Device shell cat /sdcard/qa_ui.xml) -join ''
    $nodes = @()
    foreach ($m in [regex]::Matches($dump, '<node [^>]*>')) {
        $attrs = @{}
        foreach ($a in [regex]::Matches($m.Value, '([\w-]+)="([^"]*)"')) { $attrs[$a.Groups[1].Value] = [System.Net.WebUtility]::HtmlDecode($a.Groups[2].Value) }
        $nodes += [pscustomobject]@{ Text = $attrs['text']; Id = $attrs['resource-id']; Class = $attrs['class']; Bounds = $attrs['bounds'] }
    }
    return $nodes
}

function Get-Center($node) {
    $m = [regex]::Match([string]$node.Bounds, '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
    $x = [int](([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2)
    $y = [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2)
    return @($x, $y)
}

function Wait-Ui { Start-Sleep -Milliseconds 1500 }

if ((Get-SystemLocale).StartsWith($Locale)) {
    Invoke-Adb $Device shell cmd locale set-app-locales $script:AppId --locales '""' | Out-Null
    Write-Host "[$Device] idioma del sistema: $(Get-SystemLocale)"
    return
}

Invoke-Adb $Device shell am start -a android.settings.LOCALE_SETTINGS | Out-Null
Wait-Ui

$labels = Get-UiNodes | Where-Object { $_.Id -eq 'com.android.settings:id/label' }
if (-not ($labels | Where-Object { $_.Text -eq $targetName })) {
    $add = Get-UiNodes | Where-Object { $_.Id -eq 'com.android.settings:id/add_language' } | Select-Object -First 1
    $c = Get-Center $add
    Invoke-Adb $Device shell input tap $c[0] $c[1]
    Wait-Ui
    $item = $null
    for ($i = 0; $i -lt 15 -and -not $item; $i++) {
        $item = Get-UiNodes | Where-Object { $_.Id -eq 'android:id/locale' -and $_.Text -eq $targetName } | Select-Object -First 1
        if (-not $item) { Invoke-Adb $Device shell input swipe 670 2400 670 1200 300; Wait-Ui }
    }
    if (-not $item) { throw "[$Device] no encuentro '$targetName' en la lista de idiomas" }
    $c = Get-Center $item
    Invoke-Adb $Device shell input tap $c[0] $c[1]
    Wait-Ui
}

# Drag the target language's handle onto the first row.
$nodes = Get-UiNodes
$rows = $nodes | Where-Object { $_.Id -eq 'com.android.settings:id/label' }
$handles = @($nodes | Where-Object { $_.Id -eq 'com.android.settings:id/dragHandle' })
$targetIndex = [array]::IndexOf(@($rows | ForEach-Object { $_.Text }), $targetName)
if ($targetIndex -gt 0) {
    $from = Get-Center $handles[$targetIndex]
    $to = Get-Center $handles[0]
    Invoke-Adb $Device shell input draganddrop $from[0] $from[1] $to[0] ($to[1] - 40) 1500
    Wait-Ui
    $confirm = Get-UiNodes | Where-Object { $_.Class -eq 'android.widget.Button' -and $_.Text -in @('Change', 'Cambiar') } | Select-Object -First 1
    if ($confirm) {
        $c = Get-Center $confirm
        Invoke-Adb $Device shell input tap $c[0] $c[1]
        Wait-Ui
    }
}

Invoke-Adb $Device shell input keyevent KEYCODE_HOME | Out-Null
Invoke-Adb $Device shell cmd locale set-app-locales $script:AppId --locales '""' | Out-Null
$now = Get-SystemLocale
if (-not $now.StartsWith($Locale)) { throw "[$Device] no se pudo cambiar el idioma (sigue en '$now')" }
Write-Host "[$Device] idioma del sistema: $now"
