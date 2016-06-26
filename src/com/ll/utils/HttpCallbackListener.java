package com.ll.utils;

public interface HttpCallbackListener  {
	//服务器成功响应请求时的调用
	void onFinish(String response);
	//网络操作出现异常的时候的调用
	void onError(Exception e);
	//除了异常意外的其他错误处理
	void onError(String response);
}
