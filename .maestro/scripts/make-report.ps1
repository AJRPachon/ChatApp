<#
.SYNOPSIS
  Builds the self-contained HTML execution report of a Maestro run (.maestro/out/<run>/report.html).
.DESCRIPTION
  Collects, for the run: every flow of the suite (executed or not, with the reason it was not run),
  each executed flow's steps (commands.json), and for failures the error and the last screenshot
  (embedded). Adds the trend of each flow and a history table from the other runs in .maestro/out,
  the device / build environment, and the "bugs found" table of .maestro/README.md. The data is
  injected into scripts/report-template.html (same layout as the EuroDigital Maestro report).
.EXAMPLE
  ./make-report.ps1                                    # latest run
  ./make-report.ps1 -RunDir .maestro/out/20261124083253-es
  ./make-report.ps1 -RunDirs out/A-es,out/B-es -Output out/combined.html   # several runs, latest result per flow
#>
param(
    [string]$RunDir = '',
    # Several runs of the same device merged into one report (later runs win per flow). Runs cut
    # short without report.xml are rebuilt from each flow's commands.json.
    [string[]]$RunDirs = @(),
    [string]$Output = '',
    [string]$Title = ''
)
. "$PSScriptRoot/common.ps1"

$outRoot = Join-Path $script:MaestroRoot 'out'
$runs = Get-ChildItem $outRoot -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'report.xml') } | Sort-Object Name
$RunDirs = @($RunDirs | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ } | ForEach-Object { (Resolve-Path $_).Path } | Sort-Object)
if ($RunDirs.Count -gt 0) { $RunDir = $RunDirs[-1] }
if (-not $RunDir) { $RunDir = ($runs | Select-Object -Last 1).FullName }
$RunDir = (Resolve-Path $RunDir).Path
if ($RunDirs.Count -eq 0) { $RunDirs = @($RunDir) }
if (-not $Output) { $Output = Join-Path $RunDir 'report.html' }

$sectionNames = @{
    '01' = 'Arranque'; '02' = 'Autenticación'; '03' = 'Lista de conversaciones'; '04' = 'Estados'
    '05' = 'Enviar en el chat'; '06' = 'Acciones sobre mensajes'; '07' = 'Menú del chat'
    '08' = 'Nuevo chat e invitaciones'; '09' = 'Grupos'; '10' = 'Perfil'; '11' = 'Búsqueda'
    '12' = 'Ciclo de vida'; '13' = 'Menú QA/Debug'; 'MD' = 'Multi-dispositivo'
}

function Get-FlowFileByName {
    $map = @{}
    foreach ($f in (Get-ChildItem (Join-Path $script:MaestroRoot 'flows') -Recurse -Filter *.yaml)) {
        $t = Get-Content $f.FullName -Raw -Encoding UTF8
        if ($t -match 'name:\s*"([^"]+)"') { $map[($Matches[1] -replace '[\/:*?"<>|]', '_')] = $f.Name }
    }
    $map
}

# A run stopped before Maestro wrote report.xml: one case per flow folder that finished.
function Read-PartialRun([string]$dir) {
    $byName = Get-FlowFileByName
    $items = foreach ($d in (Get-ChildItem $dir -Recurse -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'commands.json') })) {
        $cmds = @((Get-Content (Join-Path $d.FullName 'commands.json') -Raw -Encoding UTF8 | ConvertFrom-Json) | ForEach-Object { $_ })
        $top = @($cmds | Where-Object { $_.metadata.depth -eq 0 })
        if (@($top | Where-Object { $_.metadata.status -in 'PENDING', 'RUNNING' }).Count -or -not $top.Count) { continue }
        $failed = @($cmds | Where-Object { $_.metadata.status -eq 'FAILED' })
        $err = if ($failed) { "$(($failed | Select-Object -Last 1).metadata.error.message)" } else { '' }
        [pscustomobject]@{
            File = $byName[$d.Name]; Name = $d.Name; Seconds = ((@($top | ForEach-Object { [double]$_.metadata.duration }) | Measure-Object -Sum).Sum / 1000)
            Ok = -not $failed; Error = $err; Dir = $dir
        }
    }
    $meta = Join-Path $dir 'run-meta.json'
    $dev = if (Test-Path $meta) { (Get-Content $meta -Raw -Encoding UTF8 | ConvertFrom-Json).serial } else { '' }
    [pscustomobject]@{ RunId = Split-Path $dir -Leaf; Device = $dev; Start = ''; Seconds = (($items | Measure-Object Seconds -Sum).Sum); Cases = @($items | Where-Object File) }
}

