package com.ll.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import com.amap.api.maps.model.LatLng;
import com.ll.Zxing.CaptureActivity;
import com.ll.data.SearchResultDialog.OnListItemClick;
import com.ll.utils.AMapUtil;
import com.ll.utils.LogUtil;
import com.ll.utils.PoiItem;
import com.ll.utils.ToastUtil;
import com.ll.R;

/**
 * 采集路径过程中位置点信息的采集页面，分为用户点和其他点两种
 * @author Administrator
 */
public class TabhostActivity extends Activity implements OnClickListener,
        OnItemSelectedListener{
    private String TAG=TabhostActivity.class.getName();
    String point; //用于记录当前显示出来的经纬度点
    double[] lnglat=new double[2];
    String disNum,disName; //用于传递台区编码和台区名称
    Intent resultIntent;
    private List<String> disList=new ArrayList<String>();
	private int searchFlag=0; //用于标记搜索方式，0--输入用户ID，1--扫描用户ID，2--输入用户名，3--扫描表计条码

    //界面空间相关
    private TabHost mTabHost;
    //第一个tab页，以用户位置为采集点
    private TextView mUserIdTextView;
    private TextView mUserNameTextView;
    private TextView mUnitTextView;
    private TextView mPowerUnitTextView;
    private TextView mDistrictNumTextView;
    private TextView mDistrictNameTextView;
    private TextView mTerminalNumTextView;
    private TextView mMeterNumTextView;
    private TextView mLogicalAddrTextView;
    private TextView mCollectUnitTextView;
    private TextView mCoordinateTextView;
    private EditText mUserAddrEditText;
    private EditText mRemarksEditText;
    private Button mSearchUserButton;
    private Button mRelocateButton;
    private Button mSaveUserButton;
    private Button mCancelUserButton;
    private TextView mScanResultTextView;
    
    //第二个tab页，以其他位置为采集点
    private EditText mLocDescEditText;
    private TextView mCoordinateTextView2;
    private Spinner mDistrictSpinner;
    private Button mLocateButton;
    private Button mSaveLocButton;
    private Button mCancelLocButton;
    
    //数据库操作相关
    private SQLiteDatabase db;
    private MyDatabaseHelper dbHelper;
    private Cursor cursor=null;
    private ContentValues values=new ContentValues();;
/*-------------------------------生命周期函数----------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add_point);

        dbHelper=new MyDatabaseHelper(this,  "Locations.db", null, 2);
        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onOptionsItemSelected");
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
		mTabHost.getTabWidget().getChildAt(0).setBackgroundColor(Color.parseColor("#B0E2FF"));
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();
    }
/*-------------------------------初始化操作------------------------------------*/
    /**
     * 界面布局及spinner数据初始化
     */
    private void initViews() {
        LogUtil.d(TAG, "initViews");
        mTabHost=(TabHost)findViewById(R.id.tabhost);
        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec("tab01")
        		.setIndicator(composeLayout("用户点",0))
        		.setContent(R.id.tab01));
        mTabHost.addTab(mTabHost.newTabSpec("tab02")
        		.setIndicator(composeLayout("其他点",0))
        		.setContent(R.id.tab02));
        mTabHost.setCurrentTab(0);
        
        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				// TODO Auto-generated method stub
				LogUtil.d(TAG, "onTabChanged");
				for(int i=0;i<mTabHost.getTabWidget().getChildCount();i++){
					mTabHost.getTabWidget().getChildAt(i).setBackgroundDrawable(null);
				}
				mTabHost.getTabWidget().getChildAt(mTabHost.getCurrentTab()).setBackgroundColor(Color.parseColor("#B0E2FF"));
			}
		});
        //以用户位置为点的tab页
        mUserIdTextView=(TextView)findViewById(R.id.user_id_et);
        mUserNameTextView=(TextView)findViewById(R.id.user_name_et);
        mUnitTextView=(TextView)findViewById(R.id.unit_et);
        mPowerUnitTextView=(TextView)findViewById(R.id.power_unit_et);
        mDistrictNumTextView=(TextView)findViewById(R.id.district_num_et);
        mDistrictNameTextView=(TextView)findViewById(R.id.district_name_et);
        mTerminalNumTextView=(TextView)findViewById(R.id.terminal_num_et);
        mMeterNumTextView=(TextView)findViewById(R.id.meter_num_et);
        mLogicalAddrTextView=(TextView)findViewById(R.id.logical_addr_et);
        mCollectUnitTextView=(TextView)findViewById(R.id.collection_unit_et);
        mCoordinateTextView=(TextView)findViewById(R.id.coordinate_tv);
        mUserAddrEditText=(EditText)findViewById(R.id.user_addr_et);
        mRemarksEditText=(EditText)findViewById(R.id.remarks_et);
        mSearchUserButton=(Button)findViewById(R.id.search_user_btn);
        mRelocateButton=(Button)findViewById(R.id.relocate_btn);
        mSaveUserButton=(Button)findViewById(R.id.save_btn);
        mCancelUserButton=(Button)findViewById(R.id.cancel_btn);
        mScanResultTextView=(TextView)findViewById(R.id.scan2_et);
        
        mSearchUserButton.setOnClickListener(this);
        mRelocateButton.setOnClickListener(this);
        mSaveUserButton.setOnClickListener(this);
        mCancelUserButton.setOnClickListener(this);
        
        //以其他位置为点的tab页
        mLocDescEditText=(EditText)findViewById(R.id.loc_desc_et);
        mCoordinateTextView2=(TextView)findViewById(R.id.loc_coordinate_tv);
        mDistrictSpinner=(Spinner)findViewById(R.id.spinner_district);
        mLocateButton=(Button)findViewById(R.id.locate_btn);
        mSaveLocButton=(Button)findViewById(R.id.save2_btn);
        mCancelLocButton=(Button)findViewById(R.id.cancel2_btn);
        
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, queryDistrict());//查询出所有台区信息并绑定在spinner的adapter中
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDistrictSpinner.setAdapter(adapter);
		mDistrictSpinner.setOnItemSelectedListener(this);
		
        mLocateButton.setOnClickListener(this);
        mSaveLocButton.setOnClickListener(this);
        mCancelLocButton.setOnClickListener(this);
    }

    /**
     * tabHost布局设置
     */
    public View composeLayout(String s, int i) {
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		ImageView iv = new ImageView(this);
		iv.setImageResource(i);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.setMargins(0, 5, 0, 0);
		layout.addView(iv, lp);
		TextView tv = new TextView(this);
		tv.setGravity(Gravity.CENTER);
		tv.setSingleLine(true);
		tv.setText(s);
		tv.setTextColor(Color.parseColor("#0000FF"));
		tv.setTextSize(20);
		layout.addView(tv, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT));
		return layout;
	}
