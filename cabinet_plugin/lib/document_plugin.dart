import 'dart:async';

import 'package:flutter/services.dart';

class DocumentPlugin {
  static const MethodChannel _channel =
      const MethodChannel('cabinet.plugin.document');

  /// 备份  0 sd备份到内置sd卡  1 usb备份到u盘
  static Future<bool> backUp(
      int backType, String backName, String content) async {
    Map map = {'backType': backType, 'backName': backName, 'content': content};
    final bool result = await _channel.invokeMethod("backUp", map);
    return result;
  }

  //选择文件
  static Future<String> pickFile(List<String> params) async {
    final String result = await _channel.invokeMethod("pickFile", params);
    return result;
  }
}
