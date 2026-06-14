# Run from: tools/villager-gift-editor
# First-time setup: .\setup.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$venvPython = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
if (Test-Path $venvPython) {
    $python = $venvPython
} else {
    $python = "python"
    Write-Warning "No .venv found. Run .\setup.ps1 first, or: pip install -r requirements.txt"
}

$env:PYTHONPATH = Join-Path $PSScriptRoot "src"
& $python -m gift_editor.main @args
