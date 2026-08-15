# Changelog

This file is maintained in English first, followed by a complete Russian translation.

## [Unreleased]

- No unreleased changes yet.

## [Unreleased]

### Added

- Initial HUAWEI FreeBuds 4i profile with device information, battery, basic ANC, auto-pause, double-tap, and hold controls.

## [0.3.7+32] — 2026-08

### Changed

- Renamed the application to **FreeBuddy Next**.
- Changed the Android application ID and Kotlin namespace to `io.github.razziell.freebuddynext`.
- Updated widget configuration components, resources, tests and documentation for the new package.

### Important

- Android treats FreeBuddy Next as a separate application. It can be installed alongside the previous `com.lastgimbus.the.freebuddy` package.
- Existing permissions, application settings and placed widgets are not migrated automatically from the previous package.

## [0.3.6+31] — 2026-08

### Added

- Full Huawei FreeBuds 6i profile with model-specific capabilities, colors and images.
- Custom 10-band FreeBuds 6i equalizer editor:
  - frequencies from 60 Hz to 16 kHz;
  - range from −6.0 dB to +6.0 dB;
  - 0.5 dB step;
  - up to three on-device presets;
  - create, rename, edit, select, reset and delete operations.
- Extended device diagnostics:
  - unknown TLV fields;
  - lossless mixed binary/text rendering such as `0x0145 ("E")`;
  - raw MBB response history;
  - copy-to-clipboard action;
  - FreeBuds 6i EQ and preset diagnostics.
- Estimated production dates for the case and both earbuds, decoded from their serial numbers.
- Warning when the left and right earbuds appear to have been manufactured in different periods.
- Automatic color detection based on confirmed device-information codes:
  - FreeBuds 5i: white, blue and black;
  - FreeBuds 6i: black.
- Per-headset manual color override, keyed primarily by the case serial number with MAC fallback.
- TalkBack accessibility semantics for headings, live connection states, switches, radio buttons, sliders, ANC controls, widgets and interactive cards.

### Changed

- Split model-specific logic into `HeadphonesProtocol`, `FreeBuds5iProtocol` and `FreeBuds6iProtocol`.
- Aligned setting read/write packets with verified OpenFreebuds Huawei MBB handlers.
- Made ANC hold-mode constraints explicit and automatically restore a valid mode set when ANC cycling is enabled.
- Stabilized widget and lifecycle updates:
  - one combined widget update for battery and ANC changes;
  - stable DataStore flows;
  - bounded A2DP liveness polling;
  - safe ANC-widget behavior when the process or connection is unavailable.
- Updated Android build stack to AGP 8.13.2 while keeping Gradle 8.14.3, Kotlin 2.2.20 and compile/target SDK 36.

### Fixed

- Invalid empty read requests for battery, ANC, gestures, EQ, low latency and Dual Connect.
- Hold and swipe settings now write both earbuds where required.
- Low-latency read-back no longer races the headset state update.
- ANC mode toggles no longer remain permanently disabled after switching hold action to None and back.
- RFCOMM writes are serialized and connection cleanup cannot cancel itself.
- Connection failures no longer leave the interface permanently in Connecting state.
- Revoked Bluetooth permission no longer causes an unhandled `SecurityException`.
- Diagnostic packet history no longer forces a full Compose recomposition for every incoming packet.

## [0.3.3+22] — 2026-08

### Added

- Separate background and element transparency controls for every widget instance.
- Per-widget battery label modes: full names, L/R, or hidden.
- Model-aware headset color settings.

### Changed

- Improved compact widget sizing and battery progress rendering.
- Reworked dynamic and fallback widget previews to match real widget geometry and percentages.

## [0.3.2+20] — 2026-08

### Added

- ANC mode widget.
- Compact battery widget.
- Individual per-widget transparency configuration with live preview.
- English, Russian, Polish and German localization.
- Application theme selection: system, light or dark.
- Optional decorative emojis.
- Device-information dialog with firmware, hardware and serial numbers.
- Separate black, white and blue FreeBuds 5i images.

### Changed

- Improved Material You contrast, widget previews and responsive widget layouts.
- Added configurable labels to the regular battery widgets.

## [0.3.1+18] — 2026-08

### Added

