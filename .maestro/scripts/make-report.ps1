<#
.SYNOPSIS
  Builds a self-contained HTML execution report from a Maestro run in .maestro/out.
.DESCRIPTION
  Reads the run's JUnit report (report.xml) and, for every failed flow, its commands.json (the
  failed step) and last screenshot (embedded). Also lists the flows of the suite that were not
  executed in that run (with their tags, e.g. emulator-only / multidevice / known-bug).
.EXAMPLE
  ./make-report.ps1                                  # latest run
  ./make-report.ps1 -RunDir .maestro/out/20261124083253-es -Title "Pixel 9 - es"
#>
param(
    [string]$RunDir = '',
    [string]$Output = '',
    [string]$Title = 'Informe de ejecución E2E (Maestro)'
)
. "$PSScriptRoot/common.ps1"

$outRoot = Join-Path $script:MaestroRoot 'out'
if (-not $RunDir) {
    $RunDir = (Get-ChildItem $outRoot -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'report.xml') } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
}
$RunDir = (Resolve-Path $RunDir).Path
$junit = Join-Path $RunDir 'report.xml'
if (-not (Test-Path $junit)) { throw "No hay report.xml en $RunDir" }
if (-not $Output) { $Output = Join-Path $RunDir 'report.html' }

[xml]$xml = Get-Content $junit -Encoding UTF8
$suite = $xml.testsuites.testsuite
$cases = @($suite.testcase)

function HtmlEnc([string]$s) { [System.Net.WebUtility]::HtmlEncode($s) }

function Get-FlowArtifacts([string]$flowName) {
    # Maestro names the artifact folder after the flow, replacing characters invalid in paths.
    $safe = ($flowName -replace '[\\/:*?"<>|]', '_')
    $dir = Get-ChildItem $RunDir -Recurse -Directory | Where-Object { $_.Name -eq $safe } | Select-Object -First 1
    if (-not $dir) { return $null }
    $failedStep = $null
    $cmdFile = Join-Path $dir.FullName 'commands.json'
    if (Test-Path $cmdFile) {
        $cmds = Get-Content $cmdFile -Raw -Encoding UTF8 | ConvertFrom-Json
        $failed = @($cmds | Where-Object { $_.metadata.status -eq 'FAILED' }) | Select-Object -Last 1
        if ($failed) {
            $name = ($failed.command.PSObject.Properties | Select-Object -First 1).Name
            $sel = $failed.command.$name.selector
            if (-not $sel) { $sel = $failed.command.$name.condition.visible }
            if (-not $sel) { $sel = $failed.command.$name.condition.notVisible }
            $target = if ($sel.idRegex) { "id: $($sel.idRegex)" } elseif ($sel.textRegex) { "texto: $($sel.textRegex)" } else { '' }
            $failedStep = "$name $target".Trim()
        }
    }
    $shot = Get-ChildItem (Join-Path $dir.FullName 'screenshots') -Filter *.png -ErrorAction SilentlyContinue |
        Sort-Object Name | Select-Object -Last 1
    $img = if ($shot) { 'data:image/png;base64,' + [Convert]::ToBase64String([IO.File]::ReadAllBytes($shot.FullName)) } else { $null }
    return [pscustomobject]@{ Step = $failedStep; Image = $img }
}

# ── Rows ─────────────────────────────────────────────────────────────────────
$rows = foreach ($c in $cases) {
    $tags = ($c.properties.property | Where-Object { $_.name -eq 'tags' }).value
    $section = if ($c.name -match '^(\d{2})') { $Matches[1] } else { '--' }
    $failed = $c.status -ne 'SUCCESS' -or $c.failure
    # Maestro writes the failure text as the element's content (no message attribute).
    $msg = if ($c.failure) { if ($c.failure -is [string]) { $c.failure } elseif ($c.failure.message) { $c.failure.message } else { $c.failure.InnerText } } else { '' }
    [pscustomobject]@{
        Name = $c.name; File = $c.file; Section = $section; Tags = $tags
        Seconds = [double]$c.time; Failed = [bool]$failed; Message = $msg
    }
}
$total = $rows.Count
$failedRows = @($rows | Where-Object Failed)
$passed = $total - $failedRows.Count
$pct = if ($total) { [math]::Round(100 * $passed / $total) } else { 0 }
$duration = [timespan]::FromSeconds([double]$suite.time)

