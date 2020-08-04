class CellData {
  //门锁状态  0开启 1关闭
  int state;

  //气压
  double pressure;

  //温度
  double temperature;

  //湿度
  double humidity;

  CellData({this.state, this.pressure, this.temperature, this.humidity});

  factory CellData.fromMap(Map<String, dynamic> json) {
    return CellData(
      state: json['state'] as int,
      pressure: json['pressure'] as double,
      temperature: json['temperature'] as double,
      humidity: json['humidity'] as double,
    );
  }
}
