import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:freebuddy/l10n/app_localizations.dart';

import '../../headphones/cubit/headphones_connection_cubit.dart';
import '../../headphones/cubit/headphones_cubit_objects.dart';
import '../../headphones/framework/bluetooth_headphones.dart';
import '../pages/home/bluetooth_disabled_info_widget.dart';
import '../pages/home/connected_closed_widget.dart';
import '../pages/home/disconnected_info_widget.dart';
import '../pages/home/no_permission_info_widget.dart';
import '../pages/home/not_paired_info_widget.dart';

/// Shows headphone-dependent content only while the RFCOMM connection is open.
/// Disconnected states use standalone pages instead of stale simulated controls.
class HeadphonesConnectionEnsuringOverlay extends StatelessWidget {
  final Widget Function(BuildContext context, BluetoothHeadphones headphones)
      builder;

  const HeadphonesConnectionEnsuringOverlay({super.key, required this.builder});

  Widget _status(Widget child) => Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: child,
        ),
      );

  @override
  Widget build(BuildContext context) {
    final l = AppLocalizations.of(context)!;
    return BlocBuilder<HeadphonesConnectionCubit, HeadphonesConnectionState>(
      builder: (context, state) => AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: switch (state) {
          HeadphonesNoPermission() => _status(const NoPermissionInfoWidget()),
          HeadphonesNotPaired() => _status(const NotPairedInfoWidget()),
          HeadphonesBluetoothDisabled() =>
            _status(const BluetoothDisabledInfoWidget()),
          HeadphonesDisconnected() => _status(const DisconnectedInfoWidget()),
          HeadphonesConnecting() => _status(
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    l.pageHomeConnecting,
                    style: Theme.of(context).textTheme.headlineSmall,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  const CircularProgressIndicator(),
                ],
              ),
            ),
          HeadphonesConnectedClosed() => _status(const ConnectedClosedWidget()),
          HeadphonesConnectedOpen(headphones: final headphones) =>
            builder(context, headphones),
          _ => _status(Text(l.pageHomeUnknown)),
        },
      ),
    );
  }
}
