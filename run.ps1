# Build (if needed) and launch Desktop Pets.
#
# Usage:
#   .\run.ps1            # build if the jar is missing, then run
#   .\run.ps1 -Rebuild   # always rebuild first
param([switch]$Rebuild)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jar = Join-Path $root 'target\desktop-pets.jar'

if ($Rebuild -or -not (Test-Path $jar)) {
    Write-Host 'Building...' -ForegroundColor Cyan
    & mvn -q -f (Join-Path $root 'pom.xml') clean package
    if (-not $?) { throw 'Build failed.' }
}

Write-Host 'Launching Desktop Pets. Right-click a pet for its menu.' -ForegroundColor Green
# javaw keeps the app running without a console window hanging around.
$javaw = Join-Path (Split-Path (Get-Command java).Source) 'javaw.exe'
if (Test-Path $javaw) {
    Start-Process -FilePath $javaw -ArgumentList '-jar', $jar
} else {
    Start-Process -FilePath 'java' -ArgumentList '-jar', $jar -WindowStyle Hidden
}
