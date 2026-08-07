// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Polish (`pl`).
class AppLocalizationsPl extends AppLocalizations {
  AppLocalizationsPl([String locale = 'pl']) : super(locale);

  @override
  String get appTitle => 'FreeBuddy';

  @override
  String get helloWorld => 'Witaj świecie!';

  @override
  String get leftBudShort => 'Lewa';

  @override
  String get rightBudShort => 'Prawa';

  @override
  String get caseShort => 'Etui';

  @override
  String get pageHomeConnected => 'Połączone';

  @override
  String get pageHomeDisconnected => 'Słuchawki nie połączone 🙅';

  @override
  String get pageHomeDisconnectedDesc =>
      'Twoje słuchawki nie są połączone z telefonem 😨';

  @override
  String get pageHomeDisconnectedOpenSettings =>
      'Otwórz ustawienia bluetooth żeby połączyć 💙';

  @override
  String get pageHomeConnecting => 'Łączenie...';

  @override
  String get pageHomeUnknown => 'Nieznane :(';

  @override
  String get pageHomeNoPermission => 'Nie mam pozwolenia na blutut 😢';

  @override
  String get pageHomeNoPermissionGrant => 'Pozwól na blutut 🔑';

  @override
  String get pageHomeNoPermissionOpenSettings =>
      'Otwórz ustawienia żeby pozwolić ⚙';

  @override
  String get pageHomeBluetoothDisabled => 'Bluetooth jest wyłączony 📶🚫';

  @override
  String get pageHomeBluetoothDisabledEnable => 'Włącz ☝';

  @override
  String get pageHomeBluetoothDisabledOpenSettings =>
      'Otwórz ustawienia żeby włączyć 💙';

  @override
  String get pageHomeNotPaired =>
      'Nie masz żadnych wspieranych słuchawek sparowanych z telefonem 😿';

  @override
  String get pageHomeNotPairedPairOpenSettings =>
      'Otwórz ustawienia bluetooth żeby sparować 🔌';

  @override
  String get pageHomeNotPairedPairOpenDemo => 'Wypróbuj apke bez słuchawek 🌐';

  @override
  String get pageHomeConnectedClosed =>
      'Słuchawki są połączone, ale nie z FreeBuddy 🤨';

  @override
  String get pageHomeConnectedClosedDesc =>
      'Pewnie otworzyłeś inną apke - połącz spowrotem 👀';

  @override
  String get pageHomeConnectedClosedConnect => 'Połącz ze słuchawkami';

  @override
  String get swipe => 'Trzepnąć';

  @override
  String get swipeDesc =>
      'Umożliwia regulację głośności poprzez przesuwanie w górę/w dół';

  @override
  String get autoPause => 'Auto pauza ⏸';

  @override
  String get autoPauseDesc =>
      'Muzyka automatycznie się zatrzyma kiedy ściągniesz słuchawki';

  @override
  String get lowLatency => 'Niskie opóźnienie';

  @override
  String get lowLatencyDesc =>
      'Minimalizuje opóźnienia, co jest przydatne w grach';

  @override
  String get soundQuality => 'Włącz kodek LDAC';

  @override
  String get soundQualityDesc =>
      'Zapewnia lepszą jakość dźwięku, ale może powodować zakłócenia';

  @override
  String get pageHeadphonesSettingsEqualizer => 'Korektor dźwięku';

  @override
  String get pageHeadphonesSettingsEqualizerDefault => 'Domyślny';

  @override
  String get pageHeadphonesSettingsEqualizerHardBass => 'Bas';

  @override
  String get pageHeadphonesSettingsEqualizerTreble => 'Tony wysokie';

  @override
  String get pageHeadphonesSettingsEqualizerVoices => 'Głosy';

  @override
  String get pageHeadphonesSettingsGestures => 'Gesty';

  @override
  String get pageHeadphonesSettingsTitle => 'Ustawienia słuchawek 🔧';

  @override
  String get pageHeadphonesSettingsDoubleTap => 'Dwukrotny tap';

  @override
  String get pageHeadphonesSettingsDoubleTapDesc =>
      'Tapnięcie dwa razy w słuchawkę spowoduje::';

  @override
  String get pageHeadphonesSettingsDoubleTapPlayPause => 'Start/pauza';

  @override
  String get pageHeadphonesSettingsDoubleTapNextSong => 'Następny utwór';

  @override
  String get pageHeadphonesSettingsDoubleTapPrevSong => 'Poprzedni utwór';

  @override
  String get pageHeadphonesSettingsDoubleTapAssist => 'Asystent głosowy';

  @override
  String get pageHeadphonesSettingsDoubleTapNone => 'Nic';

  @override
  String get pageHeadphonesSettingsTripleTap => 'Trzykrotnie tap';

  @override
  String get pageHeadphonesSettingsTripleTapDesc =>
      'Tapnięcie trzy razy w słuchawkę spowoduje::';

  @override
  String get pageHeadphonesSettingsLeftBud => 'Lewa słuchawka';

  @override
  String get pageHeadphonesSettingsRightBud => 'Prawa słuchawka';

  @override
  String get pageHeadphonesSettingsHold => 'Przytrzymanie';

