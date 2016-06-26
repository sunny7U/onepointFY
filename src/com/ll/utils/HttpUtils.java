package com.ll.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import com.autonavi.amap.mapcore.ConnectionManager;

public class HttpUtils {
    private static String TAG=HttpUtils.class.getName();

    /**
     * send a GET request with HttpClient,using a subThread inside the function
     * @param address target address of HttpServer
     * @param listener callback listener for response
     */
    public static void GetWithHttpClient(final String address,final HttpCallbackListener listener){
        //开启子线程去执行具体网络操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try{
                    LogUtil.i(TAG, "GetWithHttpClient");
                    //创建DefaultHttpClient对象
                    HttpClient httpClient=new DefaultHttpClient();
//                    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
//                    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 6000);
                    //创建一个HttpGet对象，传入目标网络地址
                    HttpGet httpGet=new HttpGet(address);
                    //发起Get请求并获得响应
                    HttpResponse httpResponse=httpClient.execute(httpGet);
                    String response=null;
                    HttpEntity returnEntity=httpResponse.getEntity();
                    //服务器返回数据有中文，直接toString(returnEntity)会有乱码
                    response=EntityUtils.toString(returnEntity,"utf-8");

                    //HttpResponse对象里封装了服务器响应数据，服务器返回的状态码为200说明请求和响应都成功了
                    if(httpResponse.getStatusLine().getStatusCode()==200){
                        LogUtil.i(TAG, "请求和响应均成功");
                        if(null!=listener){
                            //回调onFinish()方法
                            LogUtil.d(TAG, "接收成功，null!=listener,开始解析");
                            listener.onFinish(response.toString());
                        }else{
                            LogUtil.d(TAG, "接收成功，null==listener");
                        }
                    }else{
                        LogUtil.i(TAG, "请求失败，错误码："+httpResponse.getStatusLine().getStatusCode());
                        if(null!=listener){
                            //回调onFinish()方法
                            LogUtil.d(TAG, "null!=listener,开始解析");
                            listener.onError(response.toString());
                        }else{
                            LogUtil.d(TAG, "null==listener");
                        }
                    }

                }catch (Exception e){
                    if(null!=listener){
                        //回调onError()方法
                        LogUtil.d(TAG, "出现异常，null!=listener");
                        e.printStackTrace();
                        listener.onError(e);
                    }else {
                        LogUtil.d(TAG, "出现异常，null==listener");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * send a POST request with HttpClient,using a subThread inside the function
     * @param address  target address of HttpServer
     * @param message   message which will be sent to HttpServer
     * @param listener  listener callback listener for response
     */
    public static void PostWithHttpClient(final String address,final String message, final HttpCallbackListener listener){
        //开启子线程去执行具体网络操作
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try{
                    LogUtil.i(TAG, "PostWithHttpClient");
                    //创建DefaultHttpClient对象
                    HttpClient httpClient=new DefaultHttpClient();
//                    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6000);
//                    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 6000);
                    //创建一个HttpGet对象，传入目标网络地址
                    HttpPost httpPost=new HttpPost(address);
                    //通过StringEntity来存放待提交的数据
                    StringEntity postEntity=new StringEntity(message,"utf-8");
                    //将构建好的entity对象传入
                    httpPost.setEntity(postEntity);
                    //发起Post请求
                    HttpResponse httpResponse=httpClient.execute(httpPost);
                    String response=null;
                    HttpEntity returnEntity=httpResponse.getEntity();
                    //服务器返回数据有中文，直接toString(returnEntity)会有乱码
                    response=EntityUtils.toString(returnEntity,"utf-8");

                    //HttpResponse对象里封装了服务器响应数据，服务器返回的状态码为200说明请求和响应都成功了
                    if(httpResponse.getStatusLine().getStatusCode()==200){
                        LogUtil.i(TAG, "请求和响应均成功");
                        if(null!=listener){
                            //回调onFinish()方法
                            LogUtil.d(TAG, "接收成功，null!=listener,开始解析");
                            listener.onFinish(response.toString());
                        }else{
                            LogUtil.d(TAG, "接收成功，null==listener");
                        }
                    }else{
                        LogUtil.i(TAG, "请求失败，错误码："+httpResponse.getStatusLine().getStatusCode());
                        if(null!=listener){
                            //回调onFinish()方法
                            LogUtil.d(TAG, "接收成功，null!=listener,开始解析");
                            listener.onError(response.toString());
                        }else{
                            LogUtil.d(TAG, "接收成功，null==listener");
                        }
                    }

                }catch (Exception e){
                    if(null!=listener){
                        //回调onError()方法
                        LogUtil.d(TAG, "出现异常，null!=listener");
                        listener.onError(e);
                    }else {
                        LogUtil.d(TAG, "出现异常，null==listener");
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

}
