import 'dart:convert';

import 'package:cabinet_plugin/CellData.dart';
import 'package:cabinet_plugin/Result.dart';
import 'package:flutter/services.dart';

class SocketClientPlugin {
  static const MethodChannel _channel =
      const MethodChannel('cabinet.plugin.socket_server_channel');

  ///上传操作变动文件
  static Future<Result> uploadDbFile() async {
    var resultJson = await _channel.invokeMethod('db_file_upload');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  ///上传资源文件
  static Future<Result> uploadResourceFile() async {
    var resultJson = await _channel.invokeMethod('resource_file_upload');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  ///下载操作变动文件
  static Future<Result> downloadDbFile() async {
    var resultJson = await _channel.invokeMethod('db_file_download');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  ///下载资源文件
  static Future<Result> downloadResourceFile() async {
    var resultJson = await _channel.invokeMethod('resource_file_download');
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  ///鉴权验证
  static Future<Result> authenticate(String password) async {
    var resultJson =
        await _channel.invokeMethod('authenticate', {'password': password});
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }

  ///修改鉴权
  static Future<Result> updateAuthenticate(String password) async {
    var resultJson = await _channel
        .invokeMethod('update_authenticate', {'password': password});
    if (resultJson == null) {
      print('管道不存在');
      return Error('管道不存在');
    }
    var result = json.decode(resultJson);
    print('cellData=$resultJson');
    return Success(CellData.fromMap(result));
  }
}
