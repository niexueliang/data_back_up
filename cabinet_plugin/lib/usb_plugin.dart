import 'package:flutter/services.dart';

class UsbPlugin {
  ///usb监听器
  static const EventChannel _eventChannel =
      const EventChannel('cabinet.plugin.usb_monitor');

  ///监听usb
  static getUsbListener(Function(bool) usbListener) {
    _eventChannel.receiveBroadcastStream().listen(
          (event) => usbListener(event),
          onError: (error) => print('unknown ERROR=$error'),
        );
  }
}
