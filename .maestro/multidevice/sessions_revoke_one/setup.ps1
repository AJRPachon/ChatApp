param([string]$RunId)
# Disposable account signed in on both emulators (needs SUPABASE_SERVICE_ROLE_KEY in .maestro/.env).
$u = & "$PSScriptRoot/../../scripts/qa-accounts.ps1" -Action create -Label session1 -RunId $RunId
return @{ DISPOSABLE_EMAIL = $u.email; DISPOSABLE_PASSWORD = $u.password; DISPOSABLE_USERNAME = "qas1$RunId" }
