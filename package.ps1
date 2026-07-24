# Build a self-contained Windows app image with jpackage.
#
#   .\package.ps1            # -> dist\DesktopPets\DesktopPets.exe
#   .\package.ps1 -Zip       # also produce dist\DesktopPets-windows.zip
#
# The result bundles a trimmed Java runtime, so whoever runs it needs no JDK, no Maven, and no
# JavaFX — they double-click the exe. jpackage ships with JDK 21, so there is nothing extra to
# install to produce it. `--type app-image` is used deliberately rather than `msi`, because the msi
# and exe installer types additionally require WiX to be installed.
param([switch]$Zip)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jar = Join-Path $root 'target\desktop-pets.jar'
$stage = Join-Path $root 'target\jpackage-input'
$dist = Join-Path $root 'dist'

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw 'jpackage not found on PATH. It ships with JDK 21 — check that the JDK (not just a JRE) is installed.'
}

if (-not (Test-Path $jar)) {
    Write-Host 'Jar missing; building first...' -ForegroundColor Cyan
    & mvn -q -f (Join-Path $root 'pom.xml') clean package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed.' }
}

# Stage only the fat jar. jpackage copies its whole --input directory into the image, and target\
# also holds the unshaded original-desktop-pets.jar.
if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
New-Item -ItemType Directory -Force $stage | Out-Null
Copy-Item $jar $stage

if (Test-Path (Join-Path $dist 'DesktopPets')) {
    Remove-Item -Recurse -Force (Join-Path $dist 'DesktopPets')
}
New-Item -ItemType Directory -Force $dist | Out-Null

Write-Host 'Running jpackage (this takes a minute)...' -ForegroundColor Cyan
& jpackage `
    --type app-image `
    --name DesktopPets `
    --app-version '0.1.0' `
    --vendor 'Gaurav Sharma' `
    --description 'Chubby puppies that live on your Windows desktop' `
    --input $stage `
    --main-jar 'desktop-pets.jar' `
    --main-class 'dev.gauravs.desktoppets.Launcher' `
    --dest $dist
# Note: --win-console is deliberately *not* passed. It is a bare flag that attaches a console
# window; omitting it is what gives the pets a windowless launch.
if ($LASTEXITCODE -ne 0) { throw "jpackage failed (exit $LASTEXITCODE)." }

$exe = Join-Path $dist 'DesktopPets\DesktopPets.exe'
Write-Host "Built $exe" -ForegroundColor Green

if ($Zip) {
    # Not $zip — PowerShell variable names are case-insensitive, so that would clobber the -Zip switch.
    $zipPath = Join-Path $dist 'DesktopPets-windows.zip'
    if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
    Compress-Archive -Path (Join-Path $dist 'DesktopPets') -DestinationPath $zipPath
    Write-Host "Zipped $zipPath ($([math]::Round((Get-Item $zipPath).Length / 1MB, 1)) MB)" -ForegroundColor Green
}
