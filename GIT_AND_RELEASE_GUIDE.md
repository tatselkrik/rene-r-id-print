# Rene'R ID Print: Git and Release Guide

## What Git Does

Git preserves named snapshots of the source code. It works locally and does not require an account or internet connection. The stable version lives on the `main` branch, and the approved current release is `v1.0.4`. Earlier version tags remain preserved.

Useful commands in Android Studio's Terminal:

```powershell
git status
git log --oneline --decorate
git tag
```

- `git status` shows files changed since the last saved snapshot.
- `git log --oneline --decorate` shows saved versions.
- `git tag` lists release markers such as `v1.0.0`, `v1.0.1`, and `v1.0.2`.

## Adding a Feature Later

Start new work on a separate branch so the stable release remains safe. For example:

```powershell
git switch -c codex/short-feature-name
```

After the feature has been tested:

```powershell
git add -A
git commit -m "Add selectable preview layouts"
git switch main
git merge codex/short-feature-name
```

For the next release, choose the new `versionName`, increase `versionCode` above `5`, build the installer with the same signing key, and add a matching Git tag only after testing passes.

## Local Git Versus Backup

Local Git protects against accidental code changes, but it is still stored on this computer. A private GitHub repository or an external-drive copy can be added later for protection against drive failure. A GitHub sign-in is not required for Version 1.

## Creating the Signed Installer

From v1.0.3, signing is automatic on the configured Windows account. Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\create-signed-installer.ps1
```

To sign the already verified release build without rebuilding:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\create-signed-installer.ps1 -UsePreparedApk
```

The helper reads the generated password from Windows DPAPI-protected local storage, signs with the permanent automatic key, and verifies the APK's signature and identity. No password prompt is required. It checks the APK version, package, and non-debuggable status. Prepared mode also checks the recorded unsigned APK hash.

The private key and encrypted credential are under the ignored `private-signing/automatic-v1` directory. Its permissions restrict access to the Windows owner and SYSTEM. Never commit these files or include them in a release. The encrypted credential is bound to this Windows account and installation; copying the folder alone to a different PC is not a portable recovery method. Preserve a secure system backup and deliberately migrate signing before replacing Windows or the account.

Missing or inaccessible signing files cause a failure, never a silent key replacement. `-InitializeAutomaticSigning` is only for an explicitly authorized first setup and refuses to overwrite existing signing state. Normal releases must not use it.

### Signing identity change in v1.0.3

Kirk explicitly authorized a new signing identity because the old password was unavailable. The original key and v1.0.0–v1.0.2 APKs remain preserved. Version 1.0.3 requires uninstalling the old signed app before installation; this clears its private settings and cache. Save any photos that need to be kept before uninstalling. The new app will discover and pair with the printer again.

Later releases must keep the v1.0.3 automatic key, allowing normal updates without uninstalling. Keep the application identifier `com.idphoto.printing` unchanged. The helper verifies the pinned automatic certificate and compares it with previous releases from v1.0.3 onward.

Phone approval is still required before committing, tagging, pushing, or uploading a release APK.
