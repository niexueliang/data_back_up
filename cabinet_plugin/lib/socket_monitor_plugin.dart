import 'dart:async';

import 'package:flutter/services.dart';

class SocketMonitorPlugin {
  static const EventChannel _connectChannel =
      const EventChannel('cabinet.plugin.socket_monitor');

  //监听通道是否建立
  static StreamSubscription<dynamic> getChannelConnectListener(
      Function(bool) connectListener) {
    return _connectChannel.receiveBroadcastStream().listen(
      (event) {
        print('event=$event');
        connectListener(event);
      },
      onError: (error) => print('unknown ERROR=$error'),
    );
  }
}
