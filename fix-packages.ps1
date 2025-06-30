# PowerShell Script to Fix All Package References
# Run this from the Samsung-My-Files-Root-Extension directory

Write-Host "Fixing all package references in copied CIFS files..."

# Get all Kotlin files in the project
$kotlinFiles = Get-ChildItem -Path "." -Filter "*.kt" -Recurse

foreach ($file in $kotlinFiles) {
    Write-Host "Processing: $($file.FullName)"
    
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Fix all package declarations
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.common', 'package com.samsung.cifs.common'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.db', 'package com.samsung.cifs.data.db'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.preference', 'package com.samsung.cifs.data.preference'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.interfaces', 'package com.samsung.cifs.storage'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.smbj', 'package com.samsung.cifs.storage.smbj'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.jcifsng', 'package com.samsung.cifs.storage.jcifsng'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.manager', 'package com.samsung.cifs.storage.manager'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.domain', 'package com.samsung.cifs.domain'
    $content = $content -replace 'package com\.wa2c\.android\.cifsdocumentsprovider\.presentation', 'package com.samsung.cifs.ui'
    
    # Fix all import statements
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.common', 'import com.samsung.cifs.common'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.db', 'import com.samsung.cifs.data.db'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.preference', 'import com.samsung.cifs.data.preference'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.interfaces', 'import com.samsung.cifs.storage'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.smbj', 'import com.samsung.cifs.storage.smbj'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.jcifsng', 'import com.samsung.cifs.storage.jcifsng'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.data\.storage\.manager', 'import com.samsung.cifs.storage.manager'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.domain', 'import com.samsung.cifs.domain'
    $content = $content -replace 'import com\.wa2c\.android\.cifsdocumentsprovider\.presentation', 'import com.samsung.cifs.ui'
    
    # Fix any remaining references in strings, comments, or class names
    $content = $content -replace 'com\.wa2c\.android\.cifsdocumentsprovider', 'com.samsung.cifs'
    
    # Fix specific common patterns
    $content = $content -replace 'cifsdocumentsprovider', 'cifs'
    $content = $content -replace 'CifsDocumentsProvider', 'CifsProvider'
    
    # Only write if content changed
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "  -> Updated package references"
    } else {
        Write-Host "  -> No changes needed"
    }
}

Write-Host "Package reference fixing completed!"

# Also fix any XML files (AndroidManifest.xml, etc.)
Write-Host "Fixing XML files..."
$xmlFiles = Get-ChildItem -Path "." -Filter "*.xml" -Recurse

foreach ($file in $xmlFiles) {
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    $content = $content -replace 'com\.wa2c\.android\.cifsdocumentsprovider', 'com.samsung.cifs'
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "All package fixing completed!"
