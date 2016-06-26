package com.ll.data;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.ll.utils.HttpCallbackListener;
import com.ll.utils.HttpUtils;
import com.ll.utils.JsonUtils;
import com.ll.utils.LogUtil;
import com.ll.utils.ToastUtil;
import com.ll.R;


public class DataUpdateActivity extends Activity implements OnClickListener{
    private static String TAG=DataUpdateActivity.class.getName();
    private final int POST=0;
    private final int GET=1;
    private final int ELSE=2;
    private String routeUpdateTime;
    private String userUpdateTime;
    private String addrHead,addrTail;
    private String serverIP;

    private EditText mUrlEditText;
    private Button mComfirmButton;
    private Button mRouteDownloadButton;
    private Button mRouteUploadButton;
    private Button mUserDownloadButton;
    private Button mUserUploadButton;

    private SharedPreferences preference;
    private SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_update);

        preference=getSharedPreferences("updatetime", MODE_MULTI_PROCESS);
        editor=preference.edit();

        initViews();
    }

    /**
     * 界面初始化
     */
    public void initViews() {
        mUrlEditText=(EditText)findViewById(R.id.url_et);
        mComfirmButton=(Button)findViewById(R.id.comfirm_btn);
        mRouteDownloadButton=(Button)findViewById(R.id.route_data_download_btn);
        mRouteUploadButton=(Button)findViewById(R.id.route_data_upload_btn);
        mUserDownloadButton=(Button)findViewById(R.id.user_data_download_btn);
        mUserUploadButton=(Button)findViewById(R.id.user_data_upload_btn);

        mRouteUploadButton.setClickable(false);
        mUserUploadButton.setClickable(false);

        mComfirmButton.setOnClickListener(this);
        mRouteDownloadButton.setOnClickListener(this);
        mRouteUploadButton.setOnClickListener(this);
        mUserDownloadButton.setOnClickListener(this);
        mUserUploadButton.setOnClickListener(this);

        mUrlEditText.setText(getAddress("",ELSE));
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        String address;
        switch (v.getId()) {
            //修改服务器地址后确认
            case R.id.comfirm_btn:
                serverIP=mUrlEditText.getText().toString().trim();
                if(null==serverIP||"".equals(serverIP)){
                    ToastUtil.show(this, "主机IP不能为空");
                }else{
                    editor.putString("serverIP", serverIP);
                    editor.commit();
                    ToastUtil.show(this, "地址已更新");
                }
                break;
            //请求下载路线信息
            case R.id.route_data_download_btn:
                address=getAddress("route",GET);
                if(!ToastUtil.isShowing()){
                    ToastUtil.showDialog(this,"开始下载...");
                }
                HttpUtils.GetWithHttpClient(address, new mRouteDownloadlistener());
                break;
            //上传路线信息
            case R.id.route_data_upload_btn:
                address=getAddress("route", POST);
                if(!ToastUtil.isShowing()){
                    ToastUtil.showDialog(this,"开始上传...");
                }
                String routemsg=JsonUtils.convertLocToJSON(this);
                if(null!=routemsg&&!("".equals(routemsg))){
                    HttpUtils.PostWithHttpClient(address, routemsg, new mRouteUploadlistener());
                }else{
                	LogUtil.i(TAG, "没有可更新内容");
                }
                break;
            case R.id.user_data_download_btn:
                address=getAddress("user", GET);
                if(!ToastUtil.isShowing()){
                    ToastUtil.showDialog(this,"开始下载...");
                }
                HttpUtils.GetWithHttpClient(address, new mUserDownloadlistener());
                break;
            case R.id.user_data_upload_btn:
                address=getAddress("user", POST);
                if(!ToastUtil.isShowing()){
                    ToastUtil.showDialog(this,"开始上传...");
                }
                String usermsg=JsonUtils.convertUsersToJSON(this);
                if(null!=usermsg&&!"".equals(usermsg)){
                    HttpUtils.PostWithHttpClient(address, usermsg, new mUserUploadlistener());
                }else{
                	LogUtil.i(TAG, "没有可更新内容");
                }
                break;
            default:
                break;
        }
    }

    public String getAddress(String content,int mode){
        addrHead=preference.getString("addrHead", "http://");
        serverIP=preference.getString("serverIP", "121.199.75.35");
        addrTail=preference.getString("addrTail", ":8000/api/");
        String addr=addrHead+serverIP+addrTail;

        if(mode==GET){
            if("route".equals(content)){
                routeUpdateTime=preference.getString("routeUpdateTime", "1900-01-01");
                addr=addr+"route?date="+routeUpdateTime;
            }else if("user".equals(content)){
                userUpdateTime=preference.getString("userUpdateTime", "1900-01-01");
                addr=addr+"user?date="+userUpdateTime;
            }
            LogUtil.d(TAG, "addr1="+addr);
        }else if(mode==POST){
            if("route".equals(content)){
                addr=addr+"route";
            }else if("user".equals(content)){
                addr=addr+"user";
            }
            LogUtil.d(TAG, "addr2="+addr);
        }else{
            addr=serverIP;
            LogUtil.d(TAG, "addr3="+addr);
        }
        return addr;
    }

    public class mRouteDownloadlistener implements HttpCallbackListener{
        private String TAG=mRouteDownloadlistener.class.getName();
        @Override
        public void  onFinish(String response) {
            // 响应码为200的处理
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                	 if(ToastUtil.isShowing()){
                         ToastUtil.changeDialog("下载成功，开始解析数据...");
                     }else{
                         ToastUtil.showDialog(DataUpdateActivity.this,"下载成功，开始解析数据...");
                     }
                }
            });
            boolean result=JsonUtils.parseJSONToLoc(DataUpdateActivity.this, response);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                }
            });
            if(result==true){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mRouteUploadButton.setClickable(true);
                        routeUpdateTime=getCurrentTime();
                        editor.putString("routeUpdateTime", routeUpdateTime);
                        editor.commit();

                        ToastUtil.show(DataUpdateActivity.this, "成功解析,可以上传本地新数据");
                    }
                });
                LogUtil.d(TAG, "成功解析,可以上传本地新数据");
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        ToastUtil.show(DataUpdateActivity.this, "没有下载到新数据或者解析失败");
                    }
                });
                LogUtil.d(TAG, "数据解析失败,请重新下载");
            }
        }
        @Override
        public void onError(String response){
            //响应码为202或其他错误时的处理
            String[] responses=JsonUtils.parseJSON(DataUpdateActivity.this, response);
            if((responses[0].equals("error"))&&(responses[1].equals("-1"))){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if(ToastUtil.isShowing()){
                            ToastUtil.dismissDialog();
                        }
                        ToastUtil.show(DataUpdateActivity.this, "上传成功，服务器解析失败");
                    }
                });
                LogUtil.d(TAG,"上传成功，服务器解析失败");
            }
        }
        @Override
        public void onError(Exception e) {
        	LogUtil.d(TAG, "onError"+"出现异常");
            // 网络连接异常的处理
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                    ToastUtil.show(DataUpdateActivity.this, "下载失败,请检查网络后重新下载");
                }
            });
        }
    }

    public class mRouteUploadlistener implements HttpCallbackListener{
        private String TAG=mRouteUploadlistener.class.getName();
        @Override
        public  void onFinish(String response) {
            // 服务器响应200时的处理
            String[] responses=JsonUtils.parseJSON(DataUpdateActivity.this, response);
            if((responses[0].equals("ok"))&&(responses[1].equals("0"))){
                MyDatabaseHelper dbHelper=new MyDatabaseHelper(DataUpdateActivity.this, "Locations.db", null, 2);
                SQLiteDatabase db=dbHelper.getWritableDatabase();
                //上传新路线，上传成功后要把对应路线设为false
                ContentValues values=new ContentValues();
                values.put("is_new", false);
                db.update("Route", values, "is_new = ?", new String[]{"1"});
                db.update("Node", values, "is_new = ?", new String[]{"1"});
                values.clear();
                db.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if(ToastUtil.isShowing()){
                            ToastUtil.dismissDialog();
                        }
                        ToastUtil.show(DataUpdateActivity.this, "上传成功，本地数据库更新完毕");
                    }
                });
                LogUtil.d(TAG, "onFinish"+"上传成功，本地数据库更新完毕");
            }
        }
        @Override
        public void onError(String response){
            //服务器响应202或其他错误时的处理
            String[] responses=JsonUtils.parseJSON(DataUpdateActivity.this, response);
            if((responses[0].equals("error"))&&(responses[1].equals("-1"))){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if(ToastUtil.isShowing()){
                            ToastUtil.dismissDialog();
                        }
                        ToastUtil.show(DataUpdateActivity.this, "上传成功，服务器解析失败");
                    }
                });
            }
            LogUtil.d(TAG, "服务器解析失败");
        }
        @Override
        public void onError(Exception e) {
            // 网络连接异常的处理
        	LogUtil.d(TAG, "onError"+"出现异常");
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                    ToastUtil.show(DataUpdateActivity.this, "出现异常，请重新上传");
                }
            });
        }
    }

    public class mUserDownloadlistener implements HttpCallbackListener{
        private String TAG=mUserDownloadlistener.class.getName();
        @Override
        public void onFinish(String response) {
            //服务器响应200时的处理
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.changeDialog("下载成功，开始解析数据...");
                    }else{
                        ToastUtil.showDialog(DataUpdateActivity.this,"下载成功，开始解析数据...");
                    }
                }
            });
            boolean result=JsonUtils.parseJSONToUsers(getApplicationContext(), response);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                }
            });
            if(result==true){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mUserUploadButton.setClickable(true);
                        userUpdateTime=getCurrentTime();
                        editor.putString("userUpdateTime", userUpdateTime);
                        editor.commit();
                        
                        ToastUtil.show(DataUpdateActivity.this, "成功解析,可以上传本地新数据");
                    }
                });
                LogUtil.d(TAG, "成功解析,可以上传本地新数据");
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        ToastUtil.show(DataUpdateActivity.this, "解析失败或没有下载到新数据");
                    }
                });
                LogUtil.d(TAG, "数据解析失败，请重新下载");
            }
        }
        @Override
        public void  onError(String response){
            //服务器响应202或其他错误码的处理
            final String reString=response;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                    ToastUtil.show(DataUpdateActivity.this, "响应失败"+reString);
                }
            });
            LogUtil.d(TAG, "响应失败");
        }
        @Override
        public void onError(Exception e) {
            // 网络连接异常
        	LogUtil.d(TAG, "onError"+"出现异常");
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                    ToastUtil.show(DataUpdateActivity.this, "下载失败,请检查网络后重新下载");
                }
            });
        }
    }

    public class mUserUploadlistener implements HttpCallbackListener{
        private String TAG=mUserUploadlistener.class.getName();
        @Override
        public void onFinish(String response) {
            // 响应码为200时的数据处理
        	LogUtil.d(TAG, "onFinish");
            String[] responses=JsonUtils.parseJSON(DataUpdateActivity.this, response);

            if((responses[0].equals("ok"))&&(responses[1].equals("0"))){
                MyDatabaseHelper dbHelper=new MyDatabaseHelper(DataUpdateActivity.this, "Locations.db", null, 2);
                SQLiteDatabase db=dbHelper.getWritableDatabase();
                //上传新路线，上传成功后要把对应路线设为false
                ContentValues values=new ContentValues();
                values.put("is_new", false);
                db.update("Users", values, "is_new = ?", new String[]{"1"});
                values.clear();
                db.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if(ToastUtil.isShowing()){
                            ToastUtil.dismissDialog();
                        }
                        ToastUtil.show(DataUpdateActivity.this, "上传成功，本地数据库更新完毕");
                    }
                });
                LogUtil.d(TAG, "onFinish"+"上传成功，本地数据库更新完毕");
            }
        }
        @Override
        public void onError(String response){
            //响应码为202或其他的处理
            String[] responses=JsonUtils.parseJSON(DataUpdateActivity.this, response);
            if((responses[0].equals("error"))&&(responses[1].equals("-1"))){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if(ToastUtil.isShowing()){
                            ToastUtil.dismissDialog();
                        }
                        ToastUtil.show(DataUpdateActivity.this, "服务器解析失败，请检查数据，重新上传");
                    }
                });
            }
            LogUtil.d(TAG, "服务器解析失败，请检查数据，重新上传");
        }
        @Override
        public void onError(Exception e) {
            // 网络连接异常的处理
        	LogUtil.d(TAG, "onError"+"出现异常");
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if(ToastUtil.isShowing()){
                        ToastUtil.dismissDialog();
                    }
                    ToastUtil.show(DataUpdateActivity.this, "出现异常,请检查网络连接");
                }
            });
        }
    }

    public static String getCurrentTime(){
    	LogUtil.d(TAG, "getCurrentTime");
        Date currentDate=new Date(System.currentTimeMillis());
        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
        String currTime=formatter.format(currentDate);
        return currTime;
    }
}
