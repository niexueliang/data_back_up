import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:cabinet_plugin/Cabinet.dart';
import 'package:cabinet_plugin/CellData.dart';
import 'package:cabinet_plugin/Result.dart';

class SocketServerPlugin {
  static const MethodChannel _channel =
      const MethodChannel('cabinet.plugin.socket_server_channel');

  //身份认证
  static Future<Result> identification() async {
    var resultJson = await _channel.invokeMethod('identification');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    String rFID = result.isNotEmpty ? result[0] as String : '';
    return Success(rFID);
  }

  //扫描柜体
  static Future<Result> scan(String md5, String cell) async {
    var resultJson =
        await _channel.invokeMethod('scan', {'md5': '$md5', 'cell': '$cell'});
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    List result = json.decode(resultJson);
    var rRFIDs = result.map((element) => element as String).toList();
    return Success(rRFIDs);
  }

  //获取柜体指定格子信息
  static Future<Result> getCabinetCellMessage(String md5, String cell) async {
    var resultJson = await _channel.invokeMethod(
        'getCabinetCellMessage', {'md5': '$md5', 'cell': '$cell'});
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  //获取设备装填
  static Future<List<Cabinet>> getDevices() async {
    List<Cabinet> result;
    var devices = await _channel.invokeMethod('getDevices');
    if (devices != null) {
      result = [];
      var deviceJson = json.decode(devices);
      deviceJson.forEach((device) {
        var cabinet = Cabinet.fromMap(device);
        result.add(cabinet);
      });
    }
    return result;
  }

  static Future<Result> openCabinetCellDoor(String md5, String cell) async {
    var resultJson = await _channel
        .invokeMethod('openCabinetCellDoor', {'md5': '$md5', 'cell': '$cell'});
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    return Success(json.decode(resultJson));
  }

  static Future<Result> getCabinetMainMessage() async {
    var resultJson = await _channel.invokeMethod('getCabinetMainMessage');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }
}
