import 'dart:async';
import 'dart:convert';

import 'package:cabinet_plugin/Result.dart';
import 'package:flutter/services.dart';

import 'ResultData.dart';

class FingerPlugin {
  static const MethodChannel _channel =
  const MethodChannel('cabinet.plugin.finger_channel');

  ///根据id检索
  static Future<Result> matchById(int fingerId) async {
    var resultJson =
    await _channel.invokeMethod("matchById", {'fingerId': fingerId});
    var resultMap = json.decode(resultJson);
    var result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }

  ///全库检索
  static Future<Result> match() async {
    var resultJson =
    await _channel.invokeMethod("match");
    var resultMap = json.decode(resultJson);
    var result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }

  static Future<Result> fingerInput() async {
    var resultJson = await _channel.invokeMethod("fingerInput");
    var resultMap = json.decode(resultJson);
    ResultData result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }

  static Future<Result> deleteById(int fingerId) async {
    var resultJson =
    await _channel.invokeMethod("deleteById", {'fingerId': fingerId});
    var resultMap = json.decode(resultJson);
    ResultData result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }

  static Future<Result> clear() async {
    var resultJson = await _channel.invokeMethod("clear");
    var resultMap = json.decode(resultJson);
    ResultData result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }

  static Future<Result> allUser() async {
    var resultJson = await _channel.invokeMethod("allUser");
    var resultMap = json.decode(resultJson);
    ResultData result = ResultData.fromMap(resultMap);
    if (result.flag) {
      return Success(result.data);
    } else {
      return Error(result.data as String);
    }
  }
}