/*--------------------------------监听函数------------------------------------*/
    /**
     * 所属台区的spinner条目被点击时，获取所点击的台区信息中的台区编码
     */
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub		
		String[] disInfo=disList.get(position).split(" ");
		disNum=disInfo[0];
		disName=disInfo[1];
		disInfo=null;
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub
	}
    
	/**
	 * 当用户按下Home键和back键时的处理
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
	        	quitDirectly();
                return true;
            }
        }
        return super.onKeyDown(keyCode,event);
    }
    
	/**
	 * 界面布局中按钮被按下时的操作
	 */
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int viewId=v.getId();
        switch (viewId) {
            case R.id.search_user_btn:
            	showSearchDialog();
                break;
            case R.id.relocate_btn:
            	getLocation();
                break;
            case R.id.save_btn:
            	saveUserData(mUserIdTextView.getText().toString().trim());
                break;
            case R.id.cancel_btn:
                quitDirectly();
                break;
            case R.id.locate_btn:
                getLocation();
                break;
            case R.id.save2_btn:
            	saveOtherLoc(mLocDescEditText.getText().toString().trim());
                break;
            case R.id.cancel2_btn:
            	quitDirectly();
                break;
            default:
                break;
        }
    }
/*-------------------------------通过条形码扫描查询-------------------------------*/   
    /**
     * 开启扫描条码页面
     * @param flag flag=1 扫描用户ID,flag=3 扫描终端局号或表计局号
     */
    private void goToScan(int flag){
    	LogUtil.d(TAG, "goToScan");
    	//实例化intent
        Intent intent = new Intent();
        //设置跳转 的界面
        intent.setClass(TabhostActivity.this, CaptureActivity.class);
        //启动Activity
        startActivityForResult(intent,flag);
    }
    
    /**
     * 对CaptureActivity返回的扫描结果做处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    	if (resultCode==1) {
    		Bundle bundle=data.getExtras();
   		 	String scan_result=bundle.getString("scan_result").trim();
   		 	LogUtil.i(TAG, "scan_result="+scan_result+", requestCode="+requestCode);
   		 	if( scan_result!=null && !"".equals(scan_result)){
   		 		//requestCode为1表示返回的scan_result是用户ID,为3表示返回的是表计局号
   		 		queryData(scan_result, requestCode);
   		 		searchFlag=0;
   		 	}
    	}
    }
/*------------------------------显示多种搜索方式的对话框----------------------------*/	
	/**
	 * 用一个对话框显示搜索方式，让用户选择
	 */
	private void showSearchDialog(){
    	View searchView=LayoutInflater.from(TabhostActivity.this)
				.inflate(R.layout.dialog_search, null);
		final EditText mSearchEditText=(EditText)searchView.findViewById(R.id.search_et);
		RadioGroup radioGroup=(RadioGroup)searchView.findViewById(R.id.search_rgroup);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				mSearchEditText.setText("");
				
				switch (checkedId) {
				case R.id.input_user_id_rb:
					searchFlag=0;
					mSearchEditText.requestFocus();
					break;
				case R.id.scan_user_id_rb:
					searchFlag=1;
					break;
				case R.id.input_user_name_rb:
					searchFlag=2;
					mSearchEditText.requestFocus();
					break;
				case R.id.scan_terminal_num_rb:
					searchFlag=3;
					break;
				case R.id.input_terminal_num_rb:
					searchFlag=4;
					mSearchEditText.requestFocus();
					break;
				case R.id.input_user_addr_rb:
					searchFlag=5;
					mSearchEditText.requestFocus();
					break;
				default:
					searchFlag=0;
					break;
				}
			}
		});
		
		AlertDialog.Builder builder=new AlertDialog.Builder(TabhostActivity.this);
		builder.setTitle("搜索用户条目");
		builder.setView(searchView);
		builder.setPositiveButton("开始搜索", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				//获取用户输入内容
				String et_temp=mSearchEditText.getText().toString().trim();
				if(searchFlag==0 || searchFlag==2 || searchFlag==4 || searchFlag==5){
					if(!"".equals(et_temp)&& et_temp!=null){
						LogUtil.d(TAG, "mSearchEditText="+et_temp+";searchFlag="+searchFlag);
						closeDialog(true, dialog);//允许关闭对话框
						queryData(et_temp,searchFlag);
						searchFlag=0;
					}else{
						ToastUtil.show(TabhostActivity.this, "请输入搜索内容");
						closeDialog(false, dialog);//不允许关闭对话框
					}
				}else{
					closeDialog(true, dialog);
					goToScan(searchFlag);
				}
			}
		});
		builder.setNegativeButton("取消搜索", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				closeDialog(true, dialog);
			}
		});
		builder.create().show();
	}
	
	 /**
     * 根据close参数决定时候关闭指定的dialog
     */
	private void closeDialog(boolean close,DialogInterface dialog){
		try{
			Field field=dialog.getClass().getSuperclass().
					getDeclaredField("mShowing");//用到了反射.................................
			field.setAccessible(true);
			field.set(dialog, close);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
/*---------------------------获取位置点信息并返回数据给路径采集页面----------------------*/
    /**
     * 对用户经纬度和位置描述做修改，保存修改并传回用户ID,所属台区和经纬度
     */
    private void saveUserData(String user_id){
        point=mCoordinateTextView.getText().toString();
        try{
            String[] coordinate=point.split(",");
            lnglat[0]=Double.parseDouble(coordinate[0].toString().trim());
            lnglat[1]=Double.parseDouble(coordinate[1].toString().trim());
        }catch(Exception e){
            e.printStackTrace();
        }
        if(lnglat[0]==0.0||lnglat[1]==0.0){
        	ToastUtil.show(this, "坐标不合理，请重新定位");
        }else if(null==user_id || ("".equals(user_id))) {
            LogUtil.i(TAG, "用户ID为空");
            ToastUtil.show(this, "请输入户号ID");
        }else{
            db=dbHelper.getWritableDatabase();
            cursor=db.query("Users", null,"user_id = ?", new String[]{user_id}, null, null, null);
            //只能更新已有用户的数据，不能新增用户
            if(!cursor.moveToFirst()){
                ToastUtil.show(this, "不存在该用户,请重新输入");
                LogUtil.i(TAG, "不存在该用户");
                cursor.close();
                db.close();
            }else{
            	String disNum=cursor.getString(cursor.getColumnIndex("district_number"));
            	String disName=cursor.getString(cursor.getColumnIndex("district_name"));
            	String user_name=cursor.getString(cursor.getColumnIndex("user_name"));
                String user_addr=mUserAddrEditText.getText().toString().trim();
                String remarks=mRemarksEditText.getText().toString();
                values.put("addr_lng", lnglat[0]);
                values.put("addr_lat", lnglat[1]);
                values.put("user_addr", user_addr);
                values.put("remarks", remarks);
                values.put("record_date", AMapUtil.getSystemTime());
                values.put("is_new", true);
                LogUtil.i(TAG, "保存用户修改:user_id="+user_id+";lng/lat="+lnglat[0]+"/"+lnglat[1]+";user_addr="+user_addr+";remarks="+remarks);
                db.update("Users", values, "user_id = ?", new String[]{user_id});
                values.clear();
                cursor.close();
                db.close();
                ToastUtil.show(this, "数据保存成功");
                backToCollecting(user_id,user_name, disNum,disName, lnglat);
            }
        }
    }
    
    /**
     * 在除用户点以外的其他位置取点，只有当位置名称和所属台区不为空且坐标合理时才回传数据到路线采集页面
     */
    private void saveOtherLoc(String loc){
    	LogUtil.d(TAG, "saveOtherLoc");
    	final String locName=loc;
        point=mCoordinateTextView2.getText().toString();
        String[] coordinate=point.split(",");
        if(coordinate.length<2){
        	lnglat[0]=0.0;
        	lnglat[1]=0.0;
        }else{
        	coordinate[0]=coordinate[0].equals("")?"0":coordinate[0];
            coordinate[1]=coordinate[1].equals("")?"0":coordinate[1];
            lnglat[0]=Double.parseDouble(coordinate[0]);
            lnglat[1]=Double.parseDouble(coordinate[1]);
        }
        
        
        if(null==loc || ("".equals(loc))){
        	ToastUtil.show(this, "位置名称不能为空");
        }else if(lnglat[0]==0.0 || lnglat[1]==0.0){
	    	ToastUtil.show(this, "坐标不合理，请重新定位");   
        }else if(disNum==null || "".equals(disNum)){
        	ToastUtil.show(this, "台区不能为空，请选择所属台区"); 	
        }else{
        	
        	try {
        		db=dbHelper.getWritableDatabase();
            	cursor=db.query("Node", null, "name = ? and district_number = ?", new String[]{locName,disNum} ,null, null, null);
            	LogUtil.d(TAG, locName+"/"+disNum+"/"+cursor.getCount());
				if(cursor.moveToFirst()){
					View alertView=LayoutInflater.from(TabhostActivity.this)
							.inflate(R.layout.dialog_alert, null);
					TextView mAlerTextView=(TextView)alertView.findViewById(R.id.alert_tv);
					AlertDialog.Builder builder=new AlertDialog.Builder(TabhostActivity.this);
					builder.setTitle("重复点警告");
					builder.setView(alertView);
					mAlerTextView.setText(disName+"已经存在名称为"+loc+"的位置点 ，继续选择该点或者给新点重新命名。");
					builder.setPositiveButton("重新命名", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							closeDialog(true, dialog);//允许关闭对话框
						}
					});
					builder.setNegativeButton("继续取点", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							closeDialog(true, dialog);
							backToCollecting(locName,locName, disNum,disName,lnglat);
						}
					});
					LogUtil.d(TAG, "builder");
					builder.create().show();
				}else{
					backToCollecting(locName,locName, disNum,disName, lnglat);
				}
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			} finally{
				if(cursor!=null&&!cursor.isClosed())
					cursor.close();
				if(db!=null&&db.isOpen())
					db.close();
			}
        }
    }
    
    /**
     * 放弃取点直接退回路径采集页面
     */
    private void quitDirectly(){
    	LogUtil.d(TAG, "quitDirectly");
    	backToCollecting("","","","",lnglat);
    }
    
    /**
     * 返回路径采集页面同时返回所采集途经点的信息
     */
    public void backToCollecting(String locId,String locName,String disNum,String disName,double[] lnglat){
    	LogUtil.i(TAG, "backToCollecting:"+locName+"/"+disName+"/");
   	 	Intent resultIntent=getIntent();
	   	resultIntent.putExtra("locId", locId);
	    resultIntent.putExtra("locName", locName);
        resultIntent.putExtra("disNum", disNum);
        resultIntent.putExtra("disName", disName);
        resultIntent.putExtra("lnglat",lnglat);
        TabhostActivity.this.setResult(0, resultIntent);
        TabhostActivity.this.finish();
    }
    
