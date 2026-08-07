// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'FreeBuddy';

  @override
  String get helloWorld => 'Hello World!';

  @override
  String get leftBudShort => 'Left';

  @override
  String get rightBudShort => 'Right';

  @override
  String get caseShort => 'Case';

  @override
  String get pageHomeConnected => 'Connected';

  @override
  String get pageHomeDisconnected => 'Headphones not connected 🙅';

  @override
  String get pageHomeDisconnectedDesc =>
      'Your headphones are not connected to your phone 😨';

  @override
  String get pageHomeDisconnectedOpenSettings =>
      'Open bluetooth settings to connect them 💙';

  @override
  String get pageHomeConnecting => 'Connecting...';

  @override
  String get pageHomeUnknown => 'Unknown :(';

  @override
  String get pageHomeNoPermission => 'No permission to access bluetooth 😢';

  @override
  String get pageHomeNoPermissionGrant => 'Grant permission 🔑';

  @override
  String get pageHomeNoPermissionOpenSettings => 'Open settings to grant ⚙';

  @override
  String get pageHomeBluetoothDisabled => 'Bluetooth is disabled 📶🚫';

  @override
  String get pageHomeBluetoothDisabledEnable => 'Enable ☝';

  @override
  String get pageHomeBluetoothDisabledOpenSettings =>
      'Open settings to enable 💙';

  @override
  String get pageHomeNotPaired =>
      'You don\'t have any supported headphones paired to your phone 😿';

  @override
  String get pageHomeNotPairedPairOpenSettings =>
      'Open bluetooth settings to pair 🔌';

  @override
  String get pageHomeNotPairedPairOpenDemo => 'Try app without headphones 🌐';

  @override
  String get pageHomeConnectedClosed =>
      'Headphones are connected, but not with FreeBuddy 🤨';

  @override
  String get pageHomeConnectedClosedDesc =>
      'You probably opened another app - connect again 👀';

  @override
  String get pageHomeConnectedClosedConnect => 'Connect to headphones';

  @override
  String get swipe => 'Swipe';

  @override
  String get swipeDesc => 'Allows you to adjust volume by swiping up/down';

  @override
  String get autoPause => 'Auto pause ⏸';

  @override
  String get autoPauseDesc =>
      'Music will automatically pause when you take off headphones';

  @override
  String get lowLatency => 'Low latency';

  @override
  String get lowLatencyDesc =>
      'Allows you to minimize delays, useful for games';

  @override
  String get soundQuality => 'Enable LDAC codec';

  @override
  String get soundQualityDesc =>
      'Allows you to listen with more quality but can cause lags';

  @override
  String get pageHeadphonesSettingsEqualizer => 'Equalizer';

  @override
  String get pageHeadphonesSettingsEqualizerDefault => 'Default';

  @override
  String get pageHeadphonesSettingsEqualizerHardBass => 'Bass';

  @override
  String get pageHeadphonesSettingsEqualizerTreble => 'Treble';

  @override
  String get pageHeadphonesSettingsEqualizerVoices => 'Voices';

  @override
  String get pageHeadphonesSettingsGestures => 'Gestures';

  @override
  String get pageHeadphonesSettingsTitle => 'Headphones settings 🔧';

  @override
  String get pageHeadphonesSettingsDoubleTap => 'Double tap';

  @override
  String get pageHeadphonesSettingsDoubleTapDesc => 'Tap a bud twice to:';

  @override
  String get pageHeadphonesSettingsDoubleTapPlayPause => 'Play/Pause';

  @override
  String get pageHeadphonesSettingsDoubleTapNextSong => 'Next song';

  @override
  String get pageHeadphonesSettingsDoubleTapPrevSong => 'Previous song';

  @override
  String get pageHeadphonesSettingsDoubleTapAssist => 'Voice assistant';

  @override
  String get pageHeadphonesSettingsDoubleTapNone => 'None';

  @override
  String get pageHeadphonesSettingsTripleTap => 'Triple tap';

  @override
  String get pageHeadphonesSettingsTripleTapDesc => 'Tap a bud thrice to:';

  @override
  String get pageHeadphonesSettingsLeftBud => 'Left bud';

  @override
  String get pageHeadphonesSettingsRightBud => 'Right bud';

  @override
  String get pageHeadphonesSettingsHold => 'Touch and hold';

  @override
  String get pageHeadphonesSettingsHoldDesc =>
      'Holding a bud will toggle these ANC modes:';

  @override
  String get ancNoiseCancel => 'Noise cancelling 🔇';

  @override
  String get ancNoiseCancelDesc => 'Reduces noise around you';

  @override
  String get ancOff => 'Off 🤷';

  @override
  String get ancOffDesc => 'Turns ANC off';

  @override
  String get ancAwareness => 'Awareness 🔊';

  @override
  String get ancAwarenessDesc => 'Allows you to hear your surroundings';

  @override
  String get ancCancelLevelNormal => 'Normal';

  @override
  String get ancCancelLevelComfort => 'Comfort';

  @override
  String get ancCancelLevelUltra => 'Ultra';

  @override
  String get ancCancelLevelDynamic => 'Dynamic';

  @override
  String get dualConnect => 'Dual connect';

  @override
  String get dualConnectConnect => 'Connect';

  @override
  String get dualConnectDisconnect => 'Disconnect';

  @override
  String get dualConnectUnpair => 'Unpair';

  @override
  String get dualConnectUnpairDesc =>
      'This device will be unpaired and cannot be connected until paired again. Continue?';

  @override
  String get dualConnectModePlaying => 'Playing';

  @override
  String get dualConnectModeConnected => 'Connected';

  @override
  String get dualConnectModeOffline => 'Offline';

  @override
  String get dualConnectDeviceName => 'Device name';

  @override
  String get dualConnectDeviceMac => 'MAC address';

  @override
  String get dualConnectAutoConnect => 'Auto connect';

  @override
  String get dualConnectPreferred => 'Preferred device';

  @override
  String get settings => 'Settings';

  @override
  String get privacyPolicy => 'Privacy policy';

  @override
  String get privacyPolicyTitle => '\"Privacy policy\"';

  @override
  String get privacyPolicyText =>
      'This app does not collect any personal information about you - no emails, identifiers, or anything like that, on any server, because I don’t even have one :D\nFreeBuddy also doesn’t use Google Analytics, or any other service that would collect your data <3';

  @override
  String get privacyPolicyUrl =>
      'https://the.lastgimbus.com/privacy-policy/freebuddy/';

  @override
  String get pageAboutTitle => 'About app';

  @override
  String get pageAboutMeHeader => 'About this app and me';

  @override
  String get pageAboutMeBio =>
      'Hi there 👋 I\'m @TheLastGimbus, creator of this app 🧑‍💻 I hope you\'re enjoying it! I created it out of pure passion 💖';

  @override
  String get pageAboutMeAnyQuestions =>
      'If you have *ANY* questions, suggestions, or just want to chat, feel free to contact me on any of below:';

  @override
  String get pageAboutMeOpenSource =>
      'FreeBuddy is 💯% open source - meaning, you can read the source code it was made with: ';

  @override
  String get pageAboutMeBlog =>
      'I also made a blog about how it was created 🧑‍🎓 you can give it a read here: ';

  @override
  String get pageAboutMentionsHeader => 'Honorable mentions 🎖';

  @override
  String get pageAboutMentionsPeopleHeader => 'People 🧑:';

  @override
  String get pageAboutMentionsPeopleStreet =>
      'All people on the street with Freebuds 4i that did\'t ghost me 👻';

  @override
  String get pageAboutMentionsPeopleHuawei =>
      'Huawei, for creating a crappy app to motivate me 💩';

  @override
  String get pageAboutMentionsTechHeader => 'Libraries and tech 🤖:';

  @override
  String get pageAboutOpenSourceLicensesBtn => 'Open source licenses';

  @override
  String get pageIntroTitle => 'Welcome to FreeBuddy 👋';

  @override
  String get pageIntroWhatIsThis =>
      'FreeBuddy is open source app for your headphones 🎧';

  @override
  String get pageIntroSupported =>
      'Currently supported are:\n - Huawei FreeBuds 5i\n - Huawei FreeBuds 4i\n - Huawei FreeBuds 3i';

  @override
  String get pageIntroShortPrivacyPolicy =>
      'This app doesn\'t collect any emails, identifiers, or any personal data 🎉 You can read more about it here: ';

  @override
  String get pageIntroAnyQuestions =>
      'If you have any questions, feel free to contact me 💌 Look at \"Settings->About\" for my socials!';

  @override
  String get pageIntroQuit => 'Okay 👍';

  @override
  String get cancel => 'Cancel';
}
