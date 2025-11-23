Add-Type -AssemblyName System.Drawing

$paths = @(
    'app\\src\\main\\res\\drawable\\aqi_logo.png',
    'app\\src\\main\\res\\drawable\\aqi_logo_candidate98.png',
    'app\\src\\main\\res\\drawable-nodpi\\aqi_logo.png'
)

foreach ($p in $paths) {
    if (-not (Test-Path $p)) { Write-Output "MISSING: $p"; continue }
    $bmp = [System.Drawing.Bitmap]::FromFile($p)
    $w = $bmp.Width
    $h = $bmp.Height
    Write-Output "-- $p ($w x $h) --"
    $coords = @(
        @{x=0; y=0},
        @{x=0; y=($h-1)},
        @{x=($w-1); y=0},
        @{x=($w-1); y=($h-1)},
        @{x=[int]($w/2); y=0},
        @{x=[int]($w/2); y=($h-1)},
        @{x=0; y=[int]($h/2)},
        @{x=($w-1); y=[int]($h/2)}
    )
    foreach ($c in $coords) {
        $col = $bmp.GetPixel($c.x, $c.y)
        Write-Output ("{0},{1}: A={2} R={3} G={4} B={5}" -f $c.x, $c.y, $col.A, $col.R, $col.G, $col.B)
    }
    $bmp.Dispose()
}
