package com.ll.map;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;






//定位及地图显示
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;


import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.ll.data.MyDatabaseHelper;
import com.ll.data.TabhostActivity;
import com.ll.event.LocateAttrsMessage;
import com.ll.utils.AMapUtil;
import com.ll.utils.Constants;
import com.ll.utils.LogUtil;
import com.ll.utils.OffLineMapUtils;
import com.ll.utils.ToastUtil;
import com.ll.R;

import de.greenrobot.event.EventBus;


/**
 * 定位采点，绘制运动轨迹
 */
public class LocCollectingActivity extends Activity implements AMapLocationListener,
        LocationSource,OnMarkerClickListener,OnInfoWindowClickListener {
    private String TAG=LocCollectingActivity.class.getName();
    private int locInterval;
    

    private Button mStartButton;
    private Button mAddButton;
    private Button mStopButton;

    private AMap aMap;  //地图的对象
    private MapView mapView;  //容器类，用于显示AMap
    private AMapLocationClient mLocationClient;//声明AMapLocationClient类对象
    private AMapLocationClientOption mLocationOption;
    private UiSettings mUiSettings;
    private OnLocationChangedListener mListener;
    private LatLng prevLocation=null;
    private LatLng prevLatLng=null,curr=null;
    private MarkerOptions startOptions=null,wayOptions=null,stopOptions=null;
    private Marker marker=null;  //自定义定位小蓝点


    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Cursor cursor;
    private long exitTime;
    private int RecordingFlag=0;
    private String RouteId=null;
    private String start=null,end=null;
    private float distance; //用于记录一段路的长度
    private Handler handler=new Handler();

/*--------------------------Activity生命周期--------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "onCreate");
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//保持屏幕常亮
        setContentView(R.layout.activity_collection);
        locInterval=getIntent().getIntExtra("loc_interval", Constants.TWO_SEC)*1000;
        
        MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
        mapView =(MapView)findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        
        dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);//初始化数据库

        initMap();
        initUI();
    }

    /**
     * Started--- >visible
     */
    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.d(TAG, "onStart");
    }

    /**
     * Resumed--- >visible
     */
    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume");
        mapView.onResume();
    }

    /**
     * Paused--- >partially visible
     * 适用于数据的持久化保存
     */
    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d(TAG, "onPause");
        mapView.onPause();
    }

    /**
     * Stopped--- >hidden
     */
    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(TAG, "onStop");
        if(RecordingFlag==1){
    		aMap.clear();
    	}
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtil.d(TAG, "onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy");
        marker.destroy();
        mapView.onDestroy();
        deactivate();
        
    }

    /**
     * 当Activity被意外销毁(内存不足或用户按了Home键等)时，用于保存一些临时性数据
     * 该数据用于下次Activity调用onCreate()时恢复到意外退出前的状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtil.d(TAG, "onSaveInstanceState");
        mapView.onSaveInstanceState(outState);
    }
/*--------------------------控件初始化--------------------------------------------*/
    /**
     * 初始化UI控件
     */
    public void initUI() {
        mStartButton=(Button)findViewById(R.id.start_btn);
        mAddButton=(Button)findViewById(R.id.add_btn);
        mStopButton=(Button)findViewById(R.id.stop_btn);

        mStartButton.setOnClickListener(new mStartListener());
        mAddButton.setOnClickListener(new mAddListener());
        mStopButton.setOnClickListener(new mStopListener());
        mAddButton.setClickable(false);
        mStopButton.setClickable(false);
    }

    /**
     * 初始化AMap对象
     */
    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
    }
    
    /**
     * 设置AMap的定位显示风格和UI相关属性
     */
    private void setUpMap() {
    	//定位层相关
        aMap.setLocationSource(this);//设置定位监听。如果不设置此定位资源则定位按钮不可点击
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);//设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);//设置定位的类型为跟随模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        
        aMap.setOnMarkerClickListener(this);
        aMap.setOnInfoWindowClickListener(this);
        aMap.moveCamera(CameraUpdateFactory.zoomBy(8));
        
        mUiSettings = aMap.getUiSettings();//返回地图的用户界面设置对象
        mUiSettings.setCompassEnabled(true);//设置指南针被显示
        mUiSettings.setScaleControlsEnabled(true);//设置比例尺被显示
    }
