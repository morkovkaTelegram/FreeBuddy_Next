# Huawei protocol modules

`HeadphonesProtocol` contains model identity, transport metadata and an explicit
list of `ProtocolModule` instances. It does not expose commands for unsupported
features. The repository and UI resolve typed modules such as `AncModule`,
`HoldModule`, `EqualizerModule` and `DualConnectModule`; if a module is absent,
that feature cannot form a command and is not shown.

Every module owns its initial read requests, write/read-back commands and packet
reducer. Shared, stateless Huawei modules are reused by verified profiles. A
model-specific implementation is required only where packets or capabilities
differ.

## Adding a model

1. Verify the profile and packets against OpenFreebuds and real hardware.
2. Create a `HeadphonesProtocol` with identity, SPP UUID, colors and modules.
3. Reuse only confirmed common Huawei modules.
4. Implement a model-specific module for every protocol difference.
5. Add packet, reducer and module-composition tests.
6. Register the profile in `HeadphonesProtocolRegistry` only after validation.

The UI derives capabilities from module presence. Do not add a second boolean
capability table and do not delegate one complete model profile to another.

## Current profiles

FreeBuds 5i and 6i share device info, battery, advanced ANC, auto pause,
gestures, low latency and LDAC modules. They intentionally differ in equalizer
and Dual Connect composition:

- 5i uses built-in EQ and Dual Connect with per-device auto-connect;
- 6i uses custom EQ and Dual Connect without per-device auto-connect.

Firmware update is not implemented.
