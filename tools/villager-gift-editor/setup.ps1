# Create a local venv and install runtime deps (avoids writing to system Python\Scripts).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$venvPython = Join-Path $PSScriptRoot ".venv\Scripts\python.exe"
if (-not (Test-Path $venvPython)) {
    Write-Host "Creating virtual environment in .venv ..."
    python -m venv .venv
}

Write-Host "Installing PySide6 into .venv ..."
& $venvPython -m pip install --upgrade pip
& $venvPython -m pip install -r requirements.txt

Write-Host ""
Write-Host "Done. Run the editor with: .\run.ps1"
