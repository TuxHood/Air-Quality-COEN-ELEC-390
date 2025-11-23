# copy_to_nodpi.ps1
# Copies the current aqi_logo.png to drawable-nodpi so Android won't scale it per-density.
# Usage: run from project root: .\scripts\copy_to_nodpi.ps1

$src = 'app\src\main\res\drawable\aqi_logo.png'
$destDir = 'app\src\main\res\drawable-nodpi'
$dest = Join-Path $destDir 'aqi_logo.png'

if (-not (Test-Path $src)) {
    Write-Error "Source not found: $src"
    exit 1
}
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
}
Copy-Item -Path $src -Destination $dest -Force
Write-Output "Copied $src -> $dest"
