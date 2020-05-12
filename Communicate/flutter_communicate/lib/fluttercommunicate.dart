import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';

import 'Cabinet.dart';

class Fluttercommunicate {
  static const MethodChannel _channel =
      const MethodChannel('fluttercommunicate');

  static Future<String> identification() async {
    var cards = await _channel.invokeMethod('identification');
    return cards.length > 0 ? cards[0] : '';
  }

  static Future<List<String>> scan(String channel, String cell) async {
    List<String> result = [];
    var rfids = await _channel
        .invokeMethod('scan', {'channel': '$channel', 'cell': '$cell'});
    var deviceJson = json.decode(rfids);
    deviceJson.forEach((rfid) {
      var data = rfid as String;
      result.add(data);
    });
    return result;
  }

  static Future<List<Cabinet>> getDevices() async {
    List<Cabinet> result = [];
    var devices = await _channel.invokeMethod('getDevices');
    var deviceJson = json.decode(devices);
    deviceJson.forEach((device) {
      var cabinet = Cabinet.fromMap(device);
      result.add(cabinet);
    });
    return result;
  }
}
