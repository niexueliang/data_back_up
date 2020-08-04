import 'dart:async';
import 'dart:convert';

import 'package:cabinet_plugin/Result.dart';
import 'package:flutter/services.dart';

import 'ResultData.dart';

class FingerPlugin {
  static const MethodChannel _channel =
      const MethodChannel('cabinet.plugin.finger_channel');

  static Future<ResultData> matchById(int fingerId) async {
    var resultJson =
        await _channel.invokeMethod("matchById", {'fingerId': fingerId});
    var resultMap = json.decode(resultJson);
    var result = ResultData.fromMap(resultMap);
    return result;
  }

  static Future<ResultData> fingerInput() async {
    var resultJson = await _channel.invokeMethod("fingerInput");
    var resultMap = json.decode(resultJson);
    ResultData result = ResultData.fromMap(resultMap);
    return result;
  }

  static Future<Result> deleteById(int fingerId) async {
    return await _channel.invokeMethod("deleteById", {'fingerId': fingerId});
  }

  static Future<Result> clear() async {
    return await _channel.invokeMethod("clear");
  }

  static Future<Result> allUser() async {
    return await _channel.invokeMethod("allUser");
  }
}
