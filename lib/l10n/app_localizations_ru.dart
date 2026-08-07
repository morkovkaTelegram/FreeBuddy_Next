// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get appTitle => 'FreeBuddy';

  @override
  String get helloWorld => 'Привет!';

  @override
  String get leftBudShort => 'Левый';

  @override
  String get rightBudShort => 'Правый';

  @override
  String get caseShort => 'Футляр';

  @override
  String get pageHomeConnected => 'Наушники подключены 🎧';

  @override
  String get pageHomeDisconnected => 'Наушники не подключены 🙅';

  @override
  String get pageHomeDisconnectedDesc =>
      'Подключите наушники к телефону через Bluetooth 📱';

  @override
  String get pageHomeDisconnectedOpenSettings =>
      'Открыть настройки Bluetooth 💙';

  @override
  String get pageHomeConnecting => 'Подключение к наушникам… 🔄';

  @override
  String get pageHomeUnknown => 'Не удалось определить состояние наушников 😕';

  @override
  String get pageHomeNoPermission => 'Нет доступа к Bluetooth 😢';

  @override
  String get pageHomeNoPermissionGrant => 'Разрешить доступ 🔑';

  @override
  String get pageHomeNoPermissionOpenSettings =>
      'Открыть настройки приложения ⚙️';

  @override
  String get pageHomeBluetoothDisabled => 'Bluetooth выключен 📶🚫';

  @override
  String get pageHomeBluetoothDisabledEnable => 'Включить Bluetooth ☝️';

  @override
  String get pageHomeBluetoothDisabledOpenSettings =>
      'Открыть настройки Bluetooth 💙';

  @override
  String get pageHomeNotPaired =>
      'Поддерживаемые наушники не сопряжены с телефоном 😿';

  @override
  String get pageHomeNotPairedPairOpenSettings =>
      'Открыть настройки Bluetooth 🔌';

  @override
  String get pageHomeNotPairedPairOpenDemo =>
      'Открыть демонстрационный режим 🌐';

  @override
  String get pageHomeConnectedClosed =>
      'Не удалось подключить FreeBuddy к наушникам 🤨';

  @override
  String get pageHomeConnectedClosedDesc =>
      'Соединение может использовать другое приложение. Закройте его и повторите попытку 👀';

  @override
  String get pageHomeConnectedClosedConnect => 'Повторить подключение 🔄';

  @override
  String get swipe => 'Смахивание';

  @override
  String get swipeDesc =>
      'Проведите вверх или вниз по наушнику, чтобы изменить громкость.';

  @override
  String get autoPause => 'Автоматическая пауза ⏸️';

  @override
  String get autoPauseDesc =>
      'Воспроизведение приостанавливается, когда вы снимаете наушники.';

  @override
  String get lowLatency => 'Низкая задержка 🎮';

  @override
  String get lowLatencyDesc =>
      'Уменьшает задержку звука. Полезно в играх и при просмотре видео.';

  @override
  String get soundQuality => 'Кодек LDAC 🎵';

  @override
  String get soundQualityDesc =>
      'Повышает качество звука, но может снизить стабильность соединения.';

  @override
  String get pageHeadphonesSettingsEqualizer => 'Эквалайзер 🎚️';

  @override
  String get pageHeadphonesSettingsEqualizerDefault => 'Стандартный';

  @override
  String get pageHeadphonesSettingsEqualizerHardBass => 'Усиление басов';

  @override
  String get pageHeadphonesSettingsEqualizerTreble => 'Усиление высоких частот';

  @override
  String get pageHeadphonesSettingsEqualizerVoices => 'Вокал';

  @override
  String get pageHeadphonesSettingsGestures => 'Управление жестами 👆';

  @override
  String get pageHeadphonesSettingsTitle => 'Настройки наушников 🔧';

  @override
  String get pageHeadphonesSettingsDoubleTap => 'Двойное касание';

  @override
  String get pageHeadphonesSettingsDoubleTapDesc =>
      'Дважды коснитесь наушника:';

  @override
  String get pageHeadphonesSettingsDoubleTapPlayPause => 'Пуск/пауза';

  @override
  String get pageHeadphonesSettingsDoubleTapNextSong => 'Вперёд';

  @override
  String get pageHeadphonesSettingsDoubleTapPrevSong => 'Назад';

  @override
  String get pageHeadphonesSettingsDoubleTapAssist => 'Ассистент';

  @override
  String get pageHeadphonesSettingsDoubleTapNone => 'Нет';

  @override
  String get pageHeadphonesSettingsTripleTap => 'Тройное касание';

  @override
  String get pageHeadphonesSettingsTripleTapDesc =>
      'Трижды коснитесь наушника:';

  @override
  String get pageHeadphonesSettingsLeftBud => 'Левый';

  @override
  String get pageHeadphonesSettingsRightBud => 'Правый';

  @override
  String get pageHeadphonesSettingsHold => 'Касание и удержание';

  @override
  String get pageHeadphonesSettingsHoldDesc =>
      'При удержании наушника будут переключаться выбранные режимы:';

  @override
  String get ancNoiseCancel => 'Шумоподавление 🔇';

  @override
  String get ancNoiseCancelDesc => 'Уменьшает уровень окружающего шума.';

  @override
  String get ancOff => 'Выключено 🤷';

  @override
  String get ancOffDesc => 'Активное шумоподавление выключено.';

  @override
  String get ancAwareness => 'Режим прозрачности 🔊';

  @override
  String get ancAwarenessDesc => 'Позволяет слышать окружающие звуки.';

  @override
  String get ancCancelLevelNormal => 'Обычный';

  @override
  String get ancCancelLevelComfort => 'Комфортный';

  @override
  String get ancCancelLevelUltra => 'Максимальный';

  @override
  String get ancCancelLevelDynamic => 'Динамический';

  @override
  String get dualConnect => 'Подключение к двум устройствам 🔗';

  @override
  String get dualConnectConnect => 'Подключить';

  @override
  String get dualConnectDisconnect => 'Отключить';

  @override
  String get dualConnectUnpair => 'Удалить устройство';

  @override
  String get dualConnectUnpairDesc =>
      'Устройство будет удалено из памяти наушников. Для следующего подключения потребуется повторное сопряжение. Продолжить?';

  @override
  String get dualConnectModePlaying => 'Воспроизводит звук';

  @override
  String get dualConnectModeConnected => 'Подключено';

  @override
  String get dualConnectModeOffline => 'Не подключено';

  @override
  String get dualConnectDeviceName => 'Имя устройства';

  @override
  String get dualConnectDeviceMac => 'MAC-адрес';

  @override
  String get dualConnectAutoConnect => 'Автоматическое подключение';

  @override
  String get dualConnectPreferred => 'Основное устройство';

  @override
  String get settings => 'Настройки ⚙️';

  @override
  String get privacyPolicy => 'Конфиденциальность 🔒';

  @override
  String get privacyPolicyTitle => 'Политика конфиденциальности';

  @override
  String get privacyPolicyText =>
      'FreeBuddy не собирает и не передаёт персональные данные, адреса электронной почты или идентификаторы устройств. Приложение также не использует Google Analytics и другие службы аналитики.';

  @override
  String get privacyPolicyUrl =>
      'https://the.lastgimbus.com/privacy-policy/freebuddy/';

  @override
  String get pageAboutTitle => 'О FreeBuddy ℹ️';

  @override
  String get pageAboutMeHeader => 'О приложении и разработчике 👨‍💻';

  @override
  String get pageAboutMeBio =>
      'Привет! Я @TheLastGimbus, создатель FreeBuddy. Надеюсь, приложение окажется для вас полезным. Оно создано как независимый проект с открытым исходным кодом.';

  @override
  String get pageAboutMeAnyQuestions =>
      'Если у вас есть вопросы или предложения, свяжитесь со мной одним из указанных ниже способов:';

  @override
  String get pageAboutMeOpenSource =>
      'Исходный код FreeBuddy открыт и доступен по ссылке: ';

  @override
  String get pageAboutMeBlog =>
      'О разработке приложения можно прочитать в статье: ';

  @override
  String get pageAboutMentionsHeader => 'Благодарности 🎖️';

  @override
  String get pageAboutMentionsPeopleHeader => 'Участники 🧑';

  @override
  String get pageAboutMentionsPeopleStreet =>
      'Владельцам FreeBuds 4i, которые помогали тестировать приложение.';

  @override
  String get pageAboutMentionsPeopleHuawei =>
      'Huawei — за создание наушников, для которых появилось это приложение.';

  @override
  String get pageAboutMentionsTechHeader => 'Библиотеки и технологии 🤖';

  @override
  String get pageAboutOpenSourceLicensesBtn => 'Лицензии открытого ПО';

  @override
  String get pageIntroTitle => 'Добро пожаловать в FreeBuddy 👋';

  @override
  String get pageIntroWhatIsThis =>
      'FreeBuddy — приложение с открытым исходным кодом для управления наушниками 🎧';

  @override
  String get pageIntroSupported =>
      'Поддерживаемые модели:\n• Huawei FreeBuds 5i\n• Huawei FreeBuds 4i\n• Huawei FreeBuds 3i';

  @override
  String get pageIntroShortPrivacyPolicy =>
      'Приложение не собирает адреса электронной почты, идентификаторы и другие персональные данные 🎉 Подробнее: ';

  @override
  String get pageIntroAnyQuestions =>
      'Контактные данные разработчика находятся в разделе «Настройки → О FreeBuddy» 💌';

  @override
  String get pageIntroQuit => 'Продолжить 👍';

  @override
  String get cancel => 'Отмена';
}
