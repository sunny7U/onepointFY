package com.ll.utils;


import java.util.HashMap;
import java.util.Map;

import org.apache.http.protocol.ResponseConnControl;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class VolleyUtil {
	private static RequestQueue mQueue;
	
	public static void getJson(final Context context, 
			String urlStr, 
			final ImageView iv, 
			final String userId,
			final int mediaType){
		//获取一个RequestQueue对象，请求队列对象可以缓存所有HTTP请求，并按照一定算法并发的发出这些请求
		if(mQueue == null){
			mQueue = Volley.newRequestQueue(context);
		}
		JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(urlStr, 
				new Response.Listener<JSONArray>() {
							@Override
							public void onResponse(JSONArray response) {
								// TODO Auto-generated method stub
								//在响应成功的回调里打印服务器返回的内容
								JsonUtils.parseDownloadJSON(context, response, iv, userId, mediaType);
								Log.d("TAG","响应成功" + response.toString());
							}
				}, 
				new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError error) {
						// TODO Auto-generated method stub
						Log.e("TAG","发生错误" + error.getMessage(), error);
					}
				});
		//用JsonObjectRequest和JsonArrayRequest
//		JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(urlStr,
//				null,
//				new Response.Listener<JSONObject>() {
//					public void onResponse(JSONObject response){
//						//在响应成功的回调里打印服务器返回的内容
//						JsonUtils.parseDownloadJSON(context, response, iv, userId, mediaType);
//						Log.d("TAG","响应成功" + response.toString());
//					}
//				}, new Response.ErrorListener() {
//					public void onErrorResponse(VolleyError error){
//						Log.e("TAG","发生错误" + error.getMessage(), error);
//					}
//				});
//		
//		StringRequest str = new StringRequest(urlStr,
//				new Response.Listener<String>() {
//
//					@Override
//					public void onResponse(String response) {
//						// TODO Auto-generated method stub
//						Log.d("TAG","响应成功" + response.toString());
//					}
//				}, 
//				new Response.ErrorListener() {
//
//					@Override
//					public void onErrorResponse(VolleyError error) {
//						// TODO Auto-generated method stub
//						Log.e("TAG","发生错误" + error.getMessage(), error);
//					}
//				});
		mQueue.add(jsonArrayRequest);
	}
	
	public static void getImage(Context context, String urlStr, final ImageView iv){
		if(mQueue == null){
			mQueue = Volley.newRequestQueue(context);
		}
		//默认为get方式,会调用父类Request<Bitmap>的构造函数来设置Method.GET
		ImageRequest imgRequest = new ImageRequest(urlStr,
				new Response.Listener<Bitmap>(){
					@Override
					public void onResponse(Bitmap response) {
						// TODO Auto-generated method stub
						Log.d("VolleyTest", "imagell");
						iv.setVisibility(View.VISIBLE);
						iv.setImageBitmap(response);
					}
			
		        },
		        0,
		        0,
		        Config.RGB_565,
		        new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						// 如果请求图片失败就展示默认图片
						Log.d("VolleyTest", "error="+error.getMessage());
					}
				});
		//将request对象添加到请求队列中
		mQueue.add(imgRequest); 
	}
	
	public static void post(Context context, String urlStr, final String strToPost){
		RequestQueue mQueue = Volley.newRequestQueue(context);
		Response.Listener<String> listener = new Response.Listener<String>(){
			public void onResponse(String response){
				//对服务器响应内容做处理
			}
		};
		Response.ErrorListener errorListener = new Response.ErrorListener() {
			public void onErrorResponse(VolleyError error){
				//对服务器返回的错误信息做处理
			}
		};
		//设定请求方式为POST
		StringRequest strRequest = new StringRequest(Method.POST, 
				urlStr, listener, errorListener){
			//getParams()是StringRequest的父类Request类的方法，重写此方法用于传入要提交的message
			@Override
			protected Map<String, String> getParams() throws AuthFailureError {
				Map<String, String> map = new HashMap<String, String>();
				map.put("msg", strToPost);
				return map;
			}
		};
		
		mQueue.add(strRequest);
	}
	
}
