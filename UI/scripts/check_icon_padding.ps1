# check_icon_padding.ps1
# Checks whether the given images have transparent padding at their edges.
# Usage: .\scripts\check_icon_padding.ps1

Add-Type -AssemblyName System.Drawing

$files = @(
    'app\src\main\res\mipmap-mdpi\ic_launcher.png',
    'app\src\main\res\mipmap-hdpi\ic_launcher.png',
    'app\src\main\res\mipmap-xhdpi\ic_launcher.png',
    'app\src\main\res\mipmap-xxhdpi\ic_launcher.png',
    'app\src\main\res\mipmap-xxxhdpi\ic_launcher.png',
    'app\src\main\res\mipmap-xxxhdpi\ic_launcher_playstore.png',
    'app\src\main\res\drawable\aqi_logo.png',
    'app\src\main\res\drawable-nodpi\aqi_logo.png'
)

function Check-TransparentEdges($path) {
    if (-not (Test-Path $path)) { return "$path -> MISSING" }
    try {
        $bmp = [System.Drawing.Bitmap]::FromFile($path)
        $w = $bmp.Width
        $h = $bmp.Height
        $edgePixels = @()
        # sample a few pixels near each edge (10 px inset)
        $inset = [Math]::Max(1, [Math]::Floor([Math]::Min($w,$h) * 0.05))
        for ($x = $inset; $x -lt ($w - $inset); $x += [Math]::Max(1, [Math]::Floor(($w - 2*$inset)/10))) {
            $edgePixels += $bmp.GetPixel($x, $inset).A
            $edgePixels += $bmp.GetPixel($x, $h - 1 - $inset).A
        }
        for ($y = $inset; $y -lt ($h - $inset); $y += [Math]::Max(1, [Math]::Floor(($h - 2*$inset)/10))) {
            $edgePixels += $bmp.GetPixel($inset, $y).A
            $edgePixels += $bmp.GetPixel($w - 1 - $inset, $y).A
        }
        $bmp.Dispose()
        $avgAlpha = [Math]::Round(($edgePixels | Measure-Object -Average).Average,2)
        return "$path -> ${w}x${h} avg-edge-alpha=$avgAlpha"
    } catch {
        return "$path -> ERROR: $_"
    }
}

foreach ($f in $files) {
    Write-Output (Check-TransparentEdges $f)
}

Write-Output "\nInterpretation: avg-edge-alpha close to 0 means fully transparent edges (good padding). Close to 255 means opaque edges (no padding)."