/*--------------------------监听器内部类-------------------------------------------*/
    /**
     * 开始记录按钮监听器
     * @author Administrator
     */
    public class mStartListener implements OnClickListener{
        @Override
        public void onClick(View v) {
        	RecordingFlag=1;
            mStartButton.setClickable(false);
            mAddButton.setClickable(true);
            mStopButton.setClickable(true);
            
            LogUtil.d(TAG, "startBtn--RecordingFlag"+RecordingFlag);
            
            Intent intent=new Intent(LocCollectingActivity.this,TabhostActivity.class);
            intent.putExtra("currentLoc", curr);
            startActivityForResult(intent, 0);
        }
    }

    /**
     * 添加中间点按钮监听器
     * @author Administrator
     */
    public class mAddListener implements OnClickListener{
        @Override
        public void onClick(View v) {
        	RecordingFlag=2;
        	
        	LogUtil.d(TAG, "addBtn--RecordingFlag"+RecordingFlag);
        	
        	Intent intent=new Intent(LocCollectingActivity.this,TabhostActivity.class);
        	intent.putExtra("currentLoc", curr);
            startActivityForResult(intent, 0);
        }
    }
    
    /**
     * 停止记录按钮监听器
     * @author Administrator
     */
    public class mStopListener implements OnClickListener{
        @Override
        public void onClick(View v) {
            RecordingFlag=0;
            mStartButton.setClickable(true);
            mAddButton.setClickable(false);
            mStopButton.setClickable(false);
            
            LogUtil.d(TAG, "stopBtn--RecordingFlag"+RecordingFlag);
            
            Intent intent=new Intent(LocCollectingActivity.this,TabhostActivity.class);
            intent.putExtra("currentLoc", curr);
            startActivityForResult(intent, 0);
        }
    }
    
    /**
     * 对TabhostActivity返回的结果进行处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode==0 && resultCode==0) {
    		 Bundle point=data.getExtras();
    		 String locId=point.getString("locId");
    		 String locName=point.getString("locName");
    		 String disNum=point.getString("disNum");
    		 String disName=point.getString("disName");
    		 double[] lnglat=point.getDoubleArray("lnglat");
    		 LogUtil.i(TAG, "RecordingFlag="+RecordingFlag+";start="+(start==null?"null":start.toString())+"locName="+locName+";disNum="+disNum);
    		 if(locId==null || ("".equals(locId)) ){
    			 ToastUtil.show(this, "未添加中间点");
    			 if(RecordingFlag==1){ 
    				 RecordingFlag=0;
    				 mStartButton.setClickable(true);
		             mAddButton.setClickable(false);
    		         mStopButton.setClickable(false);
    			 }else if(RecordingFlag==0){ //按结束键之后又放弃取点
    				 RecordingFlag=2;
    				 mStartButton.setClickable(false);
		             mAddButton.setClickable(true);
    		         mStopButton.setClickable(true);
    			 }
    		 }else if(lnglat[0]!=0.0&&lnglat[1]!=0.0){
    			 //注意，Latlng构造器要先latitude后longitude
    			 LatLng latLng=new LatLng(lnglat[1], lnglat[0]);
    	         if(start==null){
    	        	 start=locId;
    	        	 prevLatLng=latLng;
    	         }else{
    	        	 distance=AMapUtils.calculateLineDistance(prevLatLng,latLng);
    	        	 end=locId;
    	        	 addRouteToDB();
    	        	 start=locId;
    	        	 prevLatLng=latLng;
    	         }
    	       
    	         addNodeToDB(locId,disNum,lnglat);  //每次记录一个有效点时便把点名称和所属台区计入路线端点表中
    	         if(RecordingFlag==0){//按了结束点在回来或者还未开始记录
    	        	 addMarkersToMap(latLng, locName,disName,RecordingFlag);
    	        	 drawingPath(prevLatLng, RecordingFlag);
    	        	 prevLocation=null;
    	        	 RouteId=null;
    	         }else{
//    	        	 if(RouteId!=null){
//    	        		 addPointToDB(latLng);
//    	        	 }
    	        	 RouteId=AMapUtil.getCharAndNum(10);
    	        	 drawingPath(latLng, RecordingFlag);
    	        	 addMarkersToMap(latLng,locName,disName, RecordingFlag);
    	         }
    		 }
    		 if(mLocationClient==null){
    			 activate(mListener);//考虑如果mListener==null时的异常
	         }else if(!mLocationClient.isStarted()){
	        	 mLocationClient.startLocation();
	         }
		 }
    }

    /**
     * 对Home键和back键进行监听
     */
    public boolean onKeyDown(int keyCode,KeyEvent event){
        switch (keyCode){
            case KeyEvent.KEYCODE_HOME:
            {
                moveTaskToBack(true);
                return true;
            }
            case KeyEvent.KEYCODE_BACK:
            {
                if(System.currentTimeMillis()-exitTime>2000){
                    ToastUtil.show(this, "再按一次退出线路采集");
                    exitTime=System.currentTimeMillis();
                }else{
                    deactivate();
                    finish();
                    System.exit(0);
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode,event);
    }
/*--------------------------轨迹绘制和数据储存---------------------------------------*/
    /**
     * 绘制运动轨迹
     */
    private void drawingPath(LatLng arg0,int flag) {
        if(prevLocation==null){
        	LogUtil.d(TAG, "prevLocation=null");
            prevLocation=arg0;
        }else if(!prevLocation.equals(arg0)) {
        	LogUtil.d(TAG, "prevLocation=not null");
            aMap.addPolyline((new PolylineOptions())
                    .add(prevLocation,arg0) //追加两个顶点到线段终点
                    .geodesic(true)       //设置线段为大地曲线
                    .visible(true)
                    .color(Color.GREEN)); //设置线段颜色
            prevLocation=arg0;
        }
        addPointToDB(arg0);
    }

    /**
     * 在地图上添加marker
     */
    private void addMarkersToMap(LatLng point,String title,String snippet ,int flag) {
    	LogUtil.i(TAG,flag==1? "start":(flag==2?"way":"stop")+"lng/lat="+point.longitude+"/"+point.latitude);
        switch (flag) {
            case 1:{
            	if(startOptions==null){
            		startOptions=new MarkerOptions()
	                    .anchor(0.5f, 0.5f)
	                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start))
	                    .setFlat(true);    //设置标记平贴地图
            	}
            	startOptions.title(title);
            	startOptions.snippet(snippet);
            	startOptions.position(point);
                aMap.addMarker(startOptions);
                break;}
            case 2:{
            	if(wayOptions==null){
            		LogUtil.d(TAG, "wayOptions==null");
            		wayOptions=new MarkerOptions()
	                    .anchor(0.5f, 0.5f)
	                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.point))
	            		.setFlat(true);
            	}
            	wayOptions.title(title);
            	wayOptions.snippet(snippet);
            	wayOptions.position(point);
                aMap.addMarker(wayOptions);
                break;}
            case 0:{
            	if(stopOptions==null){
            		stopOptions=new MarkerOptions()
	                    .anchor(0.5f, 0.5f)
	                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end))
	                    .setFlat(true);
            	}
            	stopOptions.title(title);
            	stopOptions.snippet(snippet);
            	stopOptions.position(point);
                aMap.addMarker(stopOptions);
                break;}
            default:
                break;
        }
    }

    /**
     * 添加点信息到Location表
     */
    public void addPointToDB(LatLng arg0){
        db=dbHelper.getWritableDatabase();
        ContentValues values=new ContentValues();

        values.put("route_id", RouteId);
        values.put("longitude", arg0.longitude);
        values.put("latitude", arg0.latitude);
        
        db.insert("Location", null, values);
        db.close();
        values.clear();
        LogUtil.i(TAG, "addPointToDB():route_id="+RouteId+";lng/lat="+arg0.longitude+"/"+arg0.latitude);
    }

    /**
     * 添加路线信息到Route表
     */
    public void addRouteToDB(){
        if(RouteId==null||start.equals(end)){
        	return;
        }
        db=dbHelper.getWritableDatabase();
        ContentValues values=new ContentValues();

        values.put("route_id", RouteId);
        values.put("start", start);
        values.put("end", end);
        values.put("distance", distance);
        values.put("record_date",AMapUtil.getSystemTime());
        values.put("collector", "");
        values.put("is_new", true);

        db.insert("Route", null, values);
        db.close();
        values.clear();
        LogUtil.i(TAG , "addRouteToDB()  route_id: "+RouteId+";start: "+start+";end: "+end+";record_date: "+AMapUtil.getSystemTime());
    }
    
   /**
    * 添加路径上节点信息到Node表
    */
   public void addNodeToDB(String locName,String disNum,double[] lnglat){
       db=dbHelper.getWritableDatabase();
       cursor=db.query("Node",null,"name = ? and district_number = ?",new String[]{locName,disNum},null,null,null);
       ContentValues values=new ContentValues();
       
       //新节点，直接添加；重复节点，更新数据
       if(cursor.moveToFirst()){
    	   values.put("addr_lng", lnglat[0]);
           values.put("addr_lat", lnglat[1]);
           values.put("record_date",AMapUtil.getSystemTime());
           values.put("is_new", true);
    	   db.update("Node", values, "name = ? and district_number = ?", new String[]{locName,disNum});
    	   values.clear();
           LogUtil.i(TAG , "addNodeToDB()重复点  Node:"+locName);
       }else{
    	   //将添加路线的时间记为record_date   
    	   values.put("district_number", disNum);
           values.put("name", locName);
           values.put("addr_lng", lnglat[0]);
           values.put("addr_lat", lnglat[1]);
           values.put("record_date",AMapUtil.getSystemTime());
           values.put("is_new", true);
           db.insert("Node", null, values);
           values.clear();
           LogUtil.i(TAG , "addNodeToDB()新节点  Node:"+locName);
       }
       db.close();
       cursor.close();
   }