  @override
  String get pageHeadphonesSettingsHoldDesc =>
      'Przytrzymanie słuchawki będzie przełączać te tryby ANC:';

  @override
  String get ancNoiseCancel => 'Redukcja hałasu 🔇';

  @override
  String get ancNoiseCancelDesc => 'Wygłusza hałas dookoła Ciebie';

  @override
  String get ancOff => 'Wyłączona 🤷';

  @override
  String get ancOffDesc => 'Wyłącza ANC';

  @override
  String get ancAwareness => 'Przepuszczanie 🔊';

  @override
  String get ancAwarenessDesc => 'Pozwala ci słyszeć twoje otoczenie';

  @override
  String get ancCancelLevelNormal => 'Normalny';

  @override
  String get ancCancelLevelComfort => 'Komfortowy';

  @override
  String get ancCancelLevelUltra => 'Ultra';

  @override
  String get ancCancelLevelDynamic => 'Dynamiczny';

  @override
  String get dualConnect => 'Podwójne połączenie';

  @override
  String get dualConnectConnect => 'Połącz';

  @override
  String get dualConnectDisconnect => 'Rozłącz';

  @override
  String get dualConnectUnpair => 'Usuń parowanie';

  @override
  String get dualConnectUnpairDesc =>
      'To urządzenie zostanie rozparowane i nie będzie można się z nim połączyć do czasu ponownego sparowania. Kontynuować?';

  @override
  String get dualConnectModePlaying => 'Odtwarzanie';

  @override
  String get dualConnectModeConnected => 'Połączone';

  @override
  String get dualConnectModeOffline => 'Offline';

  @override
  String get dualConnectDeviceName => 'Nazwa urządzenia';

  @override
  String get dualConnectDeviceMac => 'Adres MAC';

  @override
  String get dualConnectAutoConnect => 'Automatyczne łączenie';

  @override
  String get dualConnectPreferred => 'Preferowane urządzenie';

  @override
  String get settings => 'Ustawienia';

  @override
  String get privacyPolicy => 'Polityka prywatności';

  @override
  String get privacyPolicyTitle => '\"Polityka prywatności\"';

  @override
  String get privacyPolicyText =>
      'Ta aplikacja nie zbiera żadnych informacji o tobie - żadnych emaili, identyfikatorów, lub czegokolwiek takiego na serwerze, bo nawet go nie mam :D\nFreeBuddy nie korzysta też z Google Analytics, czy innego serwisu który zbierałby twoje dane <3';

  @override
  String get privacyPolicyUrl =>
      'https://the.lastgimbus.com/privacy-policy/freebuddy/';

  @override
  String get pageAboutTitle => 'O aplikacji';

  @override
  String get pageAboutMeHeader => 'O aplikacji i o mnie';

  @override
  String get pageAboutMeBio =>
      'Heja 👋 Jestem @TheLastGimbus, twórca tej apki 🧑‍💻 Mam nadzieje że cie ona raduje! Stworzyłem ją z czystej pasji 💖';

  @override
  String get pageAboutMeAnyQuestions =>
      'Jeśli masz *JAKIEKOLWIEK* pytania, sugestie, albo po prostu chcesz zagadać, możesz śmiało pisać do mnie na poniższych:';

  @override
  String get pageAboutMeOpenSource =>
      'FreeBuddy jest w 💯% open source - co znaczy, że możesz obejrzeć kod z którego jest stworzone: ';

  @override
  String get pageAboutMeBlog =>
      'Napisałem też bloga o tym jak FreeBuddy zostało stworzone 🧑‍🎓 możesz go przeczytać tutaj: ';

  @override
  String get pageAboutMentionsHeader => 'Wyróżnienia 🎖';

  @override
  String get pageAboutMentionsPeopleHeader => 'Ludzie 🧑:';

  @override
  String get pageAboutMentionsPeopleStreet =>
      'Wszyscy ludzie na ulicy z Freebudsami którzy mnie nie zlali 🚿';

  @override
  String get pageAboutMentionsPeopleHuawei =>
      'Huawei, za stworzenie ciulowej apki na motywacje 💩';

  @override
  String get pageAboutMentionsTechHeader => 'Biblioteki i technologie 🤖:';

  @override
  String get pageAboutOpenSourceLicensesBtn => 'Licencje open source';

  @override
  String get pageIntroTitle => 'Witaj we FreeBuddy 👋';

  @override
  String get pageIntroWhatIsThis =>
      'FreeBuddy to open-source aplikacja do twoich słuchaweczek 🎧';

  @override
  String get pageIntroSupported =>
      'Obecnie wspierane są:\n - Huawei FreeBuds 5i\n - Huawei FreeBuds 4i\n - Huawei FreeBuds 3i';

  @override
  String get pageIntroShortPrivacyPolicy =>
      'Ta aplikacja nie zbiera żadnych maili, identyfikatorów, czy innych osobistych danych 🎉 Możesz o tym poczytać tutaj: ';

  @override
  String get pageIntroAnyQuestions =>
      'Jeśli masz jakiekolwiek pytania, napisz do mnie śmiało 💌 Wejdź w \"Ustawienia->O aplikacji\" po moje socjale!';

  @override
  String get pageIntroQuit => 'Oki doki 👍';

  @override
  String get cancel => 'Anuluj';
}
