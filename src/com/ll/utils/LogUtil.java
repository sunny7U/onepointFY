package com.ll.utils;

import android.os.Build.VERSION;
import android.util.Log;

public class LogUtil {
	public static final int VERBOSE=1; 
	public static final int DEBUG=2;  //任何有利于在调试时更详细的了解系统运行状态的内容
	public static final int INFO=3;   //用来反馈系统的当前状态给最终用户，输出的应该是对最终用户有提示意义的内容
	public static final int WARN=4;   //出现了不影响系统继续运行的问题，如资源没有释放，数据库没有关闭
	public static final int ERROR=5;  //出现的问题可能会影响到系统正常工作，空指针异常等严重错误
	public static final int NOTHING=6;
	public static final int LEVEL=NOTHING;
	
	public static void v(String tag,String msg){
		if(LEVEL<=VERBOSE){
			Log.v(tag, msg);
		}
	}
	
	public static void d(String tag,String msg){
		if(LEVEL<=DEBUG){
			Log.v(tag, msg);
		}
	}
	
	public static void i(String tag,String msg){
		if(LEVEL<=INFO){
			Log.i(tag, msg);
		}
	}
	
	public static void w(String tag,String msg){
		if(LEVEL<=WARN){
			Log.e(tag, msg);
		}
	}
	
	public static void e(String tag,String msg){
		if(LEVEL<=ERROR){
			Log.e(tag, msg);
		}
	}
	
}