function Read-Run([string]$dir) {
    if (Test-Path (Join-Path $dir 'report.xml')) { Read-Junit $dir } else { Read-PartialRun $dir }
}

function Read-Junit([string]$dir) {
    [xml]$x = Get-Content (Join-Path $dir 'report.xml') -Encoding UTF8
    $s = $x.testsuites.testsuite
    $items = foreach ($c in @($s.testcase)) {
        $msg = if ($c.failure) { if ($c.failure -is [string]) { $c.failure } elseif ($c.failure.InnerText) { $c.failure.InnerText } else { "$($c.failure)" } } else { '' }
        [pscustomobject]@{
            File = [IO.Path]::GetFileName($c.file); Name = $c.name; Seconds = [double]$c.time
            Ok = ($c.status -eq 'SUCCESS' -and -not $c.failure); Error = $msg; Dir = $dir
        }
    }
    [pscustomobject]@{ RunId = Split-Path $dir -Leaf; Device = $s.device; Start = $s.timestamp; Seconds = [double]$s.time; Cases = @($items) }
}

function Get-StepLabel($cmd) {
    $p = $cmd.PSObject.Properties | Select-Object -First 1
    $name = $p.Name; $v = $p.Value
    $sel = $v.selector; if (-not $sel) { $sel = $v.condition.visible }; if (-not $sel) { $sel = $v.condition.notVisible }
    $target = if ($sel.idRegex) { "id $($sel.idRegex)" } elseif ($sel.textRegex) { "texto `"$($sel.textRegex)`"" } else { '' }
    switch -Regex ($name) {
        '^tapOnElement' { return "Tocar $target" }
        '^inputTextCommand' { return "Escribir `"$($v.text)`"" }
        '^assertConditionCommand' { $neg = if ($v.condition.notVisible) { 'no ' } else { '' }; return "Comprobar que ${neg}se ve $target" }
        '^runFlowCommand' { return "Subflow $(if ($v.sourceDescription) { $v.sourceDescription } else { '' })".Trim() }
        '^launchAppCommand' { return 'Lanzar la app' + $(if ($v.clearState) { ' (estado limpio)' } else { '' }) }
        '^scrollUntilVisible' { return "Desplazar hasta ver $(if ($v.selector.idRegex) { 'id ' + $v.selector.idRegex } else { $v.selector.textRegex })" }
        '^backPressCommand' { return 'Atrás' }
        '^hideKeyboardCommand' { return 'Ocultar teclado' }
        '^eraseTextCommand' { return 'Borrar texto' }
        '^pressKeyCommand' { return "Tecla $($v.code)" }
        '^swipeCommand' { return 'Deslizar' }
        '^repeatCommand' { return 'Repetir' }
        '^setAirplaneModeCommand' { return "Modo avión: $($v.value)" }
        default { return ($name -replace 'Command$', '') }
    }
}

