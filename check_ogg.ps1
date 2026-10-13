$ffmpeg = ".\ffmpeg.exe"
$folder = "src\main\resources\assets\customjukebox\sounds\tracks"

$bad = @()
Get-ChildItem "$folder\disc_*.ogg" | Sort-Object { [int]($_.BaseName -replace 'disc_','') } | ForEach-Object {
    $file = $_.FullName
    $name = $_.Name
    Write-Host "Checking $name..."
    $output = & $ffmpeg -v error -i "$file" -f null - 2>&1
    if ($output) {
        Write-Host "  PROBLEM: $name" -ForegroundColor Red
        $bad += $name
    }
}

Write-Host ""
Write-Host "=== RESULT ==="
if ($bad.Count -eq 0) {
    Write-Host "No broken files found." -ForegroundColor Green
} else {
    Write-Host "Broken/problem files:" -ForegroundColor Red
    foreach ($item in $bad) {
        Write-Host "  $item"
    }
}
