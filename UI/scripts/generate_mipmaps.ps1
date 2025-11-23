# generate_mipmaps.ps1
# Usage: run from project root (where app\ exists)
# .\scripts\generate_mipmaps.ps1

$src = 'app\src\main\res\drawable\aqi_logo.png'
$projRes = 'app\src\main\res'

if (-not (Test-Path $src)) {
    Write-Error "Source image not found: $src"
    exit 1
}

# sizes: folder -> pixel size (square)
$sizes = @{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

# ensure folders exist
foreach ($f in $sizes.Keys) {
    $dir = Join-Path $projRes $f
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
}

# Play Store icon
$playDir = Join-Path $projRes 'mipmap-xxxhdpi'
if (-not (Test-Path $playDir)) { New-Item -ItemType Directory -Path $playDir -Force | Out-Null }

# Helper: uses ImageMagick if available, otherwise .NET fallback
function Resize-WithMagick($in, $out, $size) {
    $magick = Get-Command magick -ErrorAction SilentlyContinue
    if (-not $magick) { return $false }
    # Use magick (IMv7): magick input -resize WxH output
    $cmd = @($in, '-resize', "${size}x${size}", $out)
    try {
        & magick @cmd
        return $true
    } catch {
        Write-Warning "ImageMagick failed for $($out): $($_)"
        return $false
    }
}

function Resize-WithDotNet($inFile, $outFile, $size) {
    Add-Type -AssemblyName System.Drawing
    try {
        $srcImg = [System.Drawing.Image]::FromFile($inFile)
        $bmp = New-Object System.Drawing.Bitmap $size, $size
        $gfx = [System.Drawing.Graphics]::FromImage($bmp)
        $gfx.Clear([System.Drawing.Color]::Transparent)
        $gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $gfx.DrawImage($srcImg, [System.Drawing.Rectangle]::new(0,0,$size,$size))
        $gfx.Dispose()
        $srcImg.Dispose()
        $bmp.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        return $true
    } catch {
        Write-Warning ".NET resize failed for $($outFile): $($_)"
        return $false
    }
}

# Generate each mipmap icon
foreach ($kv in $sizes.GetEnumerator()) {
    $folder = $kv.Key
    $px = $kv.Value
    $out = Join-Path $projRes "$folder\ic_launcher.png"
    Write-Output "Generating $out (${px}x${px})..."
    $ok = Resize-WithMagick $src $out $px
    if (-not $ok) { $ok = Resize-WithDotNet $src $out $px }
    if (-not $ok) { Write-Error "Failed to create $out"; exit 2 }
}

# Generate adaptive icon foreground copies (optional) and playstore icon
$playOut = Join-Path $projRes 'mipmap-xxxhdpi\ic_launcher_playstore.png'
Write-Output "Generating Play Store icon $playOut (512x512)..."
$ok = Resize-WithMagick $src $playOut 512
if (-not $ok) { $ok = Resize-WithDotNet $src $playOut 512 }
if (-not $ok) { Write-Error "Failed to create Play Store icon"; exit 3 }

Write-Output "All icons generated. Please rebuild with: .\gradlew assembleDebug"