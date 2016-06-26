package com.ll.map;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;


//定位及地图显示
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.AMap.OnCameraChangeListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.ll.data.MyDatabaseHelper;
import com.ll.data.SearchResultDialog;
import com.ll.data.SearchResultDialog.OnListItemClick;
import com.ll.utils.AMapUtil;
import com.ll.utils.Constants;
import com.ll.utils.LogUtil;
import com.ll.utils.OffLineMapUtils;
import com.ll.utils.PoiItem;
import com.ll.utils.ToastUtil;
import com.ll.R;

/**
 * AMapV2地图中简单介绍route搜索
 */
public class RouteNaviActivity extends Activity implements LocationSource,OnInfoWindowClickListener,
    AMapLocationListener,OnClickListener,OnCameraChangeListener,OnMarkerClickListener{
    private String TAG=RouteNaviActivity.class.getName();
    private long exitTime;
    private boolean naviflag;
    private String start=null;
    private String end=null;
    private String RouteId;
    
    //地图相关
    private AMap aMap;
    private MapView mapView;
    private AMapLocationClient mLocationClient;//声明AMapLocationClient类对象
    private AMapLocationClientOption mLocationOption;
    private OnLocationChangedListener mListener;
    private UiSettings mUiSettings;
    private LatLng prevLatLng=null;
    private Marker marker=null,myLoc=null;
    private Polyline mPolyline=null;
    private LatLng curr;

    //界面控件
    private EditText mStartEditText;
    private EditText mEndEditText;
    private ImageView mStartImageView;
    private ImageView mEndImageView;
    private Button mSearchButton;
    private Button mNaviButton;

    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Thread thread=null;
    private Handler handler=new Handler(){
    	public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		if(ToastUtil.isShowing())
					ToastUtil.dismissDialog();
    		if(msg.what==0x123){
    			Map<String, String> P=(ConcurrentHashMap<String, String>)msg.obj;
    			String currt=start;
     	    	String prev=P.get(start);
     	    	while(prev!=null){
     	    		LogUtil.d(TAG, "途径节点--"+prev);
     	    		querySingle(currt, prev);
     	    		currt=prev;
     	    		prev=P.get(prev);
     	    	}
    		}
    	};
    };