/*---------------------------------其他操作-----------------------------------*/
    /**
     * 查询台区信息并存入List
     */
    private List<String> queryDistrict(){
    	LogUtil.d(TAG, "queryDistrict");
    	disList=new ArrayList<String>();
    	try {
    		db=dbHelper.getWritableDatabase();
            cursor=db.query("Users", new String[]{"district_number","district_name"},null,null, null, null, null);
            if(cursor.moveToFirst()){
            	String disNum,disName;
            	do{
            		disNum=cursor.getString(cursor.getColumnIndex("district_number"));
            		disName=cursor.getString(cursor.getColumnIndex("district_name"));
            		if(!disList.contains(disNum+" "+disName)){
            			disList.add(disNum+" "+disName);
            		}
            	}while(cursor.moveToNext());
            }
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally{
			if(cursor!=null && !cursor.isClosed()){
				cursor.close();
			}
			if(db!=null && db.isOpen()){
				 db.close();
			}
        }
    	return disList;
    }
    
    /**
     * 查询用户信息并显示在界面上，同时给要传回的经纬度数组lnglat[]赋值
     */
    private void queryData(String userInfo,int flag ){
        LogUtil.d(TAG, "queryData:userInfo="+userInfo+";flag="+flag);
        db=dbHelper.getWritableDatabase();
        try {
            if(flag==0 || flag==1){
            	cursor=db.query("Users", null,"user_id like ?", new String[]{"%"+userInfo+"%"}, null, null, null);
            }else if(flag==2) {
    			cursor=db.query("Users", null,"user_name like ?", new String[]{"%"+userInfo+"%"}, null, null, null);
    		}else if(flag==3 || flag==4) {
            	cursor=db.query("Users", null,"terminal_number like ? or meter_number like ?", new String[]{"%"+userInfo+"%","%"+userInfo+"%"}, null, null, null);
    		}else if(flag==5){
    			cursor=db.query("Users", null,"user_addr like ?", new String[]{"%"+userInfo+"%"}, null, null, null);
			}
            if(cursor.moveToFirst()){
            	LogUtil.d(TAG, "查询成功");
            	mScanResultTextView.setText(userInfo+"  查询成功");
            	if(cursor.getCount()==1){
            		readCursor(cursor);
            	}else{
            		 List<PoiItem> userItems=new ArrayList<PoiItem>();
            		 while(!cursor.isAfterLast()){
            			 String user_name=cursor.getString(cursor.getColumnIndex("user_name"));
            			 String user_id=cursor.getString(cursor.getColumnIndex("user_id"));
                         String user_addr=cursor.getString(cursor.getColumnIndex("user_addr"));
                         userItems.add(new PoiItem(user_name, user_id, user_addr));
                         cursor.moveToNext();
            		 }
            		 cursor.close();
            		 SearchResultDialog dialog=new SearchResultDialog(
            				 TabhostActivity.this,userItems);
            		 dialog.setTitle("您要找的用户是：");
            		 dialog.show();
            		 dialog.setOnListClickListener(new OnListItemClick() {
    					@Override
    					public void onListItemClick(SearchResultDialog dialog, 
    							PoiItem item) {
    						// TODO Auto-generated method stub
    						try {
    							if(!db.isOpen()){
    								db=dbHelper.getWritableDatabase();
    							}
        						cursor=db.query("Users", null,"user_id = ?", new String[]{item.getId()}, null, null, null);
        						cursor.moveToFirst();
        						readCursor(cursor);
        						cursor.close();
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							} finally{
								if(db!=null&&db.isOpen())
									db.close();
								if(cursor!=null&&!cursor.isClosed())
									cursor.close();
							}
    					}
    				});
            	}
            }else{
            	mScanResultTextView.setText(userInfo+"  查询失败");
            	LogUtil.i(TAG, "查询失败");
                ToastUtil.show(this,"查询失败，请重新输入");
            }
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			ToastUtil.show(TabhostActivity.this, "查询失败，请输入更详细信息查询");
		}finally{
			if(cursor!=null&&!cursor.isClosed())
				cursor.close();
			if(db!=null&&db.isOpen())
				db.close();
		}
    }
    
    /**
     * 读取并显示当前游标所指条目
     * @param cursor
     */
    private void readCursor(Cursor cursor){
    	LogUtil.d(TAG, "readCursor");
    	String unit=cursor.getString(cursor.getColumnIndex("unit"));
    	String power_unit=cursor.getString(cursor.getColumnIndex("power_unit"));
    	String district_number=cursor.getString(cursor.getColumnIndex("district_number"));
    	String district_name=cursor.getString(cursor.getColumnIndex("district_name"));
    	String user_id=cursor.getString(cursor.getColumnIndex("user_id"));
    	String user_name=cursor.getString(cursor.getColumnIndex("user_name"));
    	String user_addr=cursor.getString(cursor.getColumnIndex("user_addr"));
    	String terminal_number=cursor.getString(cursor.getColumnIndex("terminal_number"));
    	String meter_number=cursor.getString(cursor.getColumnIndex("meter_number"));
    	String logical_addr=cursor.getString(cursor.getColumnIndex("logical_addr"));
    	String collection_unit=cursor.getString(cursor.getColumnIndex("collection_unit"));
    	Double addr_lng=cursor.getDouble(cursor.getColumnIndex("addr_lng"));
    	Double addr_lat=cursor.getDouble(cursor.getColumnIndex("addr_lat"));
    	String remarks=cursor.getString(cursor.getColumnIndex("remarks"));
    	mUserIdTextView.setText(user_id);
        mUserNameTextView.setText(user_name);
        mUnitTextView.setText(unit);
        mPowerUnitTextView.setText(power_unit);
        mDistrictNumTextView.setText(district_number);
        mDistrictNameTextView.setText(district_name);
        mTerminalNumTextView.setText(terminal_number);
        mMeterNumTextView.setText(meter_number);
        mLogicalAddrTextView.setText(logical_addr);
        mCollectUnitTextView.setText(collection_unit);
        mCoordinateTextView.setText(addr_lng+" , "+addr_lat);
        mUserAddrEditText.setText(user_addr==null?"地址为空，请输入详细地址":user_addr);
        mRemarksEditText.setText(remarks==null?"如有故障，请添加详细描述":remarks);
    }
    
    /**
     * 重新定位，则显示路径采集页面传递过来的定位信息
     */
    public void getLocation(){
    	LatLng currLatLng=(LatLng)getIntent().getExtras().get("currentLoc");//获取路径采集页面传递的intent中携带的经纬度信息
    	if(currLatLng==null){
    		ToastUtil.show(this, "定位失败，请返回前页重新定位");
    		return;
    	}
    	if(mTabHost.getCurrentTab()==0){
        	mCoordinateTextView.setText(currLatLng.longitude+" , "+currLatLng.latitude);
        }else if(mTabHost.getCurrentTab()==1){
        	mCoordinateTextView2.setText(currLatLng.longitude+" , "+currLatLng.latitude);
        }
    }
}
