# One-shot setup: install the prerequisites if they are missing, build, and run.
#
#   .\setup.ps1              # install what's missing, build, launch
#   .\setup.ps1 -NoRun       # install and build only
#
# Installs are done with winget (built into Windows 10 1809+ / Windows 11). Nothing is installed
# that is already present, and nothing is installed silently without being named below:
#
#   Microsoft.OpenJDK.21   JDK 21 — the language level this project targets
#   Apache.Maven           the build tool
#
# JavaFX itself needs no installation: it resolves as ordinary Maven dependencies and is shaded
# into the output jar.
param([switch]$NoRun)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

function Have($name) {
    return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

function Install-With-Winget($id, $label) {
    if (-not (Have 'winget')) {
        throw "winget is not available, so $label cannot be installed automatically. Install $label manually, then re-run this script."
    }
    Write-Host "Installing $label ($id)..." -ForegroundColor Cyan
    winget install --id $id --exact --accept-source-agreements --accept-package-agreements --disable-interactivity
    if ($LASTEXITCODE -ne 0) { throw "winget failed to install $label (exit $LASTEXITCODE)." }
}

# --- Java ---------------------------------------------------------------------------------------
$javaOk = $false
if (Have 'java') {
    # "openjdk version "21.0.10" ..." -> 21
    $verLine = (& java -version 2>&1 | Select-Object -First 1)
    if ($verLine -match '"(\d+)') { $javaOk = [int]$Matches[1] -ge 21 }
    if (-not $javaOk) { Write-Host "Found Java, but it is older than 21: $verLine" -ForegroundColor Yellow }
}
if (-not $javaOk) {
    Install-With-Winget 'Microsoft.OpenJDK.21' 'JDK 21'
} else {
    Write-Host 'JDK 21+ already present.' -ForegroundColor DarkGray
}

# --- Maven --------------------------------------------------------------------------------------
if (-not (Have 'mvn')) {
    Install-With-Winget 'Apache.Maven' 'Apache Maven'
} else {
    Write-Host 'Maven already present.' -ForegroundColor DarkGray
}

# winget updates the machine PATH, but this already-running shell will not see it.
if (-not (Have 'mvn') -or -not (Have 'java')) {
    Write-Host ''
    Write-Host 'Prerequisites installed, but this shell has a stale PATH.' -ForegroundColor Yellow
    Write-Host 'Open a new terminal and run:  .\setup.ps1' -ForegroundColor Yellow
    exit 0
}

# --- Build --------------------------------------------------------------------------------------
Write-Host 'Building...' -ForegroundColor Cyan
& mvn -q -f (Join-Path $root 'pom.xml') clean package
if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
Write-Host "Built $(Join-Path $root 'target\desktop-pets.jar')" -ForegroundColor Green

if (-not $NoRun) {
    & (Join-Path $root 'run.ps1')
}
