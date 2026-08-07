/// Periodic Android background work.

import 'package:rxdart/rxdart.dart';
import 'package:workmanager/workmanager.dart';

import '../../../di.dart' as di;
import '../../../headphones/cubit/headphones_connection_cubit.dart';
import '../../../headphones/cubit/headphones_cubit_objects.dart';
import '../../../headphones/framework/lrc_battery.dart';
import '../../../logger.dart';
import '../appwidgets/battery_appwidget.dart';

const commonTimeout = Duration(seconds: 10);
const taskIdRoutineUpdate = 'freebuddy.routine_update';

Future<bool> routineUpdateCallback() async {
  HeadphonesConnectionCubit? cubit;
  try {
    if (await HeadphonesConnectionCubit.cubitAlreadyRunningSomewhere()) {
      loggI.d('Skipping ROUTINE_UPDATE: app cubit is already running');
      return true;
    }

    cubit = di.getHeadphonesCubit();
    final state = await cubit.stream
        .debounceTime(const Duration(seconds: 1))
        .firstWhere((state) => state is! HeadphonesConnecting)
        .timeout(commonTimeout);

    if (state is! HeadphonesConnectedOpen) {
      loggI.d('Skipping ROUTINE_UPDATE because: $state');
      return true;
    }
    if (state.headphones is! LRCBattery) {
      loggI.d('Skipping ROUTINE_UPDATE: LRCBattery is unsupported');
      return true;
    }

    final batteryData = await (state.headphones as LRCBattery)
        .lrcBattery
        .first
        .timeout(commonTimeout);
    await updateBatteryHomeWidget(batteryData);
    return true;
  } catch (error, stackTrace) {
    loggI.e(
      'ROUTINE_UPDATE failed',
      error: error,
      stackTrace: stackTrace,
    );
    return false;
  } finally {
    await cubit?.close();
  }
}

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    loggI.d(
      'Running background task $task'
      '${inputData != null ? ' - input data: $inputData' : ''}',
    );
    try {
      switch (task) {
        case taskIdRoutineUpdate:
          return await routineUpdateCallback().timeout(commonTimeout);
        default:
          throw UnsupportedError('No such task: $task');
      }
    } catch (error, stackTrace) {
      loggI.e(
        'Background task $task failed',
        error: error,
        stackTrace: stackTrace,
      );
      return false;
    }
  });
}

Future<void> init() async {
  await Workmanager().initialize(callbackDispatcher);
  await Workmanager().registerPeriodicTask(
    taskIdRoutineUpdate,
    taskIdRoutineUpdate,
    frequency: const Duration(minutes: 15),
    initialDelay: const Duration(minutes: 5),
    existingWorkPolicy: ExistingPeriodicWorkPolicy.update,
    backoffPolicy: BackoffPolicy.linear,
  );
}