/*--------------------------定位及位置变化监听---------------------------------------*/
    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
    	LogUtil.d(TAG, "activate"+locInterval);
        mListener = listener;
        if (mLocationClient == null) {
        	mLocationClient=new AMapLocationClient(this);//初始化定位客户端对象
            mLocationOption=new AMapLocationClientOption();//初始化定位参数
            mLocationClient.setLocationListener(this);//设置定位监听
            mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，同时使用网络和GPS定位，优先返回最高精度定位结果
            mLocationOption.setOnceLocation(false);//设置是否只定位一次。false表示持续定位
            mLocationOption.setInterval(locInterval);//设置定位间隔，单位毫秒。默认为2000ms
            LogUtil.d(TAG, "1111"+mLocationOption.getInterval());
            mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
        }
        mLocationClient.startLocation();//启动定位
        if(!ToastUtil.isShowing()){
            ToastUtil.showDialog(this,"正在定位中...");
        }
        handler.postDelayed(mRunable, 40000);
    }
    
   

    
    
    //用于控制定位超时
    Runnable mRunable=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(mLocationClient!=null){
				 if(ToastUtil.isShowing()){
			            ToastUtil.dismissDialog();
			        }
				 LogUtil.d(TAG, "定位失败，请检查GPS");
				 ToastUtil.show(LocCollectingActivity.this, "定位失败，请检查GPS");
			}
		}
	};

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        LogUtil.d(TAG, "deactivate()");
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }
    
    /**
     * AMapLocationListener接口的抽象方法，用于接收一步返回的定位结果
     */
    @Override
    public void onLocationChanged(AMapLocation arg0) {
    	LogUtil.d(TAG,"定位成功："+arg0.toStr());
        if (arg0 != null && arg0.getErrorCode() == 0){
        	if(ToastUtil.isShowing()){
                ToastUtil.dismissDialog();
            }
        	curr=AMapUtil.convertToLatLng(arg0);
        	//实现自定义的定位小蓝点。     把可视区域中心调整到当前位置
        	aMap.moveCamera(CameraUpdateFactory.changeLatLng(curr));
        	if(marker!=null){
        		marker.remove();
        		marker=null;
        	}
        	marker = aMap.addMarker(new MarkerOptions()
				            .anchor(0.5f, 0.5f)
				            .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
				            .position(curr)
				            .visible(true));
        	//设置小蓝点旋转角度
        	float bearing=360-arg0.getBearing();
			marker.setPosition(curr);
			marker.setRotateAngle(bearing);
			marker.setToTop();
            if(RecordingFlag!=0){
                drawingPath(curr,RecordingFlag);
            }
        } else {
            ToastUtil.show(this,"定位异常，请检查网络和GPS.");
            LogUtil.e(TAG, "定位失败," + arg0.getErrorCode()+ ": " + arg0.getErrorInfo());
        }
    }

	@Override
	public boolean onMarkerClick(Marker arg0) {
		LogUtil.d(TAG, "onMarkerClick");
		if(!arg0.isInfoWindowShown()){
			arg0.showInfoWindow();
		}
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker arg0) {
		//点击正在显示的信息窗口，则隐藏该窗口
		if(arg0.isInfoWindowShown()){
			arg0.hideInfoWindow();
		}
	}
}
