class Result {
  //判定是否得到正常数据
  bool get succeeded {
    return this is Success && (this as Success).data != null;
  }

  //判定是否得到错误数据
  bool get errored {
    return this is Error;
  }

  //获取正常数据的data
  T getData<T>() {
    if (this is Success) {
      var data = (this as Success).data;
      if (data is T) {
        return data;
      } else {
        throw Exception('data type error!');
      }
    } else {
      throw Exception('error => class  TYPE is ERROR!');
    }
  }

  //获取异常数据的内容
  T getException<T>() {
    if (this is Error) {
      var data = (this as Error).data;
      if (data is T) {
        return data;
      } else {
        throw Exception('data type error!');
      }
    } else {
      throw Exception('error => class  TYPE is Success!');
    }
  }
}

class Success<T> extends Result {
  T data;
  Success(this.data);
}

class Error<T> extends Result {
  T data;
  Error(data);
}
