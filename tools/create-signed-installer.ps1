[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
$keytool = Join-Path $androidStudioJbr "bin\keytool.exe"
$keystoreDirectory = Join-Path $projectRoot "private-signing"
$keystorePath = Join-Path $keystoreDirectory "rene-r-id-print-release.jks"
$keyAlias = "rene-r-id-print"
$passwordVariable = "RENER_SIGNING_PASSWORD"

function Read-PlainPassword([string]$Prompt) {
    $securePassword = Read-Host $Prompt -AsSecureString
    return [System.Net.NetworkCredential]::new("", $securePassword).Password
}

if (-not (Test-Path -LiteralPath $keytool)) {
    throw "Android Studio's signing tool was not found at $keytool"
}

$isNewKey = -not (Test-Path -LiteralPath $keystorePath)
$password = Read-PlainPassword $(
    if ($isNewKey) {
        "Create a permanent signing password (at least 8 characters)"
    } else {
        "Enter the permanent signing password"
    }
)

if ($password.Length -lt 8) {
    throw "Use a signing password with at least 8 characters."
}

if ($isNewKey) {
    $confirmation = Read-PlainPassword "Enter the same password again"
    if ($password -cne $confirmation) {
        throw "The two passwords did not match. No signing key was created."
    }
}

try {
    Set-Item -Path "Env:$passwordVariable" -Value $password
    $env:JAVA_HOME = $androidStudioJbr
    if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
        $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
    }

    if ($isNewKey) {
        New-Item -ItemType Directory -Force -Path $keystoreDirectory | Out-Null
        & $keytool -genkeypair -v `
            -keystore $keystorePath `
            -alias $keyAlias `
            -keyalg RSA `
            -keysize 4096 `
            -validity 10000 `
            -dname "CN=Rene'R ID Print" `
            "-storepass:env" $passwordVariable `
            "-keypass:env" $passwordVariable
        if ($LASTEXITCODE -ne 0) {
            throw "The permanent signing key could not be created."
        }
    }

    Push-Location $projectRoot
    try {
        & ".\gradlew.bat" assembleRelease
        if ($LASTEXITCODE -ne 0) {
            throw "The release build failed."
        }
    } finally {
        Pop-Location
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
        throw "The installer could not be signed. Check the password and try again."
    }

    & (Join-Path $buildToolsDirectory.FullName "apksigner.bat") verify `
        --verbose --print-certs $signedApk
    if ($LASTEXITCODE -ne 0) {
        throw "The signed installer did not pass verification."
    }

    Remove-Item -LiteralPath $alignedApk -Force
    Write-Host ""
    Write-Host "Version $versionName installer created successfully:" -ForegroundColor Green
    Write-Host $signedApk -ForegroundColor Green
    Write-Host ""
    Write-Warning "Back up private-signing and record the password securely. Both are required for every update."
} finally {
    Remove-Item -Path "Env:$passwordVariable" -ErrorAction SilentlyContinue
    $password = $null
}
