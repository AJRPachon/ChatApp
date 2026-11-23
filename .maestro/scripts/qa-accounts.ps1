<#
.SYNOPSIS
  Creates / deletes disposable Supabase users for the destructive Maestro flows.
.DESCRIPTION
  Uses the Supabase Auth Admin API with SUPABASE_SERVICE_ROLE_KEY from .maestro/.env (local only,
  never in CI). Created users are email-confirmed so they can sign in right away; they have no
  username yet, so the app shows the username-setup screen on first sign-in.
.EXAMPLE
  $u = ./qa-accounts.ps1 -Action create -Label mfa        # prints and returns {id,email,password}
  ./qa-accounts.ps1 -Action delete -Email $u.email
  ./qa-accounts.ps1 -Action delete-run -RunId 20261123120000  # every disposable of one run
  ./qa-accounts.ps1 -Action purge                          # every disposable user (qa.disposable+*)
#>
param(
    [ValidateSet('create', 'delete', 'delete-run', 'purge')][string]$Action = 'create',
    [string]$Label = 'user',
    [string]$RunId = '',
    [string]$Email = ''
)
. "$PSScriptRoot/common.ps1"

$vars = Read-MaestroEnv
foreach ($k in @('SUPABASE_URL', 'SUPABASE_SERVICE_ROLE_KEY', 'DISPOSABLE_PASSWORD')) {
    if (-not $vars[$k]) { throw "Falta $k en .maestro/.env" }
}
$base = $vars['SUPABASE_URL'].TrimEnd('/') + '/auth/v1/admin/users'
$headers = @{
    apikey        = $vars['SUPABASE_SERVICE_ROLE_KEY']
    Authorization = "Bearer $($vars['SUPABASE_SERVICE_ROLE_KEY'])"
}
$prefix = 'qa.disposable+'
$domain = '@example.com'

function Get-DisposableUsers {
    $all = @()
    $page = 1
    do {
        $resp = Invoke-RestMethod -Method Get -Uri "$base`?page=$page&per_page=200" -Headers $headers
        $batch = @($resp.users)
        $all += $batch
        $page++
    } while ($batch.Count -eq 200)
    return $all | Where-Object { $_.email -like "$prefix*$domain" }
}

function Remove-User([string]$id, [string]$mail) {
    Invoke-RestMethod -Method Delete -Uri "$base/$id" -Headers $headers | Out-Null
    Write-Host "borrado $mail"
}

switch ($Action) {
    'create' {
        if (-not $RunId) { $RunId = New-RunId }
        $mail = "$prefix$RunId-$Label$domain"
        $body = @{ email = $mail; password = $vars['DISPOSABLE_PASSWORD']; email_confirm = $true } | ConvertTo-Json
        $user = Invoke-RestMethod -Method Post -Uri $base -Headers $headers -Body $body -ContentType 'application/json'
        $result = [pscustomobject]@{ id = $user.id; email = $mail; password = $vars['DISPOSABLE_PASSWORD'] }
        Write-Host "creado $mail ($($user.id))"
        return $result
    }
    'delete' {
        $u = Get-DisposableUsers | Where-Object { $_.email -eq $Email }
        if (-not $u) { Write-Host "no existe $Email"; return }
        Remove-User $u.id $u.email
    }
    'delete-run' {
        Get-DisposableUsers | Where-Object { $_.email -like "$prefix$RunId-*" } | ForEach-Object { Remove-User $_.id $_.email }
    }
    'purge' {
        Get-DisposableUsers | ForEach-Object { Remove-User $_.id $_.email }
    }
}
