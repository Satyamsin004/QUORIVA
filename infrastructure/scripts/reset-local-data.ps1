# CS Scholar AI — Local Development Data Purge & Clean-State Reset (PowerShell)
param (
    [switch]$Confirm,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "CS Scholar AI — Local Development Data Reset" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

# 1. Check if virtual environment python exists
$PythonExe = Join-Path $PSScriptRoot "..\..\ai-service\.venv\Scripts\python.exe"
if (-not (Test-Path $PythonExe)) {
    $PythonExe = "python"
}

# 2. Check if containers are running
Write-Host "Checking local backing containers..." -ForegroundColor Yellow
$RunningContainers = @(docker ps --format "{{.Names}}")
if ($RunningContainers -notcontains "cs-scholar-postgres") {
    Write-Host "Warning: cs-scholar-postgres is not running. Starting infrastructure..." -ForegroundColor Yellow
    docker compose up -d
    Start-Sleep -Seconds 5
}

# 3. Build script arguments
$ScriptPath = Join-Path $PSScriptRoot "reset_local_data.py"
$ArgsList = @($ScriptPath)
if ($Confirm) {
    $ArgsList += "--confirm"
}
if ($DryRun) {
    $ArgsList += "--dry-run"
}

# 4. Execute python reset script
Write-Host "Running reset script with $PythonExe..." -ForegroundColor Cyan
& $PythonExe $ArgsList
if ($LASTEXITCODE -ne 0) {
    Write-Host "Data reset failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

