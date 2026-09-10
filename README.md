# تسبیحات حضرت زهرا

پروژه Android آفلاین و بدون dependency خارجی، با Java و یک Custom View برای کنترل دقیق UI/Transition/Audio.

## تعارض Specification
سند اندازه Artwork را 1920×1080 گفته، در حالی که خود اپ باید Portrait باشد. پیاده‌سازی اندازه منابع را 1920×1080 نگه می‌دارد و در صفحه Portrait با CENTER_CROP تصویر را تا حد ممکن بزرگ می‌کند؛ در نتیجه ممکن است بخشی از کناره‌های تصویر crop شود. این تنها تعارض اصلی مشاهده‌شده در Specification است.

## Build
- Android Gradle Plugin: 9.3.0
- Gradle: 9.5.0
- compileSdk/targetSdk: 36
- buildToolsVersion: 36.0.0
- minSdk: 23
- JDK: 17

فایل wrapper باینری Gradle (`gradle-wrapper.jar`) در محیط فعلی قابل دریافت نبود؛ بنابراین `gradlew` به Gradle نصب‌شده سیستم متصل شده است. در Android Studio می‌توانید پروژه را باز و Sync کنید و Gradle 9.5 را انتخاب کنید.

## منابع
همه تصاویر قابل تعویض در:
`app/src/main/res/drawable-nodpi/`

نام‌ها:
`section1_01` تا `section1_34`
`section2_01` تا `section2_33`
`section3_01` تا `section3_33`
`intro`
`final_image`

صداها در:
`app/src/main/res/raw/`
`audio1.wav`، `audio2.wav`، `audio3.wav`، `audio4.wav`

برای نسخه واقعی فقط فایل‌های تصویر و صدا را با همان نام جایگزین کنید و منطق برنامه را تغییر ندهید.

## رفتار پیاده‌سازی‌شده
- شروع با تصویر Intro و انتقال بعد از 2 ثانیه.
- 34 تصویر الله اکبر، 33 تصویر الحمدلله، 33 تصویر سبحان الله.
- لمس برای رفتن به تصویر بعدی.
- Slide افقی نرم.
- شمارنده و نقاط پیشرفت پویا.
- audio4 مستقل و Loop؛ بین مراحل قطع یا از ابتدا پخش نمی‌شود.
- audio1/2/3 با هر تصویر از ابتدا اجرا می‌شوند.
- کنترل مستقل موسیقی و ذکر و ذخیره وضعیت با SharedPreferences.
- شروع مجدد از هر بخش به تصویر اول.
- صفحه نهایی بدون UI پیمایش و با بازخورد و شروع مجدد.
- بازخورد با Intent ایمیل و بدون سرور.
