[CmdletBinding()]
param(
    [switch]$UsePreparedApk,
    [switch]$InitializeAutomaticSigning
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
$keytool = Join-Path $androidStudioJbr "bin\keytool.exe"
$keystoreDirectory = Join-Path $projectRoot "private-signing\automatic-v1"
$keystorePath = Join-Path $keystoreDirectory "rene-r-id-print-release.jks"
$passwordPath = Join-Path $keystoreDirectory "password.dpapi"
$certificatePath = Join-Path $keystoreDirectory "certificate.sha256"
$keyAlias = "rene-r-id-print"
$passwordVariable = "RENER_SIGNING_PASSWORD"

if (-not (Test-Path -LiteralPath $keytool)) {
    throw "Android Studio's signing tool was not found at $keytool"
}

if ($UsePreparedApk) {
    $preparedApk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
    $preparedHashPath = Join-Path $projectRoot "release\prepared-unsigned.sha256"
    if (-not (Test-Path -LiteralPath $preparedHashPath)) {
        throw "The prepared APK verification record is missing. Ask Codex to prepare the release first."
    }
    $preparedHash = (Get-Content -LiteralPath $preparedHashPath -Raw).Trim()
    if ((Get-FileHash -LiteralPath $preparedApk -Algorithm SHA256).Hash -cne $preparedHash) {
        throw "The prepared APK changed after verification. Ask Codex to verify the build again."
    }
}
if ($InitializeAutomaticSigning) {
    if (Test-Path -LiteralPath $keystoreDirectory) {
        throw "Automatic signing already has local state. Refusing to replace or regenerate it."
    }
    New-Item -ItemType Directory -Path $keystoreDirectory | Out-Null
    $acl = New-Object System.Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $owner = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($owner)
    foreach ($sid in @($owner, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'))) {
        $acl.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new(
            $sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow'
        ))
    }
    Set-Acl -LiteralPath $keystoreDirectory -AclObject $acl
    $randomBytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($randomBytes) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($randomBytes)
    [Array]::Clear($randomBytes, 0, $randomBytes.Length)
    $securePassword = ConvertTo-SecureString -String $password -AsPlainText -Force
    # Without -Key, Windows encrypts this for the current Windows account using DPAPI.
    $securePassword | ConvertFrom-SecureString | Set-Content -LiteralPath $passwordPath -Encoding ASCII
} else {
    if (-not (Test-Path -LiteralPath $keystorePath) -or
        -not (Test-Path -LiteralPath $passwordPath) -or
        -not (Test-Path -LiteralPath $certificatePath)) {
        throw "Automatic signing files are missing. Restore them; do not generate a replacement identity."
    }
    try {
        $securePassword = (Get-Content -LiteralPath $passwordPath -Raw).Trim() | ConvertTo-SecureString
        $password = [System.Net.NetworkCredential]::new('', $securePassword).Password
    } catch {
        throw "Automatic signing could not unlock the key under this Windows account. No key was changed."
    }
}

try {
    Set-Item -Path "Env:$passwordVariable" -Value $password
    $env:JAVA_HOME = $androidStudioJbr
    if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
        $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
    }

    if ($InitializeAutomaticSigning) {
        & $keytool -genkeypair -v `
            -keystore $keystorePath `
            -alias $keyAlias `
            -storetype JKS `
            -keyalg RSA `
            -keysize 4096 `
            -validity 10000 `
            -dname "CN=Rene'R ID Print" `
            "-storepass:env" $passwordVariable `
            "-keypass:env" $passwordVariable
        if ($LASTEXITCODE -ne 0) {
            throw "The permanent signing key could not be created."
        }
        $publicCertificate = Join-Path $keystoreDirectory "certificate.der"
        & $keytool -exportcert -keystore $keystorePath -alias $keyAlias `
            '-storepass:env' $passwordVariable -file $publicCertificate
        if ($LASTEXITCODE -ne 0) { throw "The signing certificate could not be exported." }
        (Get-FileHash -LiteralPath $publicCertificate -Algorithm SHA256).Hash.ToLowerInvariant() |
            Set-Content -LiteralPath $certificatePath -Encoding ASCII
    }

    if (-not $UsePreparedApk) {
        Push-Location $projectRoot
        try {
            & ".\gradlew.bat" assembleRelease
            if ($LASTEXITCODE -ne 0) {
                throw "The release build failed."
            }
        } finally {
            Pop-Location
        }
    }

    $sdkLine = Select-String -Path (Join-Path $projectRoot "local.properties") `
        -Pattern "^sdk.dir=" | Select-Object -First 1
    if ($null -eq $sdkLine) {
        throw "The Android SDK location is missing from local.properties."
    }
    $sdkDirectory = $sdkLine.Line.Substring("sdk.dir=".Length) `
        -replace "\\:", ":" `
        -replace "\\\\", "\"
    $buildToolsDirectory = Get-ChildItem (Join-Path $sdkDirectory "build-tools") -Directory |
        Where-Object {
            (Test-Path (Join-Path $_.FullName "zipalign.exe")) -and
            (Test-Path (Join-Path $_.FullName "apksigner.bat"))
        } |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if ($null -eq $buildToolsDirectory) {
        throw "Android SDK Build-Tools with zipalign and apksigner were not found."
    }

    $versionMatch = Select-String `
        -Path (Join-Path $projectRoot "app\build.gradle.kts") `
        -Pattern 'versionName\s*=\s*"([^"]+)"' |
        Select-Object -First 1
    if ($null -eq $versionMatch) {
        throw "The Android version name could not be read."
    }
    $versionName = $versionMatch.Matches[0].Groups[1].Value
    $unsignedApk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release-unsigned.apk"
    $badging = & (Join-Path $buildToolsDirectory.FullName "aapt.exe") dump badging $unsignedApk
    if ($LASTEXITCODE -ne 0 -or
        -not ($badging | Select-String -SimpleMatch "name='com.idphoto.printing'") -or
        -not ($badging | Select-String -SimpleMatch "versionName='$versionName'") -or
        ($badging | Select-String -SimpleMatch 'application-debuggable')) {
        throw "The APK package, release version, or non-debuggable status did not match."
    }
    $releaseDirectory = Join-Path $projectRoot "release"
    $alignedApk = Join-Path $releaseDirectory "ReneR-ID-Print-v$versionName-aligned-unsigned.apk"
    $signedApk = Join-Path $releaseDirectory "ReneR-ID-Print-v$versionName.apk"
    New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null

    & (Join-Path $buildToolsDirectory.FullName "zipalign.exe") `
        -f -p 4 $unsignedApk $alignedApk
    if ($LASTEXITCODE -ne 0) {
        throw "The installer could not be aligned."
    }

    & (Join-Path $buildToolsDirectory.FullName "apksigner.bat") sign `
        --ks $keystorePath `
        --ks-key-alias $keyAlias `
        --ks-pass "env:$passwordVariable" `
        --key-pass "env:$passwordVariable" `
        --min-sdk-version 23 `
        --v1-signing-enabled true `
        --v2-signing-enabled true `
        --v3-signing-enabled true `
        --out $signedApk `
        $alignedApk
    if ($LASTEXITCODE -ne 0) {
        throw "The saved automatic signing credential could not sign the installer. Preserve the key and restore its matching credential."
    }

    $verification = & (Join-Path $buildToolsDirectory.FullName "apksigner.bat") verify `
        --verbose --print-certs $signedApk
    if ($LASTEXITCODE -ne 0) {
        throw "The signed installer did not pass verification."
    }
    $currentCertificate = @($verification |
        Select-String 'certificate SHA-256 digest: ([0-9a-fA-F]{64})$' |
        ForEach-Object { $_.Matches[0].Groups[1].Value.ToLowerInvariant() } |
        Sort-Object -Unique)
    $expectedCertificate = (Get-Content -LiteralPath $certificatePath -Raw).Trim()
    if ($currentCertificate.Count -ne 1 -or $currentCertificate[0] -cne $expectedCertificate) {
        throw "The APK does not match the permanent automatic signing identity."
    }
    $verification | Select-String '^Verifies|^Verified using|^Number of signers'

    $previousApk = Get-ChildItem -LiteralPath $releaseDirectory -Filter 'ReneR-ID-Print-v*.apk' |
        Where-Object {
            $_.BaseName -match '^ReneR-ID-Print-v(\d+\.\d+\.\d+)$' -and
            [version]$Matches[1] -lt [version]$versionName
        } |
        Sort-Object { [version]($_.BaseName -replace '^ReneR-ID-Print-v', '') } -Descending |
        Select-Object -First 1
    # v1.0.3 is the explicitly approved signing migration. Later versions must
    # retain the automatic identity and update it without an uninstall.
    if ($null -ne $previousApk -and
        [version]($previousApk.BaseName -replace '^ReneR-ID-Print-v', '') -ge [version]'1.0.3') {
        $signer = Join-Path $buildToolsDirectory.FullName "apksigner.bat"
        $previousCertificate = @(& $signer verify --print-certs $previousApk.FullName |
            Select-String 'certificate SHA-256 digest: ([0-9a-fA-F]{64})$' |
            ForEach-Object { $_.Matches[0].Groups[1].Value.ToLowerInvariant() } |
            Sort-Object -Unique)
        if ($LASTEXITCODE -ne 0 -or $previousCertificate.Count -ne 1) {
            throw "The previous APK signature could not be verified."
        }
        $currentCertificate = @(& $signer verify --print-certs $signedApk |
            Select-String 'certificate SHA-256 digest: ([0-9a-fA-F]{64})$' |
            ForEach-Object { $_.Matches[0].Groups[1].Value.ToLowerInvariant() } |
            Sort-Object -Unique)
        if ($LASTEXITCODE -ne 0 -or $currentCertificate.Count -ne 1 -or
            $previousCertificate[0] -cne $currentCertificate[0]) {
            throw "The new APK does not have the previous release's signing identity. Do not install it."
        }
    }

    Remove-Item -LiteralPath $alignedApk -Force
    Write-Host ""
    Write-Host "Version $versionName installer created successfully:" -ForegroundColor Green
    Write-Host $signedApk -ForegroundColor Green
    Write-Host ""
    Write-Host "Signed automatically with the saved Windows-protected release key."
} finally {
    Remove-Item -Path "Env:$passwordVariable" -ErrorAction SilentlyContinue
    $password = $null
    if ($null -ne $securePassword) { $securePassword.Dispose() }
}
