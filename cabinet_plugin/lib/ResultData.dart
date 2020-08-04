class ResultData {
  // true表示 正常完整  false表示异常状态
  bool flag;

  //当flag为true时表示具体的数据类型，false表示错误原因
  var data;

  ResultData({this.flag, this.data}) {
    this.flag = flag;
    this.data = data;
  }

  factory ResultData.fromMap(Map<String, dynamic> json) {
    return ResultData(
      flag: json['flag'] as bool,
      data: json['data'],
    );
  }
}
