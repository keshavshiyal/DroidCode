# Guide: Setting Up a Permanent Signing Key in the Repository

This guide details the complete, step-by-step procedures for permanently configuring and maintaining Android signing keys in this repository.

Having a permanent signing key ensures:
- **Consistent Application Signature**: Prevents the `INSTALL_FAILED_UPDATE_INCOMPATIBLE` error when updating builds across devices, emulators, and testers.
- **Reproducible CI/CD Builds**: Guarantees that GitHub Actions or local workstations produce identical, verifiable binary signatures.
- **Security Compliance**: Keeps sensitive production passwords and keys safe while allowing automated signing.

---

## Architecture of Signing in this Project

In `app/build.gradle.kts`, the project defines two signing configurations:

1. **`debugConfig`**:
   - Location: `${rootDir}/debug.keystore`
   - Store Password: `android`
   - Key Alias: `androiddebugkey`
   - Key Password: `android`
   - Purpose: Used for local development and testing builds (`assembleDebug`).
   - Repository Strategy: Tracked reliably in Git via Base64 encoding (`debug.keystore.base64`).

2. **`release`**:
   - Location: Path set by `KEYSTORE_PATH` environment variable (defaults to `${rootDir}/my-upload-key.jks`).
   - Store Password: Set by `STORE_PASSWORD` environment variable.
   - Key Alias: `upload`
   - Key Password: Set by `KEY_PASSWORD` environment variable.
   - Purpose: Used for official release builds and Google Play / GitHub Releases.

---

## Part 1: Setting Up a Permanent Debug Keystore (In-Repo)

The repository uses the standard pattern of committing an encoded Base64 debug key (`debug.keystore.base64`). This prevents binary line-ending corruption across Windows, macOS, and Linux while keeping the key permanent across all git clones.

### Step 1: Generate a Debug Keystore
If you need to generate a new debug keystore from scratch:

```bash
keytool -genkey -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

### Step 2: Encode the Keystore to Base64
Convert the binary `.keystore` file into a clean single-line or ASCII Base64 file:

- **Linux / Ubuntu**:
  ```bash
  base64 -w 0 debug.keystore > debug.keystore.base64
  ```

- **macOS**:
  ```bash
  base64 -i debug.keystore -o debug.keystore.base64
  ```

- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("debug.keystore")) | Out-File -Encoding ascii debug.keystore.base64
  ```

### Step 3: Configure `.gitignore`
Ensure the raw binary `.keystore` is ignored so developers only commit the portable `.base64` file:

```gitignore
*.keystore
*.jks
!debug.keystore.base64
```

### Step 4: Add the Restoration Hook in CI / Build Script
In `.github/workflows/build.yml`, add the decoding step before compiling:

```yaml
- name: Setup Debug Keystore
  run: |
    if [ -f debug.keystore.base64 ] && [ ! -f debug.keystore ]; then
      echo "Decoding debug.keystore.base64..."
      base64 -d debug.keystore.base64 > debug.keystore
    fi
```

### Step 5: Commit to Git
```bash
git add debug.keystore.base64 .github/workflows/build.yml
git commit -m "chore: permanently configure repository debug signing key"
git push
```

Every developer or CI runner cloning the repository can now automatically restore and use the identical key.

---

## Part 2: Setting Up a Permanent Release Signing Key (CI/CD Best Practice)

For release signing (production distribution), **never commit raw release passwords or unencrypted private keys directly into public repositories**. Instead, store the keystore as an encrypted GitHub Repository Secret.

### Step 1: Generate the Release Keystore
Run the following command to generate a production-ready Java KeyStore (JKS):

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=DroidCode Release,O=DroidCode,C=US"
```
*(Enter a strong password when prompted. Remember this password for the next steps).*

### Step 2: Convert the Release Keystore to Base64
Encode the file to a text string:

- **Linux**:
  ```bash
  base64 -w 0 my-upload-key.jks > release-keystore-base64.txt
  ```
- **macOS**:
  ```bash
  base64 -i my-upload-key.jks | tr -d '\n' > release-keystore-base64.txt
  ```

### Step 3: Add GitHub Repository Secrets
Go to your repository on GitHub:
1. Navigate to **Settings** > **Secrets and variables** > **Actions**.
2. Click **New repository secret** and add the following 4 secrets:

| Secret Name | Value | Example |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | The contents of `release-keystore-base64.txt` | `MIIDv...` |
| `RELEASE_KEYSTORE_PASSWORD`| The password set during key generation | `StrongStorePass123` |
| `RELEASE_KEY_ALIAS` | Key alias name | `upload` |
| `RELEASE_KEY_PASSWORD` | Key password | `StrongKeyPass123` |

### Step 4: Configure GitHub Actions Release Workflow
Create or update `.github/workflows/release.yml`:

```yaml
name: Build Signed Release APK

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  release:
    name: Build & Sign Release APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Decode Release Keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }}
        run: |
          echo "$KEYSTORE_BASE64" | base64 -d > "${{ github.workspace }}/my-upload-key.jks"

      - name: Build Signed Release APK
        env:
          KEYSTORE_PATH: "${{ github.workspace }}/my-upload-key.jks"
          STORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
        run: |
          ./gradlew :app:assembleRelease --stacktrace

      - name: Upload Signed APK
        uses: actions/upload-artifact@v4
        with:
          name: DroidCode-Release-APK
          path: app/build/outputs/apk/release/*.apk
```

---

## Part 3: Local Permanent Release Signing (Optional)

If you are developing locally or in a private repository and want release signing without setting environment variables every time:

1. Place `my-upload-key.jks` in the root folder of the project.
2. In your local shell configuration (`~/.bashrc` or `~/.zshrc`), add:
   ```bash
   export KEYSTORE_PATH="$HOME/path/to/droidcode/my-upload-key.jks"
   export STORE_PASSWORD="your_store_password"
   export KEY_PASSWORD="your_key_password"
   ```
3. Run:
   ```bash
   gradle :app:assembleRelease
   ```

---

## Part 4: Verification & Troubleshooting

### 1. Inspect Keystore Certificates & Fingerprints
Verify alias and certificate fingerprints (SHA-1, SHA-256):
```bash
keytool -list -v -keystore debug.keystore -storepass android
```

### 2. Verify APK Signature
Verify that the output APK has been signed with v2/v3 signing schemes:
```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
```
Expected output:
```text
Verifies: true
Signer #1 certificate DN: CN=Android Debug, O=Android, C=US
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
Signer #1 SHA-256 digest: ...
```

### 3. Fixing `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
If you see:
```text
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.keshav.droidcode.app signatures do not match newer version]
```
This means an older APK on the device was signed with a different key.
- **Development Fix**: Uninstall the existing app first:
  ```bash
  adb uninstall com.keshav.droidcode.app
  ```
- **Permanent Solution**: By adopting the persistent `debug.keystore.base64` documented above, all builds will share the exact same signature and will update seamlessly without uninstalling.
