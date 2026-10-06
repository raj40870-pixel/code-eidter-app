# Mandatory Standard Project Rule: End-to-End Release Pipeline 🚀

Whenever any new version, APK build, or release is created in this project, the agent **MUST** execute the complete end-to-end release pipeline across all 3 ecosystem repositories automatically, without requiring individual user reminders.

---

## Complete Release Protocol Checklist

### 1. Android App Repository (`code-eidter-app` / `termcode-ide`)
- [ ] Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
- [ ] Update `README.md` with the new version badges, direct APK links, and "What's New" release notes.
- [ ] Compile and sign production APK (`gradlew.bat assembleRelease`).
- [ ] Verify `app/build/outputs/apk/release/app-release.apk` is generated.
- [ ] Commit all changes to `main`.
- [ ] Create annotated git tag `vX.X.X`: `git tag -a vX.X.X -m "Release vX.X.X: <summary>"`.
- [ ] Push commit and tag to GitHub: `git push origin main` & `git push origin vX.X.X`.

### 2. Official Website & OTA Update Portal (`Code-eidter-apk-website`)
- [ ] Copy the newly built release APK to:
  - `C:\Users\Kamaljit\OneDrive\Desktop\code-editor-website\apk\CodeEditor-vX.X.X.apk`
  - `C:\Users\Kamaljit\OneDrive\Desktop\code-editor-website\apk\CodeEditor-latest.apk`
  - `C:\Users\Kamaljit\OneDrive\Desktop\CodeEditor-vX.X.X.apk`
- [ ] Compute real SHA-256 hash and file size (`Get-FileHash`).
- [ ] Update `version.json` with the new `versionCode`, `versionName`, `releaseNotes`, and published date.
- [ ] Update `index.html`:
  - Navbar download link
  - Hero badge pill, description, and download button
  - Specs build size pill
  - Download portal card (file name, size, version label, download button, SHA-256 checksum)
  - Footer version label
- [ ] Update website `README.md`.
- [ ] Commit all changes to `main`.
- [ ] Create annotated git tag `vX.X.X`: `git tag -a vX.X.X -m "CodeEditor IDE Website & Portal vX.X.X Release"`.
- [ ] Push commit and tag to GitHub: `git push origin main` & `git push origin vX.X.X` (triggers automated Vercel deployment).

### 3. Compiler & Toolchain Library Repository (`library` / `DevLocal\library`)
- [ ] Update `C:\Users\Kamaljit\DevLocal\library\README.md` (compatibility badge `vX.X.X_Compatible` and ecosystem link `Official Android App (vX.X.X)`).
- [ ] Synchronize `C:\Users\Kamaljit\Downloads\library_server_backup\README.md`.
- [ ] Commit `README.md` changes.
- [ ] Create annotated git tag `vX.X.X` for GitHub Releases: `git tag -a vX.X.X -m "CodeEditor IDE Compiler & Toolchain Suite vX.X.X Compatible"`.
- [ ] Push commit and tag to GitHub: `git push origin main` & `git push origin vX.X.X` so GitHub Releases is immediately updated.
