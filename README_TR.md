# RockJazz MetroTuner — Android APK

Bu dal, RockJazz MetroTuner V3.8'in Android WebView paketidir. HTML sürümü ayrı olarak `RockJazzMetronome-WebTest` deposunda tutulur; Android uygulaması ise bu HTML'yi APK içine gömülü olarak çalıştırır.

## Android özellikleri
- Uygulama adı: RockJazz MetroTuner
- Sürüm: 3.8.0
- Paket: `com.rockjazz.metrotuner`
- Minimum Android: 6.0 (API 23)
- WebViewAssetLoader ile güvenli `https://appassets.androidplatform.net/assets/index.html` origin'i
- Firebase Authentication ve Cloud Firestore üzerinden merkezi kullanıcı/Pro lisansı
- Telefon mikrofonu için Android RECORD_AUDIO izni ve WebView mikrofon izni
- Metronom açıkken Android foreground media-playback servisi ve partial wake lock
- Ekran kapalı/kilitliyken native metronom tıklamaları
- Bildirimden metronomu durdurma

## Firebase Android kurulumu
Firebase Console → Authentication → Settings → Authorized domains bölümüne şu alan adını ekleyin:

`appassets.androidplatform.net`

E-posta/şifre sağlayıcısı etkin olmalıdır. Firestore Rules, V3.8 lisans işlemlerini kapsayan kurallarla yayımlanmalıdır. HTML/Android istemci yapılandırmasında Firebase web config kullanılır; yönetici yetkisi Firestore güvenlik kurallarıyla korunur.

## APK oluşturma
GitHub Actions, `metrotuner-v3-android` dalına her push geldiğinde otomatik APK oluşturur:

1. Repository → Actions
2. En son **Build RockJazz MetroTuner Android APK** çalışmasını açın.
3. Artifacts bölümünden `RockJazz-MetroTuner-APK` dosyasını indirin.
4. ZIP içinden `app-debug.apk` dosyasını Android telefona kurun.

Bu, test/debug APK'sıdır. Google Play veya son kullanıcıya ticari dağıtım için ayrıca release imzası ve imzalama anahtarı gerekir.

## Gerçek cihaz test sırası
1. Firebase Authorized Domains içine `appassets.androidplatform.net` ekleyin.
2. APK'yı kurup e-posta/şifre ile giriş yapın.
3. Hesap ekranında yönetici hesabının ADMIN • PRO görünmesini doğrulayın.
4. Akort ekranında mikrofon izni verip gerçek enstrümanla deneyin.
5. Metronomu başlatın, ekranı kilitleyin; bildirimde MetroTuner metronom servisini görüp sesin devam ettiğini kontrol edin.
6. Bildirimdeki Durdur düğmesinin metronomu kapattığını kontrol edin.
