class Cabinet {
  String name;
  String md5;
  int type; //柜子类型 柜子数量  0主柜  1副柜
  int cellCount; //格子数量
  int id;

  Cabinet({this.name, this.md5, this.type, this.cellCount, this.id});

  factory Cabinet.fromMap(Map<String, dynamic> json) {
    return Cabinet(
      name: json['name'] as String,
      md5: json['md5'] as String,
      type: json['type'] as int,
      cellCount: json['cellCount'] as int,
      id: json['id'] as int,
    );
  }

  @override
  String toString() {
    return 'Cabinet{name: $name, md5: $md5, type: $type, cellCount: $cellCount, id: $id}';
  }
}
