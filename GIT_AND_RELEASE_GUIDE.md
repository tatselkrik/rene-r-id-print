# Rene'R ID Print: Git and Release Guide

## What Git Does

Git preserves named snapshots of the source code. It works locally and does not require an account or internet connection. The stable version lives on the `main` branch, and the finalized white-background release is marked with the tag `v1.0.1`.

Useful commands in Android Studio's Terminal:

```powershell
git status
git log --oneline --decorate
git tag
```

- `git status` shows files changed since the last saved snapshot.
- `git log --oneline --decorate` shows saved versions.
- `git tag` lists release markers such as `v1.0.0` and `v1.0.1`.

## Adding a Feature Later

Start new work on a separate branch so Version 1 remains safe:

```powershell
git switch -c feature/custom-preview-layouts
```

After the feature has been tested:

```powershell
git add -A
git commit -m "Add selectable preview layouts"
git switch main
git merge feature/custom-preview-layouts
```

For that feature release, set Android's `versionName` to `1.0.2`, increase `versionCode` to `3`, build the installer with the same signing key, and add the Git tag `v1.0.2`.

## Local Git Versus Backup

Local Git protects against accidental code changes, but it is still stored on this computer. A private GitHub repository or an external-drive copy can be added later for protection against drive failure. A GitHub sign-in is not required for Version 1.

## Creating the Signed Installer

Run this command in Android Studio's Terminal:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\create-signed-installer.ps1
```

On the first run, the helper asks you to create and confirm a signing password. The password is hidden while you type it. The helper then:

1. Creates the permanent signing key in `private-signing`.
2. Builds the release app.
3. Signs and verifies the installer.
4. Reads the current version from the Android project and saves it as `release\ReneR-ID-Print-v<version>.apk`.

The key and password are required for every future update. Back up the entire `private-signing` folder and record the password in a password manager or another secure location. Never send the key or password to anyone and never add them to Git.

The current Android Studio development copy uses a different signature. Uninstall that development copy before installing the first signed Version 1 APK. This clears the saved printer connection once. Later signed versions will update Version 1 normally when they use the same key and a higher `versionCode`.
