# NOVA 2.0

Assistente personale Android nativa.

## Stato del progetto
- Applicazione principale: `com.novatwo.assistant`
- Android minimo: API 23
- Build: Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, Kotlin 2.0.21
- Provider AI attivo in questa configurazione: Gemini; NOVA chiede conferma prima di inviare richieste API.
- Chiavi API e memoria conversazioni/note sono conservate localmente e cifrate con Android Keystore.
- Voce italiana Paola: download opzionale di circa 21 MB, poi sintesi offline sul dispositivo.
- Il runtime vocale nativo ARM64 viene scaricato dal workflow durante la build e non è committato nel repository.

## Build
Il workflow `.github/workflows/android.yml` esegue la compilazione e pubblica un unico artefatto `NOVA-2.0-debug-APK`.

## Nota
La compilazione CI verifica che il progetto compili e che l'APK venga prodotto. Le prove di funzionamento su un dispositivo Android reale restano necessarie per verificare microfono, provider API e riproduzione vocale.
