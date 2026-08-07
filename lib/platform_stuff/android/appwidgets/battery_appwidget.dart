import 'dart:async';

import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';
import 'package:rxdart/rxdart.dart';

import '../../../headphones/cubit/headphones_cubit_objects.dart';
import '../../../headphones/framework/lrc_battery.dart';
import '../../../logger.dart';

StreamSubscription<LRCBatteryLevels>? _headphonesBatteryStreamSub;

Future<void> batteryHomeWidgetHearBloc(
  BuildContext context,
  HeadphonesConnectionState state,
) async {
  await _headphonesBatteryStreamSub?.cancel();
  _headphonesBatteryStreamSub = null;

  if (state is! HeadphonesConnectedOpen || state.headphones is! LRCBattery) {
    return;
  }

  _headphonesBatteryStreamSub = (state.headphones as LRCBattery)
      .lrcBattery
      .distinct()
      .throttleTime(
        const Duration(seconds: 1),
        trailing: true,
        leading: false,
      )
      .listen(
    (event) async {
      logg.d('Updating widget from UI listener: $event');
      await updateBatteryHomeWidget(event);
    },
    onError: (Object error, StackTrace stackTrace) {
      logg.e(
        'Widget battery listener failed',
        error: error,
        stackTrace: stackTrace,
      );
    },
  );
}

Future<void> updateBatteryHomeWidget(LRCBatteryLevels batteryData) async {
  await HomeWidget.saveWidgetData<int?>('left', batteryData.levelLeft);
  await HomeWidget.saveWidgetData<int?>('right', batteryData.levelRight);
  await HomeWidget.saveWidgetData<int?>('case', batteryData.levelCase);
  await HomeWidget.updateWidget(
    name: 'BatteryWidgetReceiver',
    androidName: 'BatteryWidgetReceiver',
    qualifiedAndroidName: 'com.lastgimbus.the.freebuddy.BatteryWidgetReceiver',
  );
}