- Native Glance battery widgets with Material You colors.
- Three-row and three-column battery layouts.
- Dual Connect device-management interface.
- Full Russian localization and improved Polish localization.

### Fixed

- Bluetooth connection/disconnection ANRs caused by synchronous Dart/JNI callbacks.
- Multi-headset pairing races and stale connection state.
- `100%` wrapping in battery widgets.
- Background task leaks and duplicate WorkManager jobs.

## [0.3.0+16] — 2026-08

### Breaking change

- Rewrote the application from Flutter/Dart to native Kotlin and Jetpack Compose.

### Added

- Native RFCOMM transport using coroutines and IO dispatchers.
- Huawei MBB packet parser with CRC16-XMODEM validation.
- Compose Material 3 interface.
- StateFlow-based connection state and model-specific command handling.
- Native AndroidX Glance widgets.

### Changed

- Minimum Android version raised to Android 9 (API 28).
- Preserved package ID and release-signing compatibility with earlier versions.

## [0.2.0] — 2023-09-24

- Added the original battery home-screen widget.
- Adopted wallpaper-derived Material 3 colors on Android 12.
- Moved headphone settings to a dedicated screen.
- Improved landscape layout.

## [0.1.4] — 2023-04-02

- Completed gesture settings.
- Removed the experimental sleep mode.

## [0.1.0] — 2023-01-29

- Migrated the original Flutter application to a custom Bluetooth plugin.
- Improved Android 12 Bluetooth handling.
- Added Material 3 UI, animations, tests, auto-pause and demo mode.

## [0.0.1] — 2022-09-01

- Initial release with battery display and basic ANC control.

---

# Журнал изменений — Русская версия

Сначала в этом файле приводится английская версия, ниже — её полный русский перевод.

## [Не выпущено]

- Пока нет невыпущенных изменений.

## [0.3.7+32] — 2026-08

### Изменено

- Приложение переименовано в **FreeBuddy Next**.
- Android application ID и Kotlin namespace изменены на `io.github.razziell.freebuddynext`.
- Конфигурации компонентов виджетов, ресурсы, тесты и документация обновлены для нового пакета.

### Важно

- Разрешения, настройки приложения и размещённые виджеты из прежнего пакета автоматически не переносятся.

## [0.3.6+31] — 2026-08

### Добавлено

- Полноценный профиль Huawei FreeBuds 6i с отдельными возможностями, цветами и изображениями.
- Редактор пользовательского 10-полосного эквалайзера FreeBuds 6i:
  - частоты от 60 Гц до 16 кГц;
  - диапазон от −6,0 до +6,0 дБ;
  - шаг 0,5 дБ;
  - до трёх пресетов в памяти наушников;
  - создание, переименование, редактирование, выбор, сброс и удаление.
- Расширенная диагностика устройства:
  - неизвестные TLV-поля;
  - отображение смешанных бинарных и текстовых значений без потери данных, например `0x0145 ("E")`;
  - журнал сырых ответов MBB;
  - копирование в буфер обмена;
  - диагностика эквалайзера и пресетов FreeBuds 6i.
- Вычисление предполагаемой даты производства кейса и обоих наушников по серийным номерам.
- Предупреждение, если левый и правый наушники предположительно произведены в разные периоды.
- Автоматическое определение цвета по подтверждённым кодам сведений об устройстве:
  - FreeBuds 5i: белый, голубой и чёрный;
  - FreeBuds 6i: чёрный.
- Принудительный выбор цвета отдельно для каждого комплекта: основной идентификатор — серийный номер кейса, резервный — MAC.
- Семантика TalkBack для заголовков, динамических состояний подключения, переключателей, радиокнопок, ползунков, ANC, виджетов и интерактивных карточек.

### Изменено

- Модельно-зависимая логика разделена между `HeadphonesProtocol`, `FreeBuds5iProtocol` и `FreeBuds6iProtocol`.
- Команды чтения и записи настроек приведены в соответствие с проверенными обработчиками Huawei MBB из Openfreebuds.
- Ограничения режимов ANC при удержании сделаны явными; при включении смены ANC автоматически восстанавливается допустимый набор режимов.
- Улучшена стабильность виджетов и жизненного цикла:
  - одно объединённое обновление виджетов при изменении заряда и ANC;
  - стабильные потоки DataStore;
  - ограниченный опрос состояния A2DP;
  - безопасное поведение ANC-виджета при отсутствии процесса или соединения.
