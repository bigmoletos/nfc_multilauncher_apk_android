# NFC Multi-App Launcher — Instructions de build

## Prérequis

| Outil | Version minimale |
|-------|-----------------|
| Android Studio | Hedgehog 2023.1.1+ ou Jellyfish 2023.3.1+ |
| JDK | 17+ |
| Android SDK | API 34 |
| Gradle | 8.7 (téléchargé automatiquement) |

## Ouvrir dans Android Studio

1. Lancez Android Studio
2. **File → Open** → sélectionnez le dossier `NfcMultiLauncher/`
3. Attendez la synchronisation Gradle (première fois : ~3 min)
4. Connectez un appareil Android (NFC requis) ou utilisez un émulateur API 26+

## Build APK debug

```
Build → Build Bundle(s)/APK(s) → Build APK(s)
```

Ou en ligne de commande :
```bash
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Build APK release (signé)

```bash
./gradlew assembleRelease
# APK : app/build/outputs/apk/release/app-release-unsigned.apk
```

Pour signer : **Build → Generate Signed Bundle / APK**

## Installer sur l'appareil

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Tester le NFC

1. Créez une tâche avec 3 applications et des délais
2. Appuyez sur "Programmer NFC"
3. Approchez un tag NTAG213/215/216 vierge
4. Une fois programmé, approchez le tag → l'app démarre les actions en séquence

## Structure des packages

```
com.nfc.multilauncher
├── data/model/     → Task, Action, ActionType
├── data/local/     → Room DB, DAO, Repository
├── data/nfc/       → NfcReader, NfcWriter, NfcTagData
├── domain/         → TaskExecutor, ActionExecutor, DelayExecutor
├── ui/             → Activities, Adapters, Dialog, ViewModel
└── service/        → TaskExecutionService (foreground)
```

## Format JSON stocké sur le tag NFC

```json
{
  "taskId": "uuid",
  "taskName": "3 Apps + Maps",
  "actions": [
    {"id":"...","type":"LAUNCH_APP","packageName":"com.whatsapp","order":0},
    {"id":"...","type":"DELAY","delayMs":3000,"order":1},
    {"id":"...","type":"LAUNCH_APP","packageName":"org.telegram.messenger","order":2},
    {"id":"...","type":"DELAY","delayMs":3000,"order":3},
    {"id":"...","type":"LAUNCH_URL","url":"https://maps.google.com/?q=...","order":4}
  ]
}
```

## Taille JSON indicative

- 3 apps + 2 délais : ~400 octets
- 5 apps + 4 délais + URL : ~650 octets
- Maximum NTAG213 : 137 octets → utiliser NTAG215 (504 octets) ou NTAG216 (888 octets)

## Permissions demandées

| Permission | Raison |
|-----------|--------|
| `NFC` | Lire/écrire les tags |
| `QUERY_ALL_PACKAGES` | Trouver et lancer des apps par package name |
| `FOREGROUND_SERVICE` | Exécuter les actions en arrière-plan |
| `POST_NOTIFICATIONS` | Afficher la progression |
| `VIBRATE` | Feedback haptic NFC détecté |
