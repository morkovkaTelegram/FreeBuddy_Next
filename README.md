# FreeBuddy Native 🎧

Нативное Android-приложение для управления **Huawei FreeBuds 5i**.

## Технологии

- Kotlin 2.2.20
- Jetpack Compose + Material 3
- Kotlin Coroutines / StateFlow
- Android Bluetooth RFCOMM
- Glance App Widget
- Gradle 8.14.3 / AGP 8.11.1
- Android 7.0+ (minSdk 24), targetSdk 36

Flutter и Dart больше не используются. Bluetooth broadcast-обработчики не вызывают
пользовательский код синхронно и не блокируют Android main thread. Все операции
RFCOMM (`connect`, `read`, `write`, `close`) выполняются на `Dispatchers.IO`.

## Возможности

- автоматическое подключение к сопряжённым FreeBuds 5i;
- заряд левого/правого наушника и футляра;
- ANC, прозрачность и уровни шумоподавления;
- эквалайзер;
- автопауза, низкая задержка и LDAC;
- настройка двойного/тройного касания, удержания и смахивания;
- Dual Connect;
- Android-виджет заряда;
- английская, русская и польская локализации.

## Сборка

```bash
cd android
./gradlew test assembleDebug
./gradlew bundleRelease
```

Для подписи создайте `android/key.properties`. Файл и ключи игнорируются Git.

## Миграция

Нативная версия сохраняет package ID `com.lastgimbus.the.freebuddy` и подписывается
тем же ключом, поэтому устанавливается как обновление Flutter-версии. Первая
нативная версия: `0.3.0` (`versionCode 16`).

Старая Flutter-реализация сохранена в Git-коммите `5d9ed35`.