# Flows of the suite that were not part of this run
$executedFiles = $rows | ForEach-Object { [IO.Path]::GetFileName($_.File) }
$notRun = Get-ChildItem (Join-Path $script:MaestroRoot 'flows') -Recurse -Filter *.yaml |
    Where-Object { $_.Name -notin $executedFiles } | ForEach-Object {
        $text = Get-Content $_.FullName -Raw -Encoding UTF8
        $n = if ($text -match 'name:\s*"([^"]+)"') { $Matches[1] } else { $_.Name }
        $t = ([regex]::Matches($text, '(?m)^\s+-\s+([a-z-]+)') | ForEach-Object { $_.Groups[1].Value } |
            Where-Object { $_ -in @('emulator-only', 'known-bug', 'destructive', 'multidevice', 'slow') }) -join ', '
        [pscustomobject]@{ Name = $n; Tags = $t }
    }

# ── HTML ─────────────────────────────────────────────────────────────────────
$sectionRows = $rows | Group-Object Section | Sort-Object Name | ForEach-Object {
    $ok = @($_.Group | Where-Object { -not $_.Failed }).Count
    $state = if ($ok -eq $_.Count) { 'ok' } else { 'ko' }
    "<tr><td>$($_.Name)</td><td>$($_.Count)</td><td>$ok</td><td>$($_.Count - $ok)</td><td><span class='pill $state'>$(if ($state -eq 'ok') { 'OK' } else { 'Fallos' })</span></td></tr>"
}
$flowRows = $rows | Sort-Object Name | ForEach-Object {
    $state = if ($_.Failed) { "<span class='pill ko'>Falla</span>" } else { "<span class='pill ok'>OK</span>" }
    "<tr><td>$state</td><td>$(HtmlEnc $_.Name)</td><td class='muted'>$(HtmlEnc $_.Tags)</td><td class='num'>$([math]::Round($_.Seconds))s</td></tr>"
}
$failureCards = foreach ($f in ($failedRows | Sort-Object Name)) {
    $a = Get-FlowArtifacts $f.Name
    $img = if ($a -and $a.Image) { "<img src='$($a.Image)' alt='Captura del fallo de $(HtmlEnc $f.Name)'>" } else { "<p class='muted'>Sin captura</p>" }
    $step = if ($a -and $a.Step) { "<p><b>Paso:</b> <code>$(HtmlEnc $a.Step)</code></p>" } else { '' }
    "<article class='card'><div><h3>$(HtmlEnc $f.Name)</h3><p class='muted'><code>$(HtmlEnc $f.File)</code></p>$step<p><b>Error:</b> $(HtmlEnc $f.Message)</p></div>$img</article>"
}
$notRunRows = $notRun | Sort-Object Name | ForEach-Object { "<tr><td>$(HtmlEnc $_.Name)</td><td class='muted'>$(HtmlEnc $_.Tags)</td></tr>" }

