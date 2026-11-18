# Gece Postası (Android)

Native Android sürümü. Kotlin + Jetpack Compose ile yazıldı, WebView kullanılmıyor.
Paket adı: `com.okuyanstudio.gecepostasi`, minSdk 26, targetSdk 35.

## Açmak ve derlemek
1. Android Studio'da **File → Open** ile bu klasörü aç (Gradle 8.14 wrapper içinde).
2. Telefonda denemek için **Run ▶** kullan ya da `./gradlew assembleDebug` çalıştır.
3. Play Store için: **Build → Generate Signed App Bundle** ile kendi upload anahtarını seç; `app-release.aab` oluşur.

## Dosyalar
- `assets/i18n.json`: Türkçe, English, العربية, Kurdî metinleri (web sürümüyle aynı).
- `Data.kt`, `Game.kt`: hikâye, kurallar, mektup üretimi.
- `AppState.kt`: oyun durumu. `Table.kt`: sürükleme, damga ve uçma animasyonları.
- `Screen.kt`, `Letters.kt`, `Sheets.kt`: arayüz.
- `Audio.kt`: ses efektleri ve üretken müzik (AudioTrack ile canlı sentez, ses dosyası yok), titreşim.

## Testler
- `./gradlew recordPaparazziDebug`: tüm ekranların ekran görüntülerini `app/src/test/snapshots` klasörüne çıkarır.
- `./gradlew testDebugUnitTest --tests '*Logic*'`: 10 geceyi 4 dilde baştan sona 30 kez oynatır.
