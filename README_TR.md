# RockJazz Metronome – Android projesi

Bu proje, sağlanan RockJazz Profesyonel Metronom V2.5 HTML dosyasını internet gerektirmeden çalışan bir Android WebView uygulaması olarak paketler.

## APK oluşturma (Android Studio)
1. Android Studio'yu bilgisayarınıza kurun.
2. Bu ZIP dosyasını bir klasöre çıkarın.
3. Android Studio'da **Open** ile `RockJazzMetronome` klasörünü seçin.
4. Gradle eşitlemesi tamamlanınca **Build > Build Bundle(s) / APK(s) > Build APK(s)** seçin.
5. APK şu klasörde oluşur: `app/build/outputs/apk/debug/app-debug.apk`
6. APK'yı Android telefona gönderip kurun. Telefonda gerekirse bu kaynaktan uygulama yükleme izni verin.

## Uygulama bilgileri
- Paket: `com.rockjazz.metronome`
- Sürüm: `2.5.0`
- Minimum Android: 6.0 (API 23)
- İnternet izni: yok; HTML uygulama içine gömülüdür.
- Kayıtlı tempolar: Android WebView yerel depolamasında saklanır.
- Ekran: tam ekran; metronom açıkken ekranın uykuya geçmesi engellenir.

Not: Bu teslim Android Studio kaynak projesidir. Bu çalışma ortamında Android SDK/Gradle kurulu olmadığı için imzalanmış veya kurulabilir APK derlenemedi. Android Studio ile yukarıdaki adımlarda APK üretilebilir.