function Get-Artifacts([string]$runDir, [string]$flowName, [bool]$withImage) {
    $safe = ($flowName -replace '[\\/:*?"<>|]', '_')
    $dir = Get-ChildItem $runDir -Recurse -Directory | Where-Object { $_.Name -eq $safe } | Select-Object -First 1
    $res = [pscustomobject]@{ Steps = @(); FailedStep = ''; Image = $null }
    if (-not $dir) { return $res }
    $cmdFile = Join-Path $dir.FullName 'commands.json'
    if (Test-Path $cmdFile) {
        $cmds = Get-Content $cmdFile -Raw -Encoding UTF8 | ConvertFrom-Json
        $res.Steps = @($cmds | Where-Object {
                $n = ($_.command.PSObject.Properties | Select-Object -First 1).Name
                $n -notmatch '^(applyConfiguration|defineVariables)' -and $_.metadata.depth -le 3
            } | ForEach-Object {
                [pscustomobject]@{ label = (Get-StepLabel $_.command); st = $_.metadata.status; ms = [int]$_.metadata.duration; d = [int]$_.metadata.depth }
            })
        $f = @($res.Steps | Where-Object { $_.st -eq 'FAILED' }) | Select-Object -Last 1
        if ($f) { $res.FailedStep = $f.label }
    }
    if ($withImage) {
        $shot = Get-ChildItem (Join-Path $dir.FullName 'screenshots') -Filter *.png -ErrorAction SilentlyContinue | Sort-Object Name | Select-Object -Last 1
        if ($shot) { $res.Image = 'data:image/png;base64,' + [Convert]::ToBase64String([IO.File]::ReadAllBytes($shot.FullName)) }
    }
    return $res
}

function Get-RunDate([string]$runId) {
    if ($runId -match '^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})') { return "$($Matches[3])/$($Matches[2]) $($Matches[4]):$($Matches[5])" }
    return $runId
}

$parts = @($RunDirs | ForEach-Object { Read-Run $_ })
$current = $parts[-1]
if ($parts.Count -gt 1) {
    # Latest result per flow file across the merged runs.
    $merged = [ordered]@{}
    foreach ($p in $parts) { foreach ($c in $p.Cases) { $merged[$c.File] = $c } }
    $current = [pscustomobject]@{
        RunId = "$($parts.Count) ejecuciones · $(Get-RunDate (Split-Path $RunDirs[0] -Leaf)) → $(Get-RunDate (Split-Path $RunDirs[-1] -Leaf))"
        Device = ($parts | Where-Object Device | Select-Object -Last 1).Device; Start = $((Split-Path $RunDirs[0] -Leaf) -replace '^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*', '$1-$2-$3T$4:$5:$6')
        Seconds = ($parts | Measure-Object Seconds -Sum).Sum; Cases = @($merged.Values)
    }
}
$others = @($runs | Where-Object { $RunDirs -notcontains $_.FullName } | ForEach-Object { Read-Junit $_.FullName })