/*-----------------------------生命周期函数----------------------------------*/
    @Override
    protected void onCreate(Bundle bundle) {
        LogUtil.d(TAG, "onCreate");
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
        setContentView(R.layout.activity_navi);
        mapView = (MapView) findViewById(R.id.routemap);
        mapView.onCreate(bundle);// 此方法必须重写
        dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
        initMap();
        initUI();
    }

    @Override
    protected void onResume() {
        LogUtil.d(TAG, "onResume");
        super.onResume();
        mapView.onResume();
//        printAll();
    }
    
    public void printAll(){
    	dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
        db=dbHelper.getReadableDatabase();
        Cursor nodeCursor=db.query("Node", null, "name=? or name=?", new String[]{"测试点1","测试点2"}, null, null, null);
        if(nodeCursor.moveToFirst()){
        	LogUtil.d(TAG, "有路线");
            do{
	        	Double geoLng=nodeCursor.getDouble(nodeCursor.getColumnIndex("addr_lng"));
	            Double geoLat=nodeCursor.getDouble(nodeCursor.getColumnIndex("addr_lat"));
	            LogUtil.d(TAG, "lng/lat="+geoLng+"/"+geoLat);
            }while(nodeCursor.moveToNext());
            nodeCursor.close();
        }
        if(!db.isOpen()){
        	db=dbHelper.getReadableDatabase();
        }
    	Cursor locCursor=db.query("Location", null, "route_id=?", new String[]{"1J07TGFx99"}, null, null, null);
        if(locCursor.moveToFirst()){
            do{
	        	Double geoLng=locCursor.getDouble(locCursor.getColumnIndex("longitude"));
	            Double geoLat=locCursor.getDouble(locCursor.getColumnIndex("latitude"));
	            LogUtil.d(TAG, "lng/lat="+geoLng+"/"+geoLat);
            }while(locCursor.moveToNext());
            locCursor.close();
        }
        db.close();
    }

    @Override
    protected void onPause() {
        LogUtil.d(TAG, "onPause");
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    protected void onStop() {
    	// TODO Auto-generated method stub
    	super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogUtil.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();
        aMap=null;
        mapView.onDestroy();
        deactivate();
    }
/*-----------------------------控件初始化------------------------------------*/
    /**
     * UI界面初始化
     */
    private void initUI() {
        mStartEditText=(EditText)findViewById(R.id.start_tv);
        mEndEditText=(EditText)findViewById(R.id.end_tv);
        mStartImageView=(ImageView)findViewById(R.id.start_search_iv);
        mEndImageView=(ImageView)findViewById(R.id.end_search_iv);
        mSearchButton=(Button)findViewById(R.id.route_search_btn);
        mNaviButton=(Button)findViewById(R.id.route_navi_btn);

        mStartImageView.setOnClickListener(this);//查询起点的监听器
        mEndImageView.setOnClickListener(this);//查询终点的监听器
        mSearchButton.setOnClickListener(this);//给出最短路径的监听器
        mNaviButton.setOnClickListener(this);
    }

    /**
     * 初始化AMap对象
     */
    private void initMap() {
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        setUpMap();
    }

    /**
     * 设置AMap的定位显示风格和UI相关属性
     */
    private void setUpMap() {
        LogUtil.d(TAG, "setUpMap");
        aMap.setLocationSource(this);//设置定位监听。如果不设置此定位资源则定位按钮不可点击
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);//设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);//设置定位的类型为跟随模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setOnCameraChangeListener(this);//设置可视区域改变的监听器
        aMap.setOnMarkerClickListener(this);
        aMap.setOnInfoWindowClickListener(this);
        aMap.moveCamera(CameraUpdateFactory.zoomBy(8));

        mUiSettings = aMap.getUiSettings();//返回地图的用户界面设置对象
        mUiSettings.setCompassEnabled(true);//设置指南针被显示
        mUiSettings.setScaleControlsEnabled(true);//设置比例尺被显示
    }

/*-----------------------------监听器内部类-----------------------------------*/
    /**
     * 监听Home键和back键事件，做出相关处理
     */
    @Override
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
                    ToastUtil.show(this, "再按一次退出路径规划");
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

    @Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
    	int viewId=v.getId();
    	if(viewId==R.id.start_search_iv){
    		start=mStartEditText.getText().toString().trim();
    		if(start==null||"".equals(start)){
                ToastUtil.show(RouteNaviActivity.this, "起点不能为空");
                mStartEditText.requestFocus();
    		}else{
    			searchNode(start,0);
    		}
    	}else if(viewId==R.id.end_search_iv){
    		end=mEndEditText.getText().toString().trim();
    		if(end==null||"".equals(end)){
                ToastUtil.show(RouteNaviActivity.this, "终点不能为空");
                mEndEditText.requestFocus();
            }else{
            	searchNode(end,1);
            }
    	}else if(viewId==R.id.route_search_btn){
    		if(start==null||"".equals(start)){
                ToastUtil.show(RouteNaviActivity.this, "请设置起点");
                mStartEditText.requestFocus();
            }else if(end==null||"".equals(end)){
                ToastUtil.show(RouteNaviActivity.this, "请设置终点");
                mEndEditText.requestFocus();
            }else if(start.equals(end)){
            	ToastUtil.show(RouteNaviActivity.this, "起点和终点相同，无法查询");
            	mStartEditText.requestFocus();
            }else{
            	prevLatLng=null;
            	aMap.clear();
            	addMarkers(start);
            	LogUtil.w(TAG, thread==null?"null":""+thread.getState());
                ToastUtil.showDialog(RouteNaviActivity.this,"正在查询.....");
                
                //开启后台线程去查询路线
                if(thread==null || !thread.isAlive()){
                	LogUtil.w(TAG, "new thread");
                	thread=new Thread(new Runnable() {
    					@Override
    					public void run() {
    						// TODO Auto-generated method stub
    						queryRoute(start,end);
    					}
    				});
                    thread.start();
                }
            }
    	}else if(viewId==R.id.route_navi_btn){
    		if(start==null||start.equals("")){
    			ToastUtil.show(RouteNaviActivity.this, "起点为空");
    			return;
    		}
    		if(naviflag==false){
    			if(mLocationClient==null){
	       			activate(mListener);//考虑如果mListener==null时的异常
	   	        }else if(!mLocationClient.isStarted()){
	   	        	mLocationClient.startLocation();
	   	        }
				naviflag=true;
			}
    	}
	}
    
