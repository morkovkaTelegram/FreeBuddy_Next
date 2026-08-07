import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_pl.dart';
import 'app_localizations_ru.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
      : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('pl'),
    Locale('ru')
  ];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'FreeBuddy'**
  String get appTitle;

  /// No description provided for @helloWorld.
  ///
  /// In en, this message translates to:
  /// **'Hello World!'**
  String get helloWorld;

  /// No description provided for @leftBudShort.
  ///
  /// In en, this message translates to:
  /// **'Left'**
  String get leftBudShort;

  /// No description provided for @rightBudShort.
  ///
  /// In en, this message translates to:
  /// **'Right'**
  String get rightBudShort;

  /// No description provided for @caseShort.
  ///
  /// In en, this message translates to:
  /// **'Case'**
  String get caseShort;

  /// No description provided for @pageHomeConnected.
  ///
  /// In en, this message translates to:
  /// **'Connected'**
  String get pageHomeConnected;

  /// No description provided for @pageHomeDisconnected.
  ///
  /// In en, this message translates to:
  /// **'Headphones not connected 🙅'**
  String get pageHomeDisconnected;

  /// No description provided for @pageHomeDisconnectedDesc.
  ///
  /// In en, this message translates to:
  /// **'Your headphones are not connected to your phone 😨'**
  String get pageHomeDisconnectedDesc;

  /// No description provided for @pageHomeDisconnectedOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open bluetooth settings to connect them 💙'**
  String get pageHomeDisconnectedOpenSettings;

  /// No description provided for @pageHomeConnecting.
  ///
  /// In en, this message translates to:
  /// **'Connecting...'**
  String get pageHomeConnecting;

  /// No description provided for @pageHomeUnknown.
  ///
  /// In en, this message translates to:
  /// **'Unknown :('**
  String get pageHomeUnknown;

  /// No description provided for @pageHomeNoPermission.
  ///
  /// In en, this message translates to:
  /// **'No permission to access bluetooth 😢'**
  String get pageHomeNoPermission;

  /// No description provided for @pageHomeNoPermissionGrant.
  ///
  /// In en, this message translates to:
  /// **'Grant permission 🔑'**
  String get pageHomeNoPermissionGrant;

  /// No description provided for @pageHomeNoPermissionOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open settings to grant ⚙'**
  String get pageHomeNoPermissionOpenSettings;

  /// No description provided for @pageHomeBluetoothDisabled.
  ///
  /// In en, this message translates to:
  /// **'Bluetooth is disabled 📶🚫'**
  String get pageHomeBluetoothDisabled;

  /// No description provided for @pageHomeBluetoothDisabledEnable.
  ///
  /// In en, this message translates to:
  /// **'Enable ☝'**
  String get pageHomeBluetoothDisabledEnable;

  /// No description provided for @pageHomeBluetoothDisabledOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open settings to enable 💙'**
  String get pageHomeBluetoothDisabledOpenSettings;

  /// No description provided for @pageHomeNotPaired.
  ///
  /// In en, this message translates to:
  /// **'You don\'t have any supported headphones paired to your phone 😿'**
  String get pageHomeNotPaired;

  /// No description provided for @pageHomeNotPairedPairOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open bluetooth settings to pair 🔌'**
  String get pageHomeNotPairedPairOpenSettings;

  /// No description provided for @pageHomeNotPairedPairOpenDemo.
  ///
  /// In en, this message translates to:
  /// **'Try app without headphones 🌐'**
  String get pageHomeNotPairedPairOpenDemo;

  /// No description provided for @pageHomeConnectedClosed.
  ///
  /// In en, this message translates to:
  /// **'Headphones are connected, but not with FreeBuddy 🤨'**
  String get pageHomeConnectedClosed;

  /// No description provided for @pageHomeConnectedClosedDesc.
  ///
  /// In en, this message translates to:
  /// **'You probably opened another app - connect again 👀'**
  String get pageHomeConnectedClosedDesc;

  /// No description provided for @pageHomeConnectedClosedConnect.
  ///
  /// In en, this message translates to:
  /// **'Connect to headphones'**
  String get pageHomeConnectedClosedConnect;

  /// No description provided for @swipe.
  ///
  /// In en, this message translates to:
  /// **'Swipe'**
  String get swipe;

  /// No description provided for @swipeDesc.
  ///
  /// In en, this message translates to:
  /// **'Allows you to adjust volume by swiping up/down'**
  String get swipeDesc;

  /// No description provided for @autoPause.
  ///
  /// In en, this message translates to:
  /// **'Auto pause ⏸'**
  String get autoPause;

  /// No description provided for @autoPauseDesc.
  ///
  /// In en, this message translates to:
  /// **'Music will automatically pause when you take off headphones'**
  String get autoPauseDesc;

  /// No description provided for @lowLatency.
  ///
  /// In en, this message translates to:
  /// **'Low latency'**
  String get lowLatency;

  /// No description provided for @lowLatencyDesc.
  ///
  /// In en, this message translates to:
  /// **'Allows you to minimize delays, useful for games'**
  String get lowLatencyDesc;

  /// No description provided for @soundQuality.
  ///
  /// In en, this message translates to:
  /// **'Enable LDAC codec'**
  String get soundQuality;

  /// No description provided for @soundQualityDesc.
  ///
  /// In en, this message translates to:
  /// **'Allows you to listen with more quality but can cause lags'**
  String get soundQualityDesc;

  /// No description provided for @pageHeadphonesSettingsEqualizer.
  ///
  /// In en, this message translates to:
  /// **'Equalizer'**
  String get pageHeadphonesSettingsEqualizer;

  /// No description provided for @pageHeadphonesSettingsEqualizerDefault.
  ///
  /// In en, this message translates to:
  /// **'Default'**
  String get pageHeadphonesSettingsEqualizerDefault;

  /// No description provided for @pageHeadphonesSettingsEqualizerHardBass.
  ///
  /// In en, this message translates to:
  /// **'Bass'**
  String get pageHeadphonesSettingsEqualizerHardBass;

  /// No description provided for @pageHeadphonesSettingsEqualizerTreble.
  ///
  /// In en, this message translates to:
  /// **'Treble'**
  String get pageHeadphonesSettingsEqualizerTreble;

  /// No description provided for @pageHeadphonesSettingsEqualizerVoices.
  ///
  /// In en, this message translates to:
  /// **'Voices'**
  String get pageHeadphonesSettingsEqualizerVoices;

  /// No description provided for @pageHeadphonesSettingsGestures.
  ///
  /// In en, this message translates to:
  /// **'Gestures'**
  String get pageHeadphonesSettingsGestures;

  /// No description provided for @pageHeadphonesSettingsTitle.
  ///
  /// In en, this message translates to:
  /// **'Headphones settings 🔧'**
  String get pageHeadphonesSettingsTitle;

  /// No description provided for @pageHeadphonesSettingsDoubleTap.
  ///
  /// In en, this message translates to:
  /// **'Double tap'**
  String get pageHeadphonesSettingsDoubleTap;

  /// No description provided for @pageHeadphonesSettingsDoubleTapDesc.
  ///
  /// In en, this message translates to:
  /// **'Tap a bud twice to:'**
  String get pageHeadphonesSettingsDoubleTapDesc;

  /// No description provided for @pageHeadphonesSettingsDoubleTapPlayPause.
  ///
  /// In en, this message translates to:
  /// **'Play/Pause'**
  String get pageHeadphonesSettingsDoubleTapPlayPause;

  /// No description provided for @pageHeadphonesSettingsDoubleTapNextSong.
  ///
  /// In en, this message translates to:
  /// **'Next song'**
  String get pageHeadphonesSettingsDoubleTapNextSong;

  /// No description provided for @pageHeadphonesSettingsDoubleTapPrevSong.
  ///
  /// In en, this message translates to:
  /// **'Previous song'**
  String get pageHeadphonesSettingsDoubleTapPrevSong;

  /// No description provided for @pageHeadphonesSettingsDoubleTapAssist.
  ///
  /// In en, this message translates to:
  /// **'Voice assistant'**
  String get pageHeadphonesSettingsDoubleTapAssist;

  /// No description provided for @pageHeadphonesSettingsDoubleTapNone.
  ///
  /// In en, this message translates to:
  /// **'None'**
  String get pageHeadphonesSettingsDoubleTapNone;

  /// No description provided for @pageHeadphonesSettingsTripleTap.
  ///
  /// In en, this message translates to:
  /// **'Triple tap'**
  String get pageHeadphonesSettingsTripleTap;

  /// No description provided for @pageHeadphonesSettingsTripleTapDesc.
  ///
  /// In en, this message translates to:
  /// **'Tap a bud thrice to:'**
  String get pageHeadphonesSettingsTripleTapDesc;

  /// No description provided for @pageHeadphonesSettingsLeftBud.
  ///
  /// In en, this message translates to:
  /// **'Left bud'**
  String get pageHeadphonesSettingsLeftBud;

  /// No description provided for @pageHeadphonesSettingsRightBud.
  ///
  /// In en, this message translates to:
  /// **'Right bud'**
  String get pageHeadphonesSettingsRightBud;

  /// No description provided for @pageHeadphonesSettingsHold.
  ///
  /// In en, this message translates to:
  /// **'Touch and hold'**
  String get pageHeadphonesSettingsHold;

  /// No description provided for @pageHeadphonesSettingsHoldDesc.
  ///
  /// In en, this message translates to:
  /// **'Holding a bud will toggle these ANC modes:'**
  String get pageHeadphonesSettingsHoldDesc;

  /// No description provided for @ancNoiseCancel.
  ///
  /// In en, this message translates to:
  /// **'Noise cancelling 🔇'**
  String get ancNoiseCancel;

  /// No description provided for @ancNoiseCancelDesc.
  ///
  /// In en, this message translates to:
  /// **'Reduces noise around you'**
  String get ancNoiseCancelDesc;

  /// No description provided for @ancOff.
  ///
  /// In en, this message translates to:
  /// **'Off 🤷'**
  String get ancOff;

  /// No description provided for @ancOffDesc.
  ///
  /// In en, this message translates to:
  /// **'Turns ANC off'**
  String get ancOffDesc;

  /// No description provided for @ancAwareness.
  ///
  /// In en, this message translates to:
  /// **'Awareness 🔊'**
  String get ancAwareness;

  /// No description provided for @ancAwarenessDesc.
  ///
  /// In en, this message translates to:
  /// **'Allows you to hear your surroundings'**
  String get ancAwarenessDesc;

  /// No description provided for @ancCancelLevelNormal.
  ///
  /// In en, this message translates to:
  /// **'Normal'**
  String get ancCancelLevelNormal;

  /// No description provided for @ancCancelLevelComfort.
  ///
  /// In en, this message translates to:
  /// **'Comfort'**
  String get ancCancelLevelComfort;

  /// No description provided for @ancCancelLevelUltra.
  ///
  /// In en, this message translates to:
  /// **'Ultra'**
  String get ancCancelLevelUltra;

  /// No description provided for @ancCancelLevelDynamic.
  ///
  /// In en, this message translates to:
  /// **'Dynamic'**
  String get ancCancelLevelDynamic;

  /// No description provided for @dualConnect.
  ///
  /// In en, this message translates to:
  /// **'Dual connect'**
  String get dualConnect;

  /// No description provided for @dualConnectConnect.
  ///
  /// In en, this message translates to:
  /// **'Connect'**
  String get dualConnectConnect;

  /// No description provided for @dualConnectDisconnect.
  ///
  /// In en, this message translates to:
  /// **'Disconnect'**
  String get dualConnectDisconnect;

  /// No description provided for @dualConnectUnpair.
  ///
  /// In en, this message translates to:
  /// **'Unpair'**
  String get dualConnectUnpair;

  /// No description provided for @dualConnectUnpairDesc.
  ///
  /// In en, this message translates to:
  /// **'This device will be unpaired and cannot be connected until paired again. Continue?'**
  String get dualConnectUnpairDesc;

  /// No description provided for @dualConnectModePlaying.
  ///
  /// In en, this message translates to:
  /// **'Playing'**
  String get dualConnectModePlaying;

  /// No description provided for @dualConnectModeConnected.
  ///
  /// In en, this message translates to:
  /// **'Connected'**
  String get dualConnectModeConnected;

  /// No description provided for @dualConnectModeOffline.
  ///
  /// In en, this message translates to:
  /// **'Offline'**
  String get dualConnectModeOffline;

  /// No description provided for @dualConnectDeviceName.
  ///
  /// In en, this message translates to:
  /// **'Device name'**
  String get dualConnectDeviceName;

  /// No description provided for @dualConnectDeviceMac.
  ///
  /// In en, this message translates to:
  /// **'MAC address'**
  String get dualConnectDeviceMac;

  /// No description provided for @dualConnectAutoConnect.
  ///
  /// In en, this message translates to:
  /// **'Auto connect'**
  String get dualConnectAutoConnect;

  /// No description provided for @dualConnectPreferred.
  ///
  /// In en, this message translates to:
  /// **'Preferred device'**
  String get dualConnectPreferred;

  /// No description provided for @settings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settings;

  /// No description provided for @privacyPolicy.
  ///
  /// In en, this message translates to:
  /// **'Privacy policy'**
  String get privacyPolicy;

  /// No description provided for @privacyPolicyTitle.
  ///
  /// In en, this message translates to:
  /// **'\"Privacy policy\"'**
  String get privacyPolicyTitle;

  /// No description provided for @privacyPolicyText.
  ///
  /// In en, this message translates to:
  /// **'This app does not collect any personal information about you - no emails, identifiers, or anything like that, on any server, because I don’t even have one :D\nFreeBuddy also doesn’t use Google Analytics, or any other service that would collect your data <3'**
  String get privacyPolicyText;

  /// No description provided for @privacyPolicyUrl.
  ///
  /// In en, this message translates to:
  /// **'https://the.lastgimbus.com/privacy-policy/freebuddy/'**
  String get privacyPolicyUrl;

  /// No description provided for @pageAboutTitle.
  ///
  /// In en, this message translates to:
  /// **'About app'**
  String get pageAboutTitle;

  /// No description provided for @pageAboutMeHeader.
  ///
  /// In en, this message translates to:
  /// **'About this app and me'**
  String get pageAboutMeHeader;

  /// No description provided for @pageAboutMeBio.
  ///
  /// In en, this message translates to:
  /// **'Hi there 👋 I\'m @TheLastGimbus, creator of this app 🧑‍💻 I hope you\'re enjoying it! I created it out of pure passion 💖'**
  String get pageAboutMeBio;

  /// No description provided for @pageAboutMeAnyQuestions.
  ///
  /// In en, this message translates to:
  /// **'If you have *ANY* questions, suggestions, or just want to chat, feel free to contact me on any of below:'**
  String get pageAboutMeAnyQuestions;

  /// No description provided for @pageAboutMeOpenSource.
  ///
  /// In en, this message translates to:
  /// **'FreeBuddy is 💯% open source - meaning, you can read the source code it was made with: '**
  String get pageAboutMeOpenSource;

  /// No description provided for @pageAboutMeBlog.
  ///
  /// In en, this message translates to:
  /// **'I also made a blog about how it was created 🧑‍🎓 you can give it a read here: '**
  String get pageAboutMeBlog;

  /// No description provided for @pageAboutMentionsHeader.
  ///
  /// In en, this message translates to:
  /// **'Honorable mentions 🎖'**
  String get pageAboutMentionsHeader;

  /// No description provided for @pageAboutMentionsPeopleHeader.
  ///
  /// In en, this message translates to:
  /// **'People 🧑:'**
  String get pageAboutMentionsPeopleHeader;

  /// No description provided for @pageAboutMentionsPeopleStreet.
  ///
  /// In en, this message translates to:
  /// **'All people on the street with Freebuds 4i that did\'t ghost me 👻'**
  String get pageAboutMentionsPeopleStreet;

  /// No description provided for @pageAboutMentionsPeopleHuawei.
  ///
  /// In en, this message translates to:
  /// **'Huawei, for creating a crappy app to motivate me 💩'**
  String get pageAboutMentionsPeopleHuawei;

  /// No description provided for @pageAboutMentionsTechHeader.
  ///
  /// In en, this message translates to:
  /// **'Libraries and tech 🤖:'**
  String get pageAboutMentionsTechHeader;

  /// No description provided for @pageAboutOpenSourceLicensesBtn.
  ///
  /// In en, this message translates to:
  /// **'Open source licenses'**
  String get pageAboutOpenSourceLicensesBtn;

  /// No description provided for @pageIntroTitle.
  ///
  /// In en, this message translates to:
  /// **'Welcome to FreeBuddy 👋'**
  String get pageIntroTitle;

  /// No description provided for @pageIntroWhatIsThis.
  ///
  /// In en, this message translates to:
  /// **'FreeBuddy is open source app for your headphones 🎧'**
  String get pageIntroWhatIsThis;

  /// No description provided for @pageIntroSupported.
  ///
  /// In en, this message translates to:
  /// **'Currently supported are:\n - Huawei FreeBuds 5i\n - Huawei FreeBuds 4i\n - Huawei FreeBuds 3i'**
  String get pageIntroSupported;

  /// No description provided for @pageIntroShortPrivacyPolicy.
  ///
  /// In en, this message translates to:
  /// **'This app doesn\'t collect any emails, identifiers, or any personal data 🎉 You can read more about it here: '**
  String get pageIntroShortPrivacyPolicy;

  /// No description provided for @pageIntroAnyQuestions.
  ///
  /// In en, this message translates to:
  /// **'If you have any questions, feel free to contact me 💌 Look at \"Settings->About\" for my socials!'**
  String get pageIntroAnyQuestions;

  /// No description provided for @pageIntroQuit.
  ///
  /// In en, this message translates to:
  /// **'Okay 👍'**
  String get pageIntroQuit;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'pl', 'ru'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'pl':
      return AppLocalizationsPl();
    case 'ru':
      return AppLocalizationsRu();
  }

  throw FlutterError(
      'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
      'an issue with the localizations generation tool. Please file an issue '
      'on GitHub with a reproducible sample app and the gen-l10n configuration '
      'that was used.');
}
