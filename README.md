# AniLiberty для Android

Нативное Android-приложение каталога аниме [aniliberty.top](https://aniliberty.top): каталог, поиск, расписание, избранное и полноценный плеер. Весь интерфейс — Jetpack Compose, **без WebView**.

**Текущая версия: 1.8.4** (versionCode 14). История изменений — в [CHANGELOG.md](CHANGELOG.md).

## Возможности

- **Каталог и поиск** — фильтры по жанрам, годам, типам и рейтингу; явный поиск показывает все совпадения, включая 18+
- **Главная** — рекомендации, последние обновления, новые серии
- **Расписание** — вид «плитки» или «список», напоминания о выходе серий (уведомление за 15 минут)
- **Избранное и коллекции** — новые добавления в начале, статусы (Смотрю / В планах / Просмотрено / Отложено / Брошено), отметки просмотра серий, история
- **Плеер** — горизонтальный режим, перемотка ±10 секунд двойным тапом, следующая/предыдущая серия, выбор качества (480/720/1080) и озвучки, пропуск опенинга, синхронизация прогресса
- **Аккаунт** — вход по паролю, одноразовому коду (OTP) или через ВКонтакте; токен шифруется Android Keystore, синхронизация тихая
- **Офлайн** — каталог сохраняется на устройство
- **Профиль** — статистика просмотров и часов, обложки профиля, редактор
- **Настройки** — светлая/тёмная тема, акценты, порядок вкладок перетаскиванием

## Технологии

Kotlin · Jetpack Compose (Material 3) · Media3/ExoPlayer (HLS) · Retrofit/OkHttp · Coil · WorkManager · Haze

minSdk 26 · targetSdk 35 · applicationId `top.aniliberty.app` · Gradle 8.11.1

## Сборка

### Windows
Запустите `build.bat` — скрипт сам скачает JDK 17 и Android SDK и соберёт `app/build/outputs/apk/debug/app-debug.apk`.

### Linux / macOS
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
Нужны JDK 17 и Android SDK (`platform-tools`, `platforms;android-35`, `build-tools;35.0.0`); путь к SDK указывается в `local.properties` (`sdk.dir=...`). Файл `local.properties` в репозиторий не входит.

## Структура проекта

```
app/src/main/java/top/aniliberty/app/  исходники: data/ — модели, API, репозиторий, настройки; ui/ — экраны Compose
app/src/test/java/top/aniliberty/app/  unit-тесты
signing/debug.keystore                 постоянный отладочный ключ для сборки
build.bat                              сборка Windows «с нуля»
```

## Подпись отладочной сборки

`signing/debug.keystore` (пароль `android`) — стандартный debug-ключ; он намеренно включён в репозиторий, чтобы `build.bat` собирал приложение без ручной настройки. Для публикации в магазинах используйте свой ключ.

## Лицензия

MIT — см. [LICENSE](LICENSE).

Контент предоставляется публичным API aniliberty.top. Права на аниме-произведения принадлежат их правообладателям.

## Summary (EN)

Native Android client for the aniliberty.top anime catalog: browsing, search, weekly schedule with reminders, favorites, watch progress and a full-featured landscape video player. Kotlin + Jetpack Compose, no WebView. Build with `build.bat` (Windows) or `./gradlew assembleDebug`. MIT licensed.
