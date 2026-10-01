# TraductionFlottante

## Prérequis
- JDK 17+  
- Android SDK (API 33)  
- `adb` dans le PATH  

## Credentials
1. Créez un compte de service Google Cloud pour le Translate API.  
2. Téléchargez le fichier JSON (ex. `heart-translate.json`).  
3. Placez‑le dans `~/keys/heart-translate.json` (hors repo).  
4. Ajoutez le chemin dans `app/local.properties`:
   ```
   GOOGLE_APPLICATION_CREDENTIALS=~/keys/heart-translate.json
   ```

## Build & Run
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions
- Accédez à *Paramètres → Accessibilité → TraductionFlottante*, cochez :  
  en haut _« Autoriser en tant qu’assistant »_.  
- Autorisez l’overlay via *Paramètres → Applications → Autres → Dessin d’applications*.

## Tests
```bash
./gradlew test            # unit tests
./gradlew connectedAndroidTest   # instrumentation
```

## Débogage
- `adb logcat | grep TraductionFlottante`  
- Vérifiez les logs `TranslationUtils` pour les erreurs réseau/quota.

## FAQ
- **Pourquoi l’overlay disparaît‑t‑il après l’envoi ?**  
  Le code `hideOverlay()` est appelé juste après `TextInjector.injectText(...)`.  

## GitHub Actions CI

This project includes a GitHub Actions workflow for continuous integration.

### Setting up Google Cloud Credentials for CI

To enable the CI workflow to access the Google Cloud Translation API:

1. Encode your service account JSON key file in base64:
   ```bash
   base64 -i ~/keys/heart-translate.json
   ```
   Copy the entire output.

2. In your GitHub repository, go to Settings → Secrets and variables → Actions → New repository secret
   - Name: `GOOGLE_APPLICATION_CREDENTIALS_BASE64`
   - Value: [paste the base64 encoded string from step 1]

### Workflow Triggers

The CI workflow runs on:
- Push to `main` branch
- Push to any branch matching `feature/**`
- Pull requests targeting `main` branch

### Workflow Steps

The CI workflow performs the following:
1. Checks out the code
2. Sets up JDK 17
3. Sets up Android SDK (API 30+)
4. Decodes the Google Cloud credentials secret and places it in the expected location
5. Runs unit tests (`./gradlew test`)
6. Runs instrumentation tests on an Android emulator
7. Assembles the debug APK (`./gradlew assembleDebug`)
8. Uploads the APK, test results, and JaCoCo coverage report as artifacts

### Accessing Workflow Results

- **APK**: Available as an artifact named `app-debug-apk` in each workflow run
- **Test reports**: Available as artifacts named `test-results` and `jacoco-report`
- **Workflow logs**: Available in the Actions tab of your repository

# Release v1.0.0
