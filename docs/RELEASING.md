# Publishing a new version (for the developer)

The downloadable app is the **share** build: the release build without SMS reading, with its own app ID
(`com.ruvo.app.share`) so it installs next to the full app. Your own full app (`installDebug`) is unchanged.

## One-time setup (already done on DESKTOP-73AVRJU)

The signing key lives outside the repo:

- `C:\Users\bhara\.ruvo\ruvo-release.jks` — the key itself
- `C:\Users\bhara\.ruvo\keystore.properties` — its location and passwords

**Back both files up** (a USB drive or a private cloud folder, never GitHub). Every future version must be signed
with the same key, or phones that have Ruvo will refuse the update. If the key is lost, testers must uninstall the
old app before installing a new one.

The build stops if `RUVO_CLAUDE_API_KEY` is filled in `local.properties`, so a private key can never end up inside
a shared app.

## Each new version

1. Raise the version in `app/build.gradle.kts`: `versionCode` by 1 (2, 3, …) and `versionName` ("1.1", "1.2").
2. Build and test:
   ```powershell
   .\gradlew.bat test
   .\gradlew.bat :app:assembleShare
   ```
   The file is `app\build\outputs\apk\share\app-share.apk`. Rename the copy you upload to **Ruvo.apk** — the
   README's download link always fetches the file called Ruvo.apk from the newest release.
3. On GitHub, open the repo → **Releases** (right side) → **Draft a new release**.
4. **Choose a tag** → type `v1.1` (the new version) → **Create new tag**.
5. **Release title**: `Ruvo 1.1`. In the description, list what changed in a few plain lines.
6. Drag **Ruvo.apk** into the box "Attach binaries by dropping them here". Wait for the upload to finish.
7. Leave **Set as the latest release** ticked and press **Publish release**.

The README link and the QR code now give everyone the new version. Old releases can stay (they are history) or be
deleted from the Releases page with **Delete**.

## Checking a release

On a phone, open https://github.com/kumkum-bharambe/RUVO/releases/latest/download/Ruvo.apk — it should download the
new file. Install it over the old version: the app keeps its data and shows the new version in Settings.
