#!/usr/bin/env pwsh

# Fix R class imports in Samsung My Files Root Extension project
$projectPath = "C:\Users\kingslayer\Documents\Github\Kingslayer9988\Android\samsung_files\my-files-root-extension\Samsung-My-Files-Root-Extension"
$oldImport = "import com.samsung.cifs.ui.R"
$newImport = "import com.samsung.android.app.networkstoragemanager.presentation.R"

Write-Host "Fixing R class imports in Samsung My Files Root Extension project..."

# Get all Kotlin files in the presentation module
$kotlinFiles = Get-ChildItem -Path "$projectPath\presentation\src\main\java" -Recurse -Filter "*.kt"

$count = 0
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match [regex]::Escape($oldImport)) {
        $newContent = $content -replace [regex]::Escape($oldImport), $newImport
        Set-Content $file.FullName -Value $newContent -NoNewline
        Write-Host "Fixed: $($file.FullName)"
        $count++
    }
}

Write-Host "Fixed $count files."
Write-Host "Done!"