# ── Cases: every flow of the suite + multi-device scenarios ─────────────────────────
$cases = @()
Push-Location $script:MaestroRoot
foreach ($f in (Get-ChildItem (Join-Path $script:MaestroRoot 'flows') -Recurse -Filter *.yaml | Sort-Object FullName)) {
    $text = Get-Content $f.FullName -Raw -Encoding UTF8
    $section = if ($f.Directory.Name -match '^(\d{2})') { $Matches[1] } else { '--' }
    $name = if ($text -match 'name:\s*"([^"]+)"') { $Matches[1] } else { $f.BaseName }
    $tags = @([regex]::Matches(($text -split '(?m)^---')[0], '(?m)^\s+-\s+([a-z-]+)') | ForEach-Object { $_.Groups[1].Value })
    $r = $current.Cases | Where-Object { $_.File -eq $f.Name } | Select-Object -First 1
    $status = 'NO_EJECUTADO'; $reason = ''; $err = ''; $dur = 0
    $art = [pscustomobject]@{ Steps = @(); FailedStep = ''; Image = $null }
    if ($r) {
        $status = if ($r.Ok) { 'PASA' } else { 'FALLA' }; $err = $r.Error; $dur = $r.Seconds
        $art = Get-Artifacts $r.Dir $r.Name (-not $r.Ok)
        $rm = Join-Path $r.Dir 'run-meta.json'
        $caseKind = if (Test-Path $rm) { (Get-Content $rm -Raw -Encoding UTF8 | ConvertFrom-Json).kind } else { '' }
        if ($RunDirs.Count -gt 1 -and $tags -contains 'emulator-only' -and $caseKind -and $caseKind -ne 'emulador') {
            # Merged report: a result from before the flow was restricted to emulators.
            $status = 'NO_EJECUTADO'; $reason = "Solo emulador (usaría datos reales del móvil). Antes de excluirlo falló en este móvil: $($r.Error)"
            $err = ''; $art = [pscustomobject]@{ Steps = @(); FailedStep = ''; Image = $null }
        }
        # Whole flow behind a runFlow `when` that was false (e.g. QA3_USERNAME not configured).
        $real = @($art.Steps | Where-Object { $_.d -eq 0 -and $_.label -notmatch '^(applyConfiguration|defineVariables)' })
        if ($r.Ok -and $real.Count -and -not @($real | Where-Object { $_.st -ne 'SKIPPED' }).Count) {
            $status = 'NO_EJECUTADO'; $reason = 'Omitido: precondición no cumplida (p. ej. cuenta @claudeqa3 / QA3_USERNAME sin configurar)'
        }
    } elseif ($tags -contains 'known-bug') { $status = 'BUG'; $reason = 'Bug conocido (known-bug)' }
    elseif ($tags -contains 'emulator-only') { $reason = 'Solo emulador (usaría datos reales del móvil)' }
    elseif ($tags -contains 'destructive') { $reason = 'Destructivo (cuentas desechables)' }
    else { $reason = 'Fuera del filtro de esta tanda' }
    $trend = @($others | ForEach-Object {
            $o = $_; $hit = $o.Cases | Where-Object { $_.File -eq $f.Name } | Select-Object -First 1
            if ($hit) { [pscustomobject]@{ date = (Get-RunDate $o.RunId); st = $(if ($hit.Ok) { 'PASA' } else { 'FALLA' }) } }
        } | Select-Object -Last 5)
    $cases += [pscustomobject]@{
        id = "$section-$($f.BaseName)"; name = $name; file = ((Resolve-Path $f.FullName -Relative) -replace '^\.\\', '.maestro\')
        section = $section; sectionName = $sectionNames[$section]; status = $status; reason = $reason
        duration = [math]::Round($dur, 1); error = $err; failedStep = $art.FailedStep; steps = @($art.Steps); image = $art.Image
        tags = @($tags); trend = @($trend)
    }
}
Pop-Location
foreach ($sc in (Get-ChildItem (Join-Path $script:MaestroRoot 'multidevice') -Directory | Sort-Object Name)) {
    $destr = Test-Path (Join-Path $sc.FullName 'setup.ps1')
    $cases += [pscustomobject]@{
        id = 'MD-' + $sc.Name; name = "Escenario $($sc.Name)"; file = ".maestro\multidevice\$($sc.Name)"; section = 'MD'; sectionName = $sectionNames['MD']
        status = 'NO_EJECUTADO'
        reason = $(if ($destr) { 'Multi-dispositivo destructivo (run-multidevice.ps1 -IncludeDestructive)' } else { 'Multi-dispositivo (run-multidevice.ps1, dos emuladores)' })
        duration = 0; error = ''; failedStep = ''; steps = @(); image = $null; tags = @('multidevice'); trend = @()
    }
}

# ── Environment ─────────────────────────────────────────────────────────────────────
$serial = "$($current.Device)"
# junit names emulators by AVD (e.g. Pixel9ProXL_API36_A), not by serial.
$isEmu = $serial -like 'emulator-*' -or $serial -match '_API\d+'
$model = $serial; $android = '—'; $appVer = ''
$kind = if ($isEmu) { 'emulador' } else { 'móvil físico' }
$metaFile = Join-Path $RunDir 'run-meta.json'
if (-not (Test-Path $metaFile)) { $metaFile = @($RunDirs | ForEach-Object { Join-Path $_ 'run-meta.json' } | Where-Object { Test-Path $_ }) | Select-Object -Last 1 }
if (-not $metaFile) { $metaFile = Join-Path $RunDir 'run-meta.json' }
if (Test-Path $metaFile) {
    $meta = Get-Content $metaFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $serial = $meta.serial; $model = $meta.model; $android = $meta.android; $appVer = $meta.appVer; $kind = $meta.kind
}
elseif (@(& adb devices) -match "^$([regex]::Escape($serial))\s+device") {
    $model = ((Invoke-Adb $serial shell getprop ro.product.model) -join '').Trim()
    $android = ((Invoke-Adb $serial shell getprop ro.build.version.release) -join '').Trim()
    $appVer = ((@(Invoke-Adb $serial shell dumpsys package $script:AppId) -match 'versionName=') | Select-Object -First 1) -replace '.*versionName=', ''
}
$deviceLabel = if ($model -eq $serial) { $serial } else { "$model ($serial)" }
$repo = Split-Path $script:MaestroRoot -Parent
$branch = "$(& git -C $repo branch --show-current)".Trim()
$commit = "$(& git -C $repo rev-parse --short HEAD)".Trim()
$maestroVer = "$((& (Get-MaestroExe) --version 2>$null) | Select-Object -Last 1)".Trim()
$locale = if ((Split-Path $RunDir -Leaf) -match '-(es|en)$') { $Matches[1] } else { '?' }
$envRows = @(
    @('Dispositivo', $deviceLabel), @('Tipo', $kind), @('Android', $android),
    @('App', "com.ajrpachon.chatapp $appVer (debug)".Trim()), @('Maestro', $maestroVer),
    @('Código', "$branch @ $commit"), @('Idioma', $locale)
)

# ── History and bugs ──────────────────────────────────────────────────────────────────
$history = @(@($others) + @($parts) | Sort-Object RunId -Descending | Select-Object -First 12 | ForEach-Object {
        [pscustomobject]@{ runId = $_.RunId; device = $_.Device; total = @($_.Cases).Count; pass = @($_.Cases | Where-Object Ok).Count }
    })
$bugs = @()
$readme = Join-Path $script:MaestroRoot 'README.md'
if (Test-Path $readme) {
    $inBugs = $false
    foreach ($line in Get-Content $readme -Encoding UTF8) {
        if ($line -match '^## Bugs encontrados') { $inBugs = $true; continue }
        if ($inBugs -and $line -match '^## ') { break }
        if ($inBugs -and $line -match '^\|\s*([^|]+?)\s*\|\s*(.+?)\s*\|\s*$' -and $line -notmatch '^\|\s*(Estado|-{3})') {
            $bugs += [pscustomobject]@{ state = ($Matches[1] -replace '`', ''); text = ($Matches[2] -replace '`', '') }
        }
    }
}

$data = [pscustomobject]@{
    meta = [pscustomobject]@{
        runId = $current.RunId; date = $current.Start; device = $model; deviceKind = $kind
        locale = $locale; durationSec = [math]::Round($current.Seconds); branch = $branch; commit = $commit
    }
    env = $envRows; cases = @($cases); history = $history; bugs = $bugs
}
$json = ConvertTo-Json -InputObject $data -Depth 10 -Compress
$template = Get-Content (Join-Path $PSScriptRoot 'report-template.html') -Raw -Encoding UTF8
if ($Title) { $template = $template.Replace('<h1>Informe de ejecución E2E</h1>', "<h1>$([System.Net.WebUtility]::HtmlEncode($Title))</h1>") }
$html = $template.Replace('/*__DATA__*/', $json)
[IO.File]::WriteAllText($Output, $html, (New-Object System.Text.UTF8Encoding($false)))
$ranN = @($cases | Where-Object { $_.status -in 'PASA', 'FALLA' }).Count; $okN = @($cases | Where-Object status -eq 'PASA').Count
Write-Host "Informe: $Output  ($okN/$ranN OK, $(@($cases).Count) casos en la suite)"
