import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:fluttercommunicate/fluttercommunicate.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  List datas = [];

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('${showMessage()}'),
        ),
        floatingActionButton: Column(
          mainAxisAlignment: MainAxisAlignment.end,
          mainAxisSize: MainAxisSize.max,
          children: <Widget>[
            FloatingActionButton(
              child: Text(
                "识别",
                style: TextStyle(color: Colors.white),
              ),
              onPressed: () => identification(),
            ),
            FloatingActionButton(
              child: Text(
                "扫描",
                style: TextStyle(color: Colors.white),
              ),
              onPressed: () => scan(),
            ),
            FloatingActionButton(
              child: Text(
                "设备",
                style: TextStyle(color: Colors.white),
              ),
              onPressed: () => getDevices(),
            )
          ],
        ),
      ),
    );
  }

  addList(Object data) {
    setState(() {
      datas.add(data);
    });
  }

  String showMessage() {
    var buffer = StringBuffer();
    datas.forEach((data) {
      buffer.writeln(data);
    });
    return buffer.toString();
  }

  identification() async {
    String data = await Fluttercommunicate.identification();
  }

  getDevices() async {
    var data = await Fluttercommunicate.getDevices();
  }

  scan() async {
    List<String> data = await Fluttercommunicate.scan('1', '1');
  }
}
