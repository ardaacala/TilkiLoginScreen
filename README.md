# TilkiLoginScreen

Spigot 1.21.4 / Java 21 için TilkiCraft logolu AuthMe giriş ekranı.

## Kurulum

1. Sunucuyu durdur.
2. `dependencies/AuthMe-6.0.1-Spigot-1.21.jar` ve `dist/TilkiLoginScreen-1.0.0.jar` dosyalarını sunucunun `plugins` klasörüne kopyala. AuthMe zaten varsa ikinci bir kopya ekleme.
3. Sunucuyu başlat. Kaynak paketi URL ve SHA1 bilgileri JAR içinde hazırdır; ZIP'i sunucu klasörüne atman gerekmez.
4. Oyuncu kaynak paketini kabul eder; paket yüklendikten sonra siyah zemin üzerinde logo görünür. Sohbetten `/register parola parola` veya `/login parola` kullanılır. Komutların kesin biçimini AuthMe mesajları belirler.
5. AuthMe girişin başarılı olduğunu bildirdiğinde logo en geç yaklaşık 250 ms içinde kaldırılır. Hatalı şifre ekranı kapatmaz. Oturum açma zorunluluğu ve korumalar AuthMe tarafından yönetilir.

Resmi AuthMe indirme: https://github.com/AuthMe/AuthMeReloaded/releases/tag/6.0.1

## Davranış ve sınırlar

- Yeni kullanıcı henüz kaynak paketini indirmeden sunucu özel logoyu çizemez. İlk bağlantıda indirme/onay ekranı görünür. `pack-required: true` paketi zorunlu kılar.
- Bu bir istemci modu değildir: sohbet, Esc menüsü ve istemcinin kendi arayüzleri kullanılabilir. Logo dünya görüntüsünü kapatan bir title katmanıdır.
- AuthMe zorunlu bağımlılıktır. Plugin parola saklamaz, giriş komutlarını başarı kabul etmez. AuthMe API hatasında görsel katman kaldırılır; AuthMe'nin korumaları yönetilmez.
- CombatGuns paketinin yanına farklı UUID ile ek kaynak paketi gönderilir; mevcut silah model dosyalarına dokunulmaz. CombatGuns'un 30 tick paket isteğinin ardından bu paket varsayılan 60 tick'te gönderilir. Başka bir plugin sonradan tüm paketleri değiştirirse yeniden entegrasyon gerekir.
- Font namespace ve karakterleri ayrıdır. Title kanalı başka eklentilerle ortaktır; giriş öncesinde diğer title/HUD gösterimlerini kapatın. ViaVersion bağlantısı tek başına eski istemcilere 1.21.4 paket uyumluluğu sağlamaz; eski istemciler için ayrı paket gerekir.
- Envanter, konum, oyun modu veya körlük etkisi değiştirilmez. Giriş öncesi hareket/etkileşim engellerini AuthMe ayarlar.
- Paket SHA1'i değişirse `plugins/TilkiLoginScreen/config.yml` içindeki URL/SHA1 de güncellenmeli ve sunucu yeniden başlatılmalıdır.

## Derleme

`mvn package` (Java 21). Paket oluşturma: `java tools/BuildPack.java yol/tilkicraft_logo.png`.
`tools/BuildPack.java` orijinal logoyu oranını koruyarak 512x512 PNG'ye yerleştirir; tasarımını değiştirmez.

## Doğrulama

JAR derlemesi, resmi AuthMe 6.0.1 Spigot JAR'ındaki API imzaları, ZIP/PNG/font dosyaları ve yayımlanan ZIP'in SHA1'i kontrol edildi. Canlı Minecraft istemcisinde ekran yerleşimi ve sunucudaki diğer pluginlerle birlikte davranış henüz denenmedi.

Sunucu kontrolü: ilk kayıt, yanlış şifre, başarılı giriş, çıkış/giriş, AuthMe oturumla otomatik giriş, kaynak paketi reddetme, farklı GUI ölçekleri ve giriş sonrasında CombatGuns crosshair/scope/model görüntüsü.