- Android Gradle Plugin обновлён до 8.13.2 при сохранении Gradle 8.14.3, Kotlin 2.2.20 и compile/target SDK 36.

### Исправлено

- Некорректные пустые запросы чтения заряда, ANC, жестов, эквалайзера, низкой задержки и Dual Connect.
- Настройки удержания и смахивания теперь записываются для обоих наушников там, где это требуется протоколом.
- Повторное чтение низкой задержки больше не опережает обновление состояния наушников.
- Переключатели режимов ANC больше не остаются постоянно выключенными после переключения действия удержания на «Нет» и обратно.
- Записи RFCOMM сериализованы, а очистка соединения не может отменить сама себя.
- Ошибка подключения больше не оставляет интерфейс навсегда в состоянии «Подключение».
- Отзыв разрешения Bluetooth больше не вызывает необработанный `SecurityException`.
- Журнал диагностических пакетов больше не вызывает полную рекомпозицию Compose на каждый входящий пакет.

## [0.3.3+22] — 2026-08

### Добавлено

- Отдельная регулировка прозрачности внешнего фона и внутренних элементов каждого экземпляра виджета.
- Режимы подписей батарейных виджетов: полные названия, L/R или без подписей.
- Настройки цвета с учётом модели наушников.

### Изменено

- Улучшены размеры компактного виджета и отображение прогресса заряда.
- Динамические и статические эскизы виджетов приведены в соответствие с реальной геометрией и процентами.

## [0.3.2+20] — 2026-08

### Добавлено

- Виджет режимов ANC.
- Компактный виджет заряда.
- Индивидуальная прозрачность каждого виджета с живым превью.
- Английская, русская, польская и немецкая локализации.
- Выбор темы приложения: системная, светлая или тёмная.
- Возможность скрыть декоративные эмодзи.
- Диалог сведений об устройстве с прошивкой, аппаратной версией и серийными номерами.
- Отдельные изображения чёрных, белых и голубых FreeBuds 5i.

### Изменено

- Улучшены контраст Material You, превью и адаптивная разметка виджетов.
- Добавлена настройка подписей основных батарейных виджетов.

## [0.3.1+18] — 2026-08

### Добавлено

- Нативные Glance-виджеты заряда с цветами Material You.
- Виджеты с тремя строками и тремя столбцами.
- Интерфейс управления устройствами Dual Connect.
- Полная русская локализация и улучшенный польский перевод.

### Исправлено

- ANR при подключении и отключении Bluetooth, вызванные синхронными Dart/JNI callback.
- Гонки при нескольких сопряжённых парах и устаревшее состояние соединения.
- Перенос `100%` в виджетах заряда.
- Утечки фоновых задач и дублирующиеся задания WorkManager.

## [0.3.0+16] — 2026-08

### Критическое изменение

- Приложение переписано с Flutter/Dart на нативный Kotlin и Jetpack Compose.

### Добавлено

- Нативный RFCOMM-транспорт на корутинах и IO-потоках.
- Парсер Huawei MBB с проверкой CRC16-XMODEM.
- Интерфейс Compose Material 3.
- Состояние соединения на StateFlow и модельно-зависимые команды.
- Нативные AndroidX Glance-виджеты.

### Изменено

- Минимальная версия Android повышена до Android 9 (API 28).
- Сохранены package ID и совместимость release-подписи с предыдущими версиями.

## [0.2.0] — 2023-09-24

- Добавлен первоначальный виджет заряда на главном экране.
- Добавлены цвета Material 3 из обоев на Android 12.
- Настройки наушников перенесены на отдельный экран.
- Улучшена альбомная ориентация.

## [0.1.4] — 2023-04-02

- Завершена реализация настроек жестов.
- Удалён экспериментальный режим сна.

## [0.1.0] — 2023-01-29

- Исходное Flutter-приложение переведено на собственный Bluetooth-плагин.
- Улучшена работа Bluetooth на Android 12.
- Добавлены Material 3, анимации, тесты, автопауза и демонстрационный режим.

## [0.0.1] — 2022-09-01

- Первый выпуск с отображением заряда и базовым управлением ANC.
