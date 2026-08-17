# FreeBuddy Next

[English](#english) · [Русский](#русский)

## English

A native Android application for controlling supported Huawei FreeBuds.

> FreeBuddy Next is an independent open-source project and is not affiliated with, endorsed by, or sponsored by Huawei.
>
> Artificial intelligence tools were used to assist with code analysis, implementation, debugging, testing, and documentation. Technical decisions, validation on real devices, and responsibility for the final result remain with the project maintainer.

Current version: **0.4.0 (versionCode 37)**

### Supported headphones

| Model | Status | Notes |
|---|---|---|
| HUAWEI FreeBuds 4i | Supported and tested on a real device | Battery, basic ANC, auto-pause, double-tap, hold controls, device information and white color detection |
| HUAWEI FreeBuds 5i | Supported and tested | Battery, ANC, gestures, EQ, LDAC, low latency, Dual Connect, local sound-based finding, device information and color detection |
| HUAWEI FreeBuds 6i | Supported and tested on real devices | Battery, ANC, gestures, built-in and custom EQ, LDAC, low latency, Dual Connect, local sound-based finding, device information and color detection |

The protocol layer is model-specific and modular. `FreeBuds4iProtocol`, `FreeBuds5iProtocol` and `FreeBuds6iProtocol` compose reusable feature modules through the shared `HeadphonesProtocol` and `ProtocolModule` interfaces. Unsupported features are absent from a profile instead of being represented by inactive capability flags.

### Features

#### Headphone controls

- Battery levels for the left earbud, right earbud and charging case.
- Charging state reporting.
- Noise cancellation modes:
  - Off;
  - Noise cancelling;
  - Transparency.
- Noise cancellation intensity levels, including Dynamic mode where supported.
- Auto-pause when an earbud is removed.
- Low-latency mode.
- LDAC toggle.
- Dual Connect:
  - paired-device list;
  - connection state;
  - connect and disconnect;
  - preferred device;
  - per-device auto-connect where supported;
  - unpairing.
- Gesture configuration, depending on the model:
  - double tap;
  - triple tap;
  - touch and hold;
  - ANC modes cycled by holding;
  - swipe.
- Local sound-based earbud finding for connected FreeBuds 5i and 6i:
  - independent left/right signal;
  - maximum-volume safety warning;
  - automatic stop when leaving the screen, backgrounding the app, or disconnecting.

#### Equalizer

- Four built-in presets: Default, Bass boost, Treble boost and Vocals.
- FreeBuds 6i custom equalizer editor:
  - up to three presets;
  - 10 bands: 60 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 12 kHz and 16 kHz;
  - range from −6.0 dB to +6.0 dB;
  - 0.5 dB step;
  - create, rename, edit, select, reset and delete presets.

#### Device information and diagnostics

Tap the headphone image to open device information:

- model and submodel;
- firmware and hardware versions;
- case, left-earbud and right-earbud serial numbers;
- estimated production dates decoded from serial numbers;
- warning when the earbuds appear to have been manufactured in different periods;
- unknown device-information TLVs;
- FreeBuds 6i equalizer diagnostics;
- copyable raw MBB protocol responses.

Color detection currently uses confirmed device-information values:

- FreeBuds 4i: white; black and red remain available as manual choices until their codes are confirmed;
- FreeBuds 5i: white, blue and black;
- FreeBuds 6i: white, black and purple.

A manual color override is stored per physical headset set. The case serial number is the primary identity, with Bluetooth MAC used as a fallback until the serial number is available.

#### Widgets

FreeBuddy Next provides four Glance widgets:

1. Battery · 3 columns;
2. Battery · 3 rows;
3. Battery · compact;
4. ANC modes.

Widget features:

- Material You colors on Android 12 and newer;
- static compatible palette on Android 9–11;
- per-widget background transparency;
- per-widget element transparency;
- 5% transparency steps;
- optional full labels, L/R labels, or hidden labels for the two regular battery widgets;
- live configuration preview;
- localized dynamic previews on supported Android versions;
- exact pixel-based battery progress rendering;
- direct ANC mode switching from the home screen;
- accessible descriptions for TalkBack.

#### Appearance and accessibility

- System, light and dark application themes.
- Dynamic Material You colors.
- Optional decorative emojis.
- Manual headset color override per headset set.
- English, Russian, Polish and German localization.
- TalkBack semantics for headings, status changes, switches, radio buttons, sliders, widgets and interactive cards.

### Architecture

FreeBuddy Next is a native Android application. Flutter and Dart are no longer used.

Main technologies:

- Kotlin 2.2.20;
- Jetpack Compose and Material 3;
- Kotlin Coroutines and StateFlow;
- Android Bluetooth RFCOMM;
- Huawei MBB protocol with CRC16-XMODEM validation;
- AndroidX Glance App Widgets;
- Preferences DataStore;
- Gradle 8.14.3;
- Android Gradle Plugin 8.13.2;
- JDK 21.

Bluetooth socket operations (`connect`, `read`, `write`, and `close`) run on IO dispatchers. Writes are serialized, connection attempts are cancellable and time-limited, and Bluetooth broadcasts do not synchronously call application code on the Android main thread.

### Android requirements

```text
minSdk:     28 (Android 9)
targetSdk:  36
compileSdk: 36
```

Material You widget colors require Android 12 or newer. All core features and widgets remain available on Android 9–11 with fallback colors and previews.

### Building

#### Prerequisites

- JDK 21;
- Android SDK Platform 36;
- Android SDK Build-Tools 36.0.0;
- Android SDK Platform-Tools.

Set the SDK path in `android/local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

#### Debug build

```bash
./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug
```

#### Release verification and build

```bash
./android/gradlew -p android \
  clean \
  :app:testReleaseUnitTest \
  :app:lintRelease \
  :app:assembleRelease \
  --no-daemon \
  --no-configuration-cache
```

The APK is created at:

```text
android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

#### Release signing

Create `android/key.properties`:

```properties
storeFile=/absolute/path/to/freebuddy-release.jks
storePassword=your_store_password
keyAlias=freebuddy
keyPassword=your_key_password
```

`key.properties`, keystores and build artifacts are ignored by Git. Keep the release key and its passwords backed up securely; future updates must use the same signing key.

#### Install with ADB

```bash
adb install -r -g android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

### Privacy

FreeBuddy Next communicates directly with paired headphones over Bluetooth. It does not require an account and does not upload device diagnostics. Diagnostic data is copied only when the user explicitly presses the copy button.

Diagnostic dumps may contain Bluetooth addresses and real serial numbers. Remove or mask them before publishing logs publicly.

### Name and package ID change

The original application was named **FreeBuddy** and used this package ID:

```text
com.lastgimbus.the.freebuddy
```

The native Kotlin continuation was renamed to **FreeBuddy Next** and now uses:

```text
io.github.razziell.freebuddynext
```

Because the package ID changed, Android treats FreeBuddy Next as a separate application. It can be installed alongside the original FreeBuddy, but it cannot update it in place. Permissions, application settings, and home-screen widgets are not migrated automatically and must be configured again.

### Migration history

The application was rewritten from Flutter/Dart to native Kotlin starting with version 0.3.0. The previous Flutter implementation remains available in Git history. FreeBuddy Next continues the project under the new name and package ID shown above.

### License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).

---

## Русский

Нативное Android-приложение для управления поддерживаемыми Huawei FreeBuds.

> FreeBuddy Next — независимый проект с открытым исходным кодом, не связанный с Huawei и не поддерживаемый компанией Huawei.
>
> При анализе кода, разработке, отладке, тестировании и подготовке документации использовались инструменты искусственного интеллекта. Технические решения, проверка на реальных устройствах и ответственность за итоговый результат остаются за сопровождающим проекта.

Текущая версия: **0.4.0 (versionCode 37)**

### Поддерживаемые наушники

| Модель | Статус | Примечания |
|---|---|---|
| HUAWEI FreeBuds 4i | Поддерживаются и проверены на реальном устройстве | Заряд, базовый ANC, автопауза, двойное касание, удержание, сведения об устройстве и определение белого цвета |
| HUAWEI FreeBuds 5i | Поддерживаются и протестированы | Заряд, ANC, жесты, эквалайзер, LDAC, низкая задержка, Dual Connect, локальный звуковой поиск, сведения об устройстве и определение цвета |
| HUAWEI FreeBuds 6i | Поддерживаются и проверены на реальных устройствах | Заряд, ANC, жесты, встроенный и пользовательский эквалайзер, LDAC, низкая задержка, Dual Connect, локальный звуковой поиск, сведения об устройстве и определение цвета |

Протокольный слой разделён по моделям и модулям. `FreeBuds4iProtocol`, `FreeBuds5iProtocol` и `FreeBuds6iProtocol` собираются из переиспользуемых функциональных модулей через интерфейсы `HeadphonesProtocol` и `ProtocolModule`. Неподдерживаемая функция отсутствует в профиле, а не представлена неактивным флагом возможностей.

### Возможности

#### Управление наушниками

- Заряд левого наушника, правого наушника и кейса.
- Состояние зарядки компонентов.
- Режимы шумоподавления:
  - выключено;
  - шумоподавление;
  - прозрачность.
- Уровни шумоподавления, включая динамический режим на поддерживаемых моделях.
- Автоматическая пауза при снятии наушника.
- Режим низкой задержки.
- Переключатель LDAC.
- Двойное подключение:
  - список сопряжённых устройств;
  - состояние подключения;
  - подключение и отключение;
  - приоритетное устройство;
  - автоподключение для отдельных устройств на поддерживаемых моделях;
  - удаление сопряжения.
- Настройка жестов в зависимости от модели:
  - двойное касание;
  - тройное касание;
  - касание и удержание;
  - режимы ANC, переключаемые удержанием;
  - смахивание.
- Локальный звуковой поиск подключённых FreeBuds 5i и 6i:
  - независимый сигнал левого и правого наушника;
  - предупреждение о максимальной громкости;
  - автоматическая остановка при выходе с экрана, сворачивании приложения или отключении.

#### Эквалайзер

- Четыре встроенных пресета: По умолчанию, Басы, Высокие и Вокал.
- Редактор пользовательского эквалайзера FreeBuds 6i:
  - до трёх пресетов;
  - 10 полос: 60 Гц, 125 Гц, 250 Гц, 500 Гц, 1 кГц, 2 кГц, 4 кГц, 8 кГц, 12 кГц и 16 кГц;
  - диапазон от −6,0 до +6,0 дБ;
  - шаг 0,5 дБ;
  - создание, переименование, редактирование, выбор, сброс и удаление пресетов.

#### Сведения об устройстве и диагностика

Нажмите на изображение наушников, чтобы открыть сведения об устройстве:

- модель и подмодель;
- версии прошивки и аппаратной части;
- серийные номера кейса, левого и правого наушников;
- вычисленные даты производства по серийным номерам;
- предупреждение, если наушники предположительно произведены в разные периоды;
- неизвестные TLV-поля сведений об устройстве;
- диагностика эквалайзера FreeBuds 6i;
- копирование сырых ответов протокола MBB.

Автоматическое определение цвета использует подтверждённые значения сведений об устройстве:

- FreeBuds 4i: белый; чёрный и красный пока доступны как ручной выбор до подтверждения кодов;
- FreeBuds 5i: белый, голубой и чёрный;
- FreeBuds 6i: белый, чёрный и фиолетовый.

Принудительный цвет сохраняется отдельно для каждого физического комплекта. Основным идентификатором служит серийный номер кейса, а Bluetooth MAC используется как резерв до получения серийного номера.

#### Виджеты

FreeBuddy Next предоставляет четыре Glance-виджета:

1. «Заряд · 3 столбца»;
2. «Заряд · 3 строки»;
3. «Заряд · компактный»;
4. «Режимы шумоподавления».

Возможности виджетов:

- цвета Material You на Android 12 и новее;
- совместимая статическая палитра на Android 9–11;
- отдельная прозрачность внешнего фона каждого экземпляра;
- отдельная прозрачность внутренних элементов;
- шаг прозрачности 5%;
- полные подписи, L/R или отсутствие подписей для двух основных батарейных виджетов;
- живое превью настройки;
- локализованные динамические эскизы на поддерживаемых версиях Android;
- точное пиксельное отображение уровня заряда;
- прямое переключение ANC с рабочего стола;
- описания для TalkBack.

#### Оформление и доступность

- Системная, светлая и тёмная тема приложения.
- Динамические цвета Material You.
- Возможность скрыть декоративные эмодзи.
- Принудительный выбор цвета для каждого комплекта наушников.
- Английская, русская, польская и немецкая локализации.
- Семантика TalkBack для заголовков, изменений статуса, переключателей, радиокнопок, ползунков, виджетов и интерактивных карточек.

### Архитектура

FreeBuddy Next является нативным Android-приложением. Flutter и Dart больше не используются.

Основные технологии:

- Kotlin 2.2.20;
- Jetpack Compose и Material 3;
- Kotlin Coroutines и StateFlow;
- Android Bluetooth RFCOMM;
- протокол Huawei MBB с проверкой CRC16-XMODEM;
- AndroidX Glance App Widgets;
- Preferences DataStore;
- Gradle 8.14.3;
- Android Gradle Plugin 8.13.2;
- JDK 21.

Операции Bluetooth-сокета (`connect`, `read`, `write` и `close`) выполняются в IO-потоках. Запись сериализована, подключение поддерживает отмену и тайм-аут, а Bluetooth broadcast не вызывает пользовательский код синхронно в главном потоке Android.

### Требования Android

```text
minSdk:     28 (Android 9)
targetSdk:  36
compileSdk: 36
```

Цвета Material You для виджетов требуют Android 12 или новее. Все основные функции и виджеты доступны на Android 9–11 с резервной палитрой и статическими эскизами.

### Сборка

#### Необходимые компоненты

- JDK 21;
- Android SDK Platform 36;
- Android SDK Build-Tools 36.0.0;
- Android SDK Platform-Tools.

Укажите путь SDK в `android/local.properties`:

```properties
sdk.dir=/абсолютный/путь/к/Android/Sdk
```

#### Debug-сборка

```bash
./android/gradlew -p android :app:testDebugUnitTest :app:assembleDebug
```

#### Проверка и release-сборка

```bash
./android/gradlew -p android \
  clean \
  :app:testReleaseUnitTest \
  :app:lintRelease \
  :app:assembleRelease \
  --no-daemon \
  --no-configuration-cache
```

APK создаётся по пути:

```text
android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

#### Подпись release

Создайте файл `android/key.properties`:

```properties
storeFile=/абсолютный/путь/к/freebuddy-release.jks
storePassword=пароль_хранилища
keyAlias=freebuddy
keyPassword=пароль_ключа
```

`key.properties`, хранилища ключей и результаты сборки игнорируются Git. Надёжно сохраните release-ключ и пароли: все будущие обновления должны использовать тот же ключ.

#### Установка через ADB

```bash
adb install -r -g android/app/build/outputs/apk/release/FreeBuddy_Next_<versionName>+<versionCode>.apk
```

### Конфиденциальность

FreeBuddy Next напрямую связывается с сопряжёнными наушниками по Bluetooth. Приложение не требует учётной записи и не отправляет диагностику устройства в сеть. Диагностические данные копируются только после явного нажатия пользователем кнопки копирования.

Диагностические дампы могут содержать Bluetooth-адреса и реальные серийные номера. Перед публичной публикацией логов удалите или замаскируйте эти данные.

### Смена названия и package ID

Исходное приложение называлось **FreeBuddy** и использовало package ID:

```text
com.lastgimbus.the.freebuddy
```

Нативное продолжение на Kotlin переименовано в **FreeBuddy Next** и теперь использует:

```text
io.github.razziell.freebuddynext
```

Из-за смены package ID Android считает FreeBuddy Next отдельным приложением. Его можно установить одновременно с исходным FreeBuddy, но установить как обновление поверх него нельзя. Разрешения, настройки приложения и виджеты на рабочем столе автоматически не переносятся — их необходимо настроить заново.

### История миграции

Переход с Flutter/Dart на нативный Kotlin начался с версии 0.3.0. Предыдущая Flutter-реализация остаётся доступной в истории Git. FreeBuddy Next продолжает проект под новым названием и package ID, указанными выше.

### Лицензия

Проект распространяется по лицензии Apache License 2.0. См. [LICENSE](LICENSE).
