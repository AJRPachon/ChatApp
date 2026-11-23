param([string]$RunId)
# Disposable account that invites @claudeqa2 (needs SUPABASE_SERVICE_ROLE_KEY in .maestro/.env).
$u = & "$PSScriptRoot/../../scripts/qa-accounts.ps1" -Action create -Label inviter -RunId $RunId
return @{ DISPOSABLE_EMAIL = $u.email; DISPOSABLE_PASSWORD = $u.password; DISPOSABLE_USERNAME = "qainv$RunId" }
