# Huawei model profiles

Each supported model must implement `HeadphonesProtocol` and be registered in
`HeadphonesProtocolRegistry.verifiedProfiles` only after validation on real
hardware. The repository obtains matching, RFCOMM UUID, initial reads, parser,
write commands, read-back commands and capabilities exclusively from the
active profile; commands cannot leak between models.

## Adding HUAWEI FreeBuds 6i

OpenFreebuds marks its 6i profile as tested and reports Huawei SPP service
channel 1. Android resolves the standard SPP UUID to the remote RFCOMM channel,
so validate discovery on a real pair before adding any fallback/reflection for
an explicit channel.

Create `FreeBuds6iProtocol : HeadphonesProtocol`, then verify every command and
only then register it. Known OpenFreebuds capability differences versus 5i:

- custom equalizer is supported (`customEqualizer = true`);
- built-in EQ codes remain 1, 2, 3 and 9;
- Dual Connect is supported, but per-device auto-connect is not
  (`dualConnectAutoConnect = false`);
- split long-tap configuration, dynamic ANC, low latency, auto pause, swipe,
  double/triple tap and device information are supported;
- firmware update is not implemented.

Do not alias `FreeBuds5iProtocol`: copy/derive only commands confirmed by the
Openfreebuds 6i handler and field logs. Add protocol reducer and registry tests,
then gate UI with `HeadphonesSnapshot.capabilities`.
