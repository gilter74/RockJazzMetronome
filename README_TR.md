# RockJazz MetroTuner — Android

Bu Android uygulaması, RockJazz MetroTuner V3.8 web arayüzünü Android WebView içinde çalıştırır. Yeni uygulama kimliği `com.rockjazz.metrotuner` olduğundan eski `com.rockjazz.metronome` uygulamasıyla yan yana kurulabilir.

## Bu dalda
- V3.8 HTML, doğrudan Android assets içine alınır; web uygulamasının ayrı deposu değiştirilmez.
- WebViewAssetLoader, sayfayı HTTPS benzeri güvenli appassets origin'inde sunar.
- Firebase Authentication ve Cloud Firestore modüler Web SDK'sı kullanılabilir.
- Mikrofon erişimi için Android çalışma zamanı izni ve WebChromeClient izin köprüsü bulunur.
- Metronom başladığında Foreground Service ve PARTIAL_WAKE_LOCK açılır; durdurulduğunda kapatılır. Bildirimden de metronom durdurulabilir.
- Yeni uygulama adı ve gümüş/mavi uygulama simgesi.

## Firebase notu
Firebase Authentication > Settings > Authorized domains bölümüne gerekirse `appassets.androidplatform.net` alan adını ekleyin. E-posta/şifre girişi, Firestore ve mikrofonu gerçek cihazda test edin.

## APK üretimi
GitHub Actions, `metrotuner-v3-android` dalına yapılan her push sonrasında debug APK üretir:
Actions → Build RockJazz MetroTuner Android APK → son başarılı run → Artifacts → RockJazz-MetroTuner-APK.

## Bilinen test gereksinimi
Ekran kilitliyken metronomun kesintisiz sesi gerçek Android cihazında test edilmelidir. Foreground Service CPU'yu uyanık tutar ve WebView AudioContext'inin arka planda devam etmesini destekler; üretim sürümünden önce farklı Android sürümlerinde doğrulanmalıdır.
