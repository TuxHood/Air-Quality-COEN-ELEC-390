# pad_logo.ps1
# Adds transparent padding around app/src/main/res/drawable/aqi_logo.png
# Usage: run from project root
# .\scripts\pad_logo.ps1 -Padding 40

param(
    [int]$Padding = 40,
    [double]$ContentRatio = 0.0,
    [int]$MaxSize = 0,
    [string]$Src = 'app\src\main\res\drawable\aqi_logo.png',
    [switch]$InPlace = $false,
    [string]$Out = ''
)

if (-not (Test-Path $Src)) {
    Write-Error "Source image not found: $Src"
    exit 1
}

Add-Type -AssemblyName System.Drawing

try {
    $img = [System.Drawing.Image]::FromFile($Src)

    if ($ContentRatio -gt 0) {
        # Compute padding so visible content occupies ContentRatio of the shortest side
        $short = [Math]::Min($img.Width, $img.Height)
        $target = [Math]::Floor($short * $ContentRatio)
        $padPerSide = [Math]::Max(0, [Math]::Floor(( $short - $target ) / 2))
        $Padding = $padPerSide
        Write-Output "Computed padding = $Padding px from ContentRatio=$ContentRatio (target content size = $target)"
    }

    $newW = $img.Width + 2 * $Padding
    $newH = $img.Height + 2 * $Padding
    $bmp = New-Object System.Drawing.Bitmap $newW, $newH
    $gfx = [System.Drawing.Graphics]::FromImage($bmp)
    $gfx.Clear([System.Drawing.Color]::Transparent)
    $gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gfx.DrawImage($img, $Padding, $Padding, $img.Width, $img.Height)
    $gfx.Dispose()
    $img.Dispose()

    # When overwriting the source in-place, make a backup in scripts/backups.
    $backup = ''
    if ($InPlace) {
        $backupDir = 'scripts\backups'
        if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir -Force | Out-Null }
        $base = Split-Path $Src -Leaf
        $backup = Join-Path $backupDir ($base + '.bak')
        if (-not (Test-Path $backup)) { Copy-Item -Path $Src -Destination $backup -Force }
        Write-Output "Backup of original saved to: $backup"
    } else {
        Write-Output "Not running in-place; original file will not be overwritten. Use -InPlace to overwrite the source." 
    }

    # Optionally resize the padded image down to MaxSize (preserve aspect ratio)
    if ($MaxSize -gt 0) {
        $targetW = $bmp.Width
        $targetH = $bmp.Height
        if ($targetW -gt $MaxSize -or $targetH -gt $MaxSize) {
            if ($targetW -gt $targetH) {
                $scale = $MaxSize / [double]$targetW
            } else {
                $scale = $MaxSize / [double]$targetH
            }
            $resW = [Math]::Max(1, [Math]::Floor($bmp.Width * $scale))
            $resH = [Math]::Max(1, [Math]::Floor($bmp.Height * $scale))
            $resized = New-Object System.Drawing.Bitmap $resW, $resH
            $g2 = [System.Drawing.Graphics]::FromImage($resized)
            $g2.Clear([System.Drawing.Color]::Transparent)
            $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g2.DrawImage($bmp, 0, 0, $resW, $resH)
            $g2.Dispose()
            $bmp.Dispose()
            $bmp = $resized
            Write-Output "Resized padded image to ${resW}x${resH} (MaxSize=$MaxSize)"
        } else {
            Write-Output "Padded image smaller than MaxSize; no resize needed"
        }
    }

    # Decide output path: if InPlace, overwrite Src; otherwise write to Out (or default padded filename)
    if ($InPlace) {
        $outputPath = $Src
    } else {
        if ([string]::IsNullOrWhiteSpace($Out)) {
            $dir = Split-Path $Src -Parent
            $name = [System.IO.Path]::GetFileNameWithoutExtension($Src)
            $ext = [System.IO.Path]::GetExtension($Src)
            $outputPath = Join-Path $dir ($name + '_padded' + $ext)
        } else {
            $outputPath = $Out
        }
    }

    $bmp.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    if ($InPlace) {
        Write-Output "Padded $Src in-place with $Padding px (backup: $backup)"
    } else {
        Write-Output "Padded image saved to: $outputPath (original left unchanged at $Src)"
    }
    Write-Output "Next: run .\scripts\generate_mipmaps.ps1 then rebuild the app."
    exit 0
} catch {
    Write-Error "Failed to pad image: $_"
    exit 2
}
