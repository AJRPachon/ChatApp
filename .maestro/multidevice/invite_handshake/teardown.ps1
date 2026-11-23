param([string]$RunId)
& "$PSScriptRoot/../../scripts/qa-accounts.ps1" -Action delete-run -RunId $RunId