$html = @"
<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Informe E2E</title>
<style>
:root { --bg:#f6f7f9; --card:#fff; --text:#1b1f24; --muted:#5d6670; --line:#e2e5e9; --ok:#1f7a3d; --okbg:#e3f4e8; --ko:#b3261e; --kobg:#fbe5e3; }
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { --bg:#121417; --card:#1b1e22; --text:#e7e9ec; --muted:#9aa3ad; --line:#2c3137; --ok:#7ad69a; --okbg:#173322; --ko:#ff8a80; --kobg:#3a1b19; } }
:root[data-theme="dark"] { --bg:#121417; --card:#1b1e22; --text:#e7e9ec; --muted:#9aa3ad; --line:#2c3137; --ok:#7ad69a; --okbg:#173322; --ko:#ff8a80; --kobg:#3a1b19; }
* { box-sizing: border-box; }
body { margin:0; background:var(--bg); color:var(--text); font:15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
main { max-width:1100px; margin:0 auto; padding:24px 16px 48px; }
h1 { font-size:1.6rem; margin:0 0 4px; } h2 { margin:32px 0 12px; font-size:1.2rem; } h3 { margin:0 0 6px; font-size:1rem; }
.muted { color:var(--muted); } code { font-size:.85em; overflow-wrap:anywhere; }
.kpis { display:grid; grid-template-columns:repeat(auto-fit,minmax(150px,1fr)); gap:12px; margin-top:16px; }
.kpi { background:var(--card); border:1px solid var(--line); border-radius:12px; padding:14px; }
.kpi b { display:block; font-size:1.6rem; }
table { width:100%; border-collapse:collapse; background:var(--card); border:1px solid var(--line); border-radius:12px; overflow:hidden; }
th, td { padding:8px 10px; border-bottom:1px solid var(--line); text-align:left; vertical-align:top; }
th { font-size:.8rem; text-transform:uppercase; color:var(--muted); }
.num { text-align:right; white-space:nowrap; }
.pill { display:inline-block; padding:1px 8px; border-radius:999px; font-size:.8rem; font-weight:600; }
.ok { color:var(--ok); background:var(--okbg); } .ko { color:var(--ko); background:var(--kobg); }
.card { display:grid; grid-template-columns:1fr 220px; gap:16px; background:var(--card); border:1px solid var(--line); border-radius:12px; padding:14px; margin-bottom:12px; }
.card img { width:100%; border-radius:8px; border:1px solid var(--line); }
.scroll { overflow-x:auto; }
@media (max-width:640px) { .card { grid-template-columns:1fr; } .card img { max-width:260px; } }
</style>
</head>
<body><main>
<h1>$(HtmlEnc $Title)</h1>
<p class="muted">Ejecución $(HtmlEnc (Split-Path $RunDir -Leaf)) · dispositivo $(HtmlEnc $suite.device) · inicio $(HtmlEnc $suite.timestamp)</p>
<section class="kpis">
  <div class="kpi"><span class="muted">Flows ejecutados</span><b>$total</b></div>
  <div class="kpi"><span class="muted">Pasan</span><b style="color:var(--ok)">$passed</b></div>
  <div class="kpi"><span class="muted">Fallan</span><b style="color:var(--ko)">$($failedRows.Count)</b></div>
  <div class="kpi"><span class="muted">Éxito</span><b>$pct %</b></div>
  <div class="kpi"><span class="muted">Duración</span><b>$([math]::Floor($duration.TotalMinutes)) min</b></div>
</section>
<h2>Por sección</h2>
<div class="scroll"><table><thead><tr><th>Sección</th><th>Flows</th><th>OK</th><th>Fallan</th><th>Estado</th></tr></thead><tbody>
$($sectionRows -join "`n")
</tbody></table></div>
$(if ($failedRows.Count) { "<h2>Fallos</h2>" + ($failureCards -join "`n") })
<h2>Todos los flows</h2>
<div class="scroll"><table><thead><tr><th>Estado</th><th>Flow</th><th>Tags</th><th class="num">Duración</th></tr></thead><tbody>
$($flowRows -join "`n")
</tbody></table></div>
$(if ($notRun) { "<h2>No ejecutados en esta ejecución</h2><p class='muted'>Excluidos por tag (p. ej. emulator-only en un móvil físico, multidevice, known-bug) o fuera del filtro usado.</p><div class='scroll'><table><thead><tr><th>Flow</th><th>Tags</th></tr></thead><tbody>" + ($notRunRows -join "`n") + "</tbody></table></div>" })
</main></body></html>
"@

Set-Content -Path $Output -Value $html -Encoding UTF8
Write-Host "Informe: $Output  ($passed/$total OK)"
