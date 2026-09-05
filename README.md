# ServerStats

پلاگین Paper (Java 21) که بدون هیچ پروسه‌ی جدا، بدون SSH کامل، و بدون نصب Node/Python
یه داشبورد وب زنده برای سرور ماینکرفتت می‌سازه. کل HTTP server داخل خود پلاگینه.

## چطور بسازیمش (build)

نیاز داری:
- JDK 21
- Maven (`mvn`)

```bash
cd ServerStats
mvn clean package
```

خروجی نهایی: `target/ServerStats.jar` — همینو بریز تو پوشه `plugins/` سرورت.

## راه‌اندازی

1. `ServerStats.jar` رو تو `plugins/` بذار، سرور رو ری‌استارت کن.
2. یه فایل `config.yml` تو `plugins/ServerStats/` ساخته میشه:
   ```yaml
   web-port: 8123
   bind-address: "0.0.0.0"
   server-name: "My Server"
   ```
3. تو پنل هاستت (Pterodactyl و مشابه) پورت `8123` رو باز/فوروارد کن — همون کاری
   که برای پورت خود ماینکرفت کردی.
4. برو به `http://IP-SERVER:8123` — داشبورد زنده رو می‌بینی.

## چیزی که الان جمع می‌کنه

- زمان بازی هر پلیر (playtime)
- Kill / Death و K/D
- بلاک‌های شکسته/گذاشته‌شده
- فید اخیر: join / leave / kill / death

هر ۵ ثانیه صفحه خودش رو رفرش می‌کنه (بدون رفرش دستی صفحه).

## ساختار کد

- `ServerStatsPlugin.java` — نقطه‌ی شروع، همه چیز رو وصل می‌کنه
- `StatsDatabase.java` — SQLite (فایل `stats.db` تو پوشه پلاگین)
- `StatsListener.java` — لیسنرهای Bukkit که دیتا رو جمع می‌کنن
- `WebServer.java` — HTTP server با `com.sun.net.httpserver` (تو JDK هست، dependency اضافه نمی‌خواد)
- `SimpleJson.java` — سریالایزر JSON مینیمال (برای اینکه Gson اضافه نشه به jar)
- `resources/web/index.html` — کل فرانت‌اند، یه فایل تنها، embed شده تو jar

## قدم بعدی‌های پیشنهادی

- یه چارت زمانی (playtime در طول روز) با Chart.js
- نقشه‌ی زنده بلوک‌ها شبیه BlueMap (این خودش یه پروژه‌ی جداست)
- Basic Auth یا رمز ساده روی داشبورد اگه public می‌کنیش
- endpoint برای historical data (نه فقط snapshot فعلی)