/*-----------------------------路线查询及绘制功能函数----------------------------*/
    /**
     * 输入的地名只能是用户名或其他位置点名称
     * @param location
     */
    private void searchNode(String location,int flag ){
    	LogUtil.d(TAG, "searchNode:location="+location+";flag="+flag);
        final int iflag=flag;
        Cursor nodeCursor=null;
        List<PoiItem> userItems=new ArrayList<PoiItem>();
        try {
        	dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
            db=dbHelper.getReadableDatabase();
            //先去用户表中查询
        	nodeCursor=db.query("Users", null, "user_name like ? or user_id like ?", 
            		new String[]{"%"+location+"%","%"+location+"%"}, null, null, null);
        	if(nodeCursor.moveToFirst()){
        		while(!nodeCursor.isAfterLast()){
        			String user_id=nodeCursor.getString(nodeCursor.getColumnIndex("user_id"));
        			String user_name=nodeCursor.getString(nodeCursor.getColumnIndex("user_name"));
        			String user_addr=nodeCursor.getString(nodeCursor.getColumnIndex("user_addr"));
        			if(!db.isOpen()){
        	        	db=dbHelper.getReadableDatabase();
        	        }
        			Cursor cursor=db.query("Node", null, "name = ?",new String[]{user_id}, null, null, null);
        			if(cursor.moveToFirst()){
        				 userItems.add(new PoiItem(user_name, user_id,user_addr));
        				 cursor.close();
        			}
        			nodeCursor.moveToNext();
        		}
        	}
        	nodeCursor.close();
        	//再去Node表中查询
        	if(!db.isOpen()){
            	db=dbHelper.getReadableDatabase();
            }
    		nodeCursor=db.query("Node", null, "name like ?", 
            		new String[]{"%"+location+"%"}, null, null, null);
    		if(nodeCursor.moveToFirst()){
    			while(!nodeCursor.isAfterLast()){
        			int id=nodeCursor.getInt(nodeCursor.getColumnIndex("_id"));
        			String name=nodeCursor.getString(nodeCursor.getColumnIndex("name"));
        			String district_number=nodeCursor.getString(nodeCursor.getColumnIndex("district_number"));
        			userItems.add(new PoiItem(String.valueOf(id),name,  district_number));
        			nodeCursor.moveToNext();
        		}
    		}else{
    			ToastUtil.show(RouteNaviActivity.this, "找不到该点，请重新输入");
    		}
        	
        	nodeCursor.close();
        	if(!userItems.isEmpty()){
        		SearchResultDialog dialog=new SearchResultDialog(RouteNaviActivity.this,userItems);
 		   		dialog.setTitle("您要找的位置点是：");
 		   	    dialog.show();
 		    	dialog.setOnListClickListener(new OnListItemClick() {
 					@Override
 					public void onListItemClick(SearchResultDialog dialog, 
 							PoiItem item) {
 						// TODO Auto-generated method stub
 						if(marker!=null){
 							marker.remove();
 							marker=null;
 						}
 						aMap.clear();
 						naviflag=false;
 						if(iflag==0){
 							mStartEditText.setText(item.getName()+"----"+item.getId());
 							start=item.getId();
 							addMarkers(start);
 						}else if(iflag==1){
 							mEndEditText.setText(item.getName()+"----"+item.getId());
 							end=item.getId();
 							addMarkers(end);
 						}
 						
 					}
 		        });
            }else{
            	ToastUtil.show(RouteNaviActivity.this, "查不到该点，请重新输入");
            }
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			ToastUtil.show(this, "查询失败。请输入更详细信息查询。");
		} finally{
			if(nodeCursor!=null&&!nodeCursor.isClosed())
				nodeCursor.close();
			if(db!=null&&db.isOpen())
				db.close();
		}
        
    }
    
    /**
     * 由用户输入的有效起点终点查询两点间最短路径
     */
    private void queryRoute(String start,String end){
    	LogUtil.d(TAG, "queryRoute   start="+start+"  end="+end);
    	dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
        db=dbHelper.getReadableDatabase();
        Cursor nodeCursor=null,routeCursor=null;
        
        final float INFINITY=Float.MAX_VALUE; //用于当前点最短路径长度初始化
        Map<String,Float> D=new ConcurrentHashMap<String, Float>();  //用于记录源点到点i(第一个参数)的当前最短路径(第二个参数)
        Map<String, String> P=new ConcurrentHashMap<String, String>();//记录路径，第一个参数是当前节点，第二个参数是当前节点的前驱节点
        Queue<String> Q=new LinkedList<String>();  //队列，当点可以进行松弛操作且为入队，则可以让点入队
        Map<String, Boolean> IsVisited=new HashMap<String, Boolean>(); //记录当前节点(第一个参数)是否已经入队(第二个参数)
        
        //获取起点和终点的台区编码
        String sDistrict=null,eDistrict=null;
        try {
        	 nodeCursor=db.query("Node", null, "name= ? or name=?",
             		new String[]{start,end}, null, null, null);
             if(nodeCursor.getCount()<2){
             	LogUtil.d(TAG,"Node表中查不到起点或终点");
             	 runOnUiThread(new Runnable() {
     					@Override
     					public void run() {
     						// TODO Auto-generated method stub
     						if(ToastUtil.isShowing())
     							ToastUtil.dismissDialog();
     						ToastUtil.show(RouteNaviActivity.this, "路线不存在");
     					}
     				});
             	return;
             }else{
             	nodeCursor.moveToFirst();
             	do{
             		Log.d(TAG, nodeCursor.getString(nodeCursor.getColumnIndex("name")));
             		//切记不可用==判断，必须用equals方法
             		if(nodeCursor.getString(nodeCursor.getColumnIndex("name")).equals(start)){
             			sDistrict=nodeCursor.getString(
             					nodeCursor.getColumnIndex("district_number"));
             			LogUtil.d(TAG, "sDistrict"+sDistrict);
             		}else{
             			eDistrict=nodeCursor.getString(
             					nodeCursor.getColumnIndex("district_number"));
             			LogUtil.d(TAG, "eDistrict"+eDistrict);
             		}
             	}while(nodeCursor.moveToNext());
             }
             nodeCursor.close();
             if(sDistrict!=null&&eDistrict!=null){
             	 if(sDistrict.equals(eDistrict)){
                 	 //如果起点和终点在同一个台区内，查询出同一个台区所有节点
             		 LogUtil.i(TAG, "起点和终点在同一台区");
             		if(!db.isOpen()){
                    	db=dbHelper.getReadableDatabase();
                    } 
                 	nodeCursor=db.query("Node", null, "district_number= ?",
                 			 new String[]{sDistrict}, null, null, null);
                      
                 }else{
                	//否则，查询出两个台区中所有节点
                 	LogUtil.i(TAG, "起点和终点在不同台区");
                 	if(!db.isOpen()){
                    	db=dbHelper.getReadableDatabase();
                    } 
                 	nodeCursor=db.query("Node", null, "district_number= ? or district_number= ? ",
                 			 new String[]{sDistrict,eDistrict}, null, null, null);
                 }
                 //将游标移至第一条记录
                 nodeCursor.moveToFirst(); 
                 //遍历所有节点，进行初始化;
                 //因为算法记录中会记录途经点的前驱节点，而最后画图需要从查询的起点画到终点，所以把要查询的终点设置为源点
         	    while(!nodeCursor.isAfterLast()){
         	    	//返回的可能是用户Id或其他位置点名称
         	    	String name=nodeCursor.getString(nodeCursor.getColumnIndex("name"));
         	    	//切记不可用name==end来判断，否则始终会判定为name!=end
         	   	 	if(name.equals(end)){
         	   	 		D.put(end, 0.0f);//源点当前最短路径为0.0f
         	   	 		Q.offer(end); //源点入队
         	   	 		LogUtil.d(TAG, end+"入队");
         	   	 		IsVisited.put(end, true); //入队点记录为已入队，防止重复入队
         	   	 	}else {
         	   	 		D.put(name, INFINITY); //其他点当前最短路径初始化为无穷大
         	   	 		IsVisited.put(name, false);//其他点未入队，记录为false
         	   	 	}
         	   	 	nodeCursor.moveToNext();
         	    }
         	    nodeCursor.close();
         	    //当队列不为空时，弹出队首节点去扫描并对其邻接点做松弛操作
         	    while(!Q.isEmpty()){
         	    	LogUtil.d(TAG, "!Q.isEmpty()");
         	    	String u=Q.poll();  //用u记录队首弹出的点
         	    	LogUtil.d(TAG, "Q出队"+u);
         	    	IsVisited.put(u, false);
         	    	if(!db.isOpen()){
         	        	db=dbHelper.getReadableDatabase();
         	        } 
         	   	 	routeCursor=db.query("Route", null, "start = ? or end = ?",new String[]{u,u}, null, null, null);
	         	   	if(!db.isOpen()){
	     	        	db=dbHelper.getReadableDatabase();
	     	        } 
         	   	 	if(routeCursor.moveToFirst()){
         	   	 		//遍历每一个与u邻接的点
         	   	 		do{
	         	   	 		String sPoint=routeCursor.getString(routeCursor.getColumnIndex("start"));
	         	   	 		String ePoint=routeCursor.getString(routeCursor.getColumnIndex("end"));
	         	   	 		float distance=routeCursor.getFloat(routeCursor.getColumnIndex("distance"));
	         	   	 		LogUtil.d(TAG, "开始遍历点"+u+"的邻接点：start="+start+";end="+end);
	         	   	 		//默认sPoint就是u，ePoint为u的邻接点；当u是记录中的终点时进行交换
	         	   	 		if(!u.equals(sPoint)){
	         	   	 			String temp=sPoint;
	         	   	 			sPoint=ePoint;
	         	   	 			ePoint=temp;
	         	   	 		}
	         	   	 		if(D.containsKey(sPoint)&&D.containsKey(ePoint)){
	         	   	 			if(D.get(sPoint)+distance<D.get(ePoint)){
	         	   	 				D.put(ePoint, D.get(sPoint)+distance); //更新该邻接点当前最短路劲值
	         	   	 				P.put(ePoint, sPoint);//更新该邻接点的前驱
	         	   	 				Log.d(TAG, ePoint+"的前驱是"+sPoint);
	         	   	 				if(!IsVisited.get(ePoint)){
		         	   	 				Q.offer(ePoint);//将该点入队
		     	    	   	 			LogUtil.d(TAG, "Q入队"+ePoint);
		     	    	   	 			IsVisited.put(ePoint, true);//修改入队标记
	         	   	 				}
	         	   	 			}
	         	   	 		}
	         	   	 		//松弛操作：如果该邻接点使得源点到该点最短路径值可更新且该点不在队列中，则更新该点并将该点入队
         	   	 		}while(routeCursor.moveToNext());
         	   	 	};
         	   	 	routeCursor.close();
         	    }
         	   
         	    if(P.get(start)!=null){
         	    	LogUtil.i(TAG, "已经找到路线"+start);
         	    	try {
                    	Message msg= Message.obtain(handler);
                        msg.what=0x123;
                        msg.obj=P;
						Thread.sleep(20);
						msg.sendToTarget();
					} catch (InterruptedException e) {
						// TODO: handle exception
						e.printStackTrace();
					}
         	    }else{
         	    	runOnUiThread(new Runnable() {
         					@Override
         					public void run() {
         						// TODO Auto-generated method stub
         						if(ToastUtil.isShowing())
         							ToastUtil.dismissDialog();
         						ToastUtil.show(RouteNaviActivity.this, "终点不可达");
         					}
         				});
         	    	LogUtil.i(TAG, "终点不可到达");
         	    	return;
         	    }
             }
		} catch (Exception e) {
			e.printStackTrace();
			runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						if(ToastUtil.isShowing())
							ToastUtil.dismissDialog();
						ToastUtil.show(RouteNaviActivity.this, "最短路径查询异常");
					}
				});
			
		} finally{
			if(routeCursor!=null&&!routeCursor.isClosed())
				routeCursor.close();
			if(nodeCursor!=null&&!nodeCursor.isClosed())
				nodeCursor.close();
			if(db!=null&&db.isOpen())
				db.close();
        }
    }
    
    /**
     * 查询原始route表中单段路线
     * @param start 
     * @param end
     * @return 查询成功返回true
     */
    private boolean querySingle(String start,String end){
        LogUtil.d(TAG, "querySingle");
        Cursor routeCursor=null;
        try {
        	dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
            db=dbHelper.getReadableDatabase();
            String selection="start = ? and  end = ?";
            String[] selectionArgs1=new String[]{start,end};
            String[] selectionArgs2=new String[]{end,start};
        	routeCursor=db.query("Route", new String[]{"route_id"}, selection, selectionArgs1, null, null, null);
            if(routeCursor.moveToFirst()){
            	LogUtil.d(TAG, "单段顺序查找");
                do{
                    RouteId=routeCursor.getString(routeCursor.getColumnIndex("route_id"));
                    Cursor locCursor=db.query("Location", null, "route_id = ?", new String[]{RouteId}, null, null, null);
                	if(!db.isOpen()){
	     	        	db=dbHelper.getReadableDatabase();
	     	        } 
                    if(locCursor.moveToFirst()){
                        do{
                        	Double geoLng=locCursor.getDouble(locCursor.getColumnIndex("longitude"));
                            Double geoLat=locCursor.getDouble(locCursor.getColumnIndex("latitude"));
                            LatLng pointLatLng=new LatLng(geoLat, geoLng);
                            drawingPath(pointLatLng);
                        }while(locCursor.moveToNext());
                    }
                    locCursor.close();
                }while (routeCursor.moveToNext());
                routeCursor.close();
                db.close();
                addMarkers(end);
                return true;
            }else{
            	LogUtil.d(TAG, "单段逆序查找");
            	if(!db.isOpen()){
                	db=dbHelper.getReadableDatabase();
                }
            	routeCursor=db.query("Route", new String[]{"route_id"}, selection, selectionArgs2, null, null, null);
            	if(!db.isOpen()){
     	        	db=dbHelper.getReadableDatabase();
     	        } 
            	if(routeCursor.moveToFirst()){
                    do{
                        RouteId=routeCursor.getString(routeCursor.getColumnIndex("route_id"));
                        if(!db.isOpen()){
                        	db=dbHelper.getReadableDatabase();
                        }
                        Cursor locCursor=db.query("Location", null, "route_id = ?", new String[]{RouteId}, null, null, null);
                        if(locCursor.moveToLast()){
                            do{
                            	Double geoLng=locCursor.getDouble(locCursor.getColumnIndex("longitude"));
                                Double geoLat=locCursor.getDouble(locCursor.getColumnIndex("latitude"));
                                LatLng pointLatLng=new LatLng(geoLat, geoLng);
                                drawingPath(pointLatLng);
                            }while(locCursor.moveToPrevious());
                        }
                        locCursor.close();
                    }while (routeCursor.moveToNext());
                    routeCursor.close();
                    db.close();
                    addMarkers(end);
                    return true;
                }else{
                	return false;
                }        	
            }
		} catch (Exception e) {
			LogUtil.d(TAG, "single异常");
			e.printStackTrace();
			return false;
		} finally{
			if(routeCursor!=null&&!routeCursor.isClosed())
				routeCursor.close();
			if(db!=null&&db.isOpen())
				db.close();
		}
    }
    
    private void drawDirection(LatLng mLatLng){
    	Cursor startCursor=null;
    	LogUtil.d(TAG, "start="+start);
		try {
    		Double start_lng=0.0,start_lat=0.0;
    		dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
            db=dbHelper.getReadableDatabase();
    		startCursor=db.query("Node", null, "name = ?", new String[]{start}, null, null, null);
    		if(startCursor.moveToFirst()){
    			start_lng=startCursor.getDouble(startCursor.getColumnIndex("addr_lng"));
    	    	start_lat=startCursor.getDouble(startCursor.getColumnIndex("addr_lat"));
    	    	if(start_lng!=0.0 && start_lat!=0.0){
    	    		if(mPolyline!=null){
    	    			mPolyline.remove();
    	    			mPolyline=null;
                	}
	    	    	mPolyline=aMap.addPolyline((new PolylineOptions())
				             .add(mLatLng,new LatLng(start_lat, start_lng)) //追加两个顶点到线段终点
				             .geodesic(true)       //设置线段为大地曲线
				             .setDottedLine(true)
		                     .color(Color.CYAN)); //设置线段颜色
	    	    	LogUtil.d(TAG, "start lng/lat="+start_lng+"/"+start_lat+";curr lng/lat="+mLatLng.toString());
    	    	}
    		}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally{
			if(startCursor!=null&&!startCursor.isClosed())
				startCursor.close();
        	if(db!=null&&db.isOpen())
        		db.close();
		}
    }
    
    /**
     * 两点之间画路线
     * @param currLatLng 当前点
     */
    private void drawingPath(LatLng currLatLng){
    	LogUtil.d(TAG, "drawingPath:prev="+(prevLatLng==null?"null":prevLatLng.longitude+"/"+prevLatLng.latitude)
         		+";curr="+currLatLng.longitude+"/"+currLatLng.latitude);
        if(prevLatLng==null){
            prevLatLng=currLatLng;
        }else {
            aMap.addPolyline((new PolylineOptions())
                    .add(prevLatLng,currLatLng) //追加两个顶点到线段终点
                    .geodesic(true)       //设置线段为大地曲线
                    .color(Color.GREEN)); //设置线段颜色
            prevLatLng=currLatLng;
        }
    }
    
    private void addMarkers(String nodeName){
    	Cursor nodeCursor=null,userCursor=null;
        try {
        	dbHelper=new MyDatabaseHelper(this, "Locations.db", null, 2);
            db=dbHelper.getReadableDatabase();
            nodeCursor=db.query("Node", null, "name = ?", new String[]{nodeName},null, null, null);
            if(nodeCursor.moveToFirst()){
           	 	String title=nodeName;
            	String snippet=nodeCursor.getString(nodeCursor.getColumnIndex("district_number"));
            	Double addr_lng=nodeCursor.getDouble(nodeCursor.getColumnIndex("addr_lng"));
                Double addr_lat=nodeCursor.getDouble(nodeCursor.getColumnIndex("addr_lat"));
                LatLng latLng=new LatLng(addr_lat, addr_lng);
                //如果是用户ID，要进一步查出用户名，显示用户名和用户号；否则显示其他位置点名称和位置点所在区号
                if(!db.isOpen()){
                	db=dbHelper.getReadableDatabase();
                }
        		userCursor=db.query("Users", null, "user_id=?", new String[]{nodeName},null, null, null);
        		if(userCursor.moveToFirst()){
        			title=userCursor.getString(userCursor.getColumnIndex("user_name"));
        			snippet=nodeName;
        		}
        		int markerIcon=0;
            	if(nodeName.equals(start)){
    	       		markerIcon=R.drawable.start;
    	       		aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
    						latLng, 18, 0, 0)));
    	       	    LogUtil.d(TAG, "该点为起点");
    	       	}else if(nodeName.equals(end)){
    	       		markerIcon=R.drawable.end;
    	       		aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(
    						latLng, 18, 0, 0)));
    	       		LogUtil.d(TAG, "该点为终点");
    	       	}else{
    	       		markerIcon=R.drawable.point;
    	       		LogUtil.d(TAG, "该点为途径点");
    	       	}
    			marker=aMap.addMarker(new MarkerOptions()
						        .anchor(0.5f, 0.5f)
						        .icon(BitmapDescriptorFactory.fromResource(markerIcon))
						        .title(title)
						        .snippet(snippet)
						        .position(latLng)   
						        .setFlat(true));
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(userCursor!=null&&!userCursor.isClosed())
				userCursor.close();
			if(nodeCursor!=null&&!nodeCursor.isClosed())
				nodeCursor.close();
			if(db!=null&&db.isOpen())
				db.close();
		}
    }
/*-------------------------------定位相关------------------------------------*/
    /**
     * 启动定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
    	LogUtil.d(TAG, "activate"+getIntent().getIntExtra("loc_interval", Constants.TWO_SEC));
    	mListener = listener;
        if (mLocationClient == null) {
        	mLocationClient=new AMapLocationClient(this);//初始化定位客户端对象
            mLocationOption=new AMapLocationClientOption();//初始化定位参数
            mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);//设置定位模式为高精度模式，同时使用网络和GPS定位，优先返回最高精度定位结果
            mLocationOption.setOnceLocation(false);//设置是否只定位一次。false表示持续定位
            mLocationOption.setInterval(getIntent().getIntExtra("loc_interval", Constants.TWO_SEC)*1000);//设置定位间隔，单位毫秒。默认为2000ms
            mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
            mLocationClient.setLocationListener(this);//设置定位监听
        }
        mLocationClient.startLocation();//启动定位
        if(!ToastUtil.isShowing()){
            ToastUtil.showDialog(this,"正在定位中...");
        }
        handler.postDelayed(mRunable, 40000);
    }
    
    
	Runnable mRunable=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(mLocationClient!=null){
				 if(ToastUtil.isShowing()){
			            ToastUtil.dismissDialog();
			        }
				 LogUtil.d(TAG, "定位失败，请检查GPS");
				 ToastUtil.show(RouteNaviActivity.this, "定位失败，请检查GPS");
			}
		}
	};

    /**
     * 停止定位
     */
	@Override
    public void deactivate() {
        LogUtil.d(TAG, "deactivate()");
        mListener=null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    public void onLocationChanged(AMapLocation arg0) {
        // TODO Auto-generated method stub
        if (arg0 != null && arg0.getErrorCode() == 0){
        	if(ToastUtil.isShowing()){
                ToastUtil.dismissDialog();
            }
        	curr=AMapUtil.convertToLatLng(arg0);
        	//实现自定义的定位小蓝点。
        	if(myLoc!=null){
        		myLoc.remove();
        		myLoc=null;
        	}
    		myLoc = aMap.addMarker(new MarkerOptions()
				            .anchor(0.5f, 0.5f)
				            .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker))
				            .position(curr)
				            .visible(true));
    		float bearing=360-arg0.getBearing();
			myLoc.setRotateAngle(bearing);
			myLoc.setToTop();
			if(naviflag==true){
        		aMap.moveCamera(CameraUpdateFactory.changeLatLng(curr));
    			drawDirection(curr);
        	}
        } else {
            LogUtil.d(TAG,"Location ERR:" + arg0.getErrorCode());
            ToastUtil.show(this,"定位异常，请检查网络和GPS.");
        }
    }

	@Override
	public void onCameraChange(CameraPosition arg0) {
	}

	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		if(!arg0.isInfoWindowShown()){
			arg0.showInfoWindow();
		}
		return false;
	}

	@Override
	public void onInfoWindowClick(Marker arg0) {
		// TODO Auto-generated method stub
		arg0.hideInfoWindow();
	}

}