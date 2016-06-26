package com.ll.data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.ll.Zxing.CaptureActivity;
import com.ll.data.SearchResultDialog.OnListItemClick;
import com.ll.main.MainActivity;
import com.ll.utils.AMapUtil;
import com.ll.utils.LogUtil;
import com.ll.utils.PoiItem;
import com.ll.utils.ToastUtil;
import com.ll.R;


public class InfoUpdateActivity extends Activity implements OnClickListener,
        AMapLocationListener{
    private String TAG=InfoUpdateActivity.class.getName();
    private int searchFlag=0; //用于标记搜索方式，0--输入用户ID，1--扫描用户ID，2--输入用户名，3--扫描表计条码

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
    private Button mSaveButton;
    private Button mCancelButton;
    private TextView mScanResultTextView;

    private SQLiteDatabase db;
    private MyDatabaseHelper dbHelper;
    private Cursor cursor=null;
    private ContentValues values;

    private AMapLocationClient locationClient = null;
	private AMapLocationClientOption locationOption = null;
/*-----------------------------------生命周期函数-----------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
    	LogUtil.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_user_data);

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
    protected void onDestroy() {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();
        if (null != locationClient) {
			/**
			 * 如果AMapLocationClient是在当前Activity实例化的，
			 * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
			 */
			locationClient.onDestroy();
			locationClient = null;
			locationOption = null;
		}
    }
/*------------------------------------控件初始化-----------------------------------*/
    private void initViews() {
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
        mSaveButton=(Button)findViewById(R.id.save_btn);
        mCancelButton=(Button)findViewById(R.id.cancel_btn);
        mSearchUserButton.setOnClickListener(this);
        mRelocateButton.setOnClickListener(this);
        mSaveButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        
        mScanResultTextView=(TextView)findViewById(R.id.scan_et);
    }
/*-----------------------------------onClick监听---------------------------------*/
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
                saveNewData(mUserIdTextView.getText().toString().trim());
                break;
            case R.id.cancel_btn:
                Intent backIntent=new Intent(InfoUpdateActivity.this,MainActivity.class);
                startActivity(backIntent);
                break;
            default:
                break;
        }
    }
/*----------------------------------查询及修改用户数据--------------------------------*/
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
            	mScanResultTextView.setText(userInfo+"  查询成功");
            	LogUtil.d(TAG, "查询成功");
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
            				 InfoUpdateActivity.this,userItems);
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
			ToastUtil.show(InfoUpdateActivity.this, "查询失败，请输入更详细信息查询");
		}finally{
			if(cursor!=null&&!cursor.isClosed())
				cursor.close();
			if(db!=null&&db.isOpen())
				db.close();
		}
    }
    
    /**
     * 读取并显示当前游标所指条目
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
    
    /*
     * 保存修改后的用户数据
     */
    private void saveNewData(String user_id){
        LogUtil.d(TAG, "saveNewData:user_id="+user_id);
        if(null!=user_id && !("".equals(user_id))){
            db=dbHelper.getWritableDatabase();
            Cursor cursor=db.query("Users", new String[]{"user_id"},"user_id = ?", new String[]{user_id}, null, null, null);
            //只能更新已有用户的数据
            try {
            	if(!cursor.moveToFirst()){
                    ToastUtil.show(this, "不存在该用户");
                    LogUtil.d(TAG, "不存在该用户");
                }else{
                    values=new ContentValues();

                    Double lngDouble=0.0;
                    Double latDouble=0.0;
                    String user_addr=mUserAddrEditText.getText().toString();
                    String remarks=mRemarksEditText.getText().toString();
                    String point=mCoordinateTextView.getText().toString();
                    if(null!=point){
                        String[] coordinate=point.split(",");
                        lngDouble=Double.parseDouble(coordinate[0].toString().trim());
                        latDouble=Double.parseDouble(coordinate[1].toString().trim());
                    }
                    values.put("addr_lng", lngDouble);
                    values.put("addr_lat", latDouble);
                    values.put("user_addr", user_addr);
                    values.put("remarks", remarks);
                    values.put("record_date", AMapUtil.getSystemTime());
                    values.put("is_new", true);
                    db.update("Users", values, "user_id = ?", new String[]{user_id});
                    LogUtil.i(TAG, "保存修改lng/lat="+lngDouble+"/"+latDouble+";user_addr="+user_addr+";remarks="+remarks);
                    ToastUtil.show(this, "数据保存成功");
                    values.clear();
                }
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				ToastUtil.show(InfoUpdateActivity.this, "储存出现异常");
			}finally{
				if(cursor!=null&&!cursor.isClosed())
					cursor.close();
				if(db!=null&&db.isOpen())
					db.close();
			}
        }else {
            LogUtil.i(TAG, "用户ID为空");
            ToastUtil.show(this, "请输入户号ID");
        }
    }
/*---------------------------------显示多种搜索方式的对话框----------------------------*/	
	/**
	 * 用一个对话框显示搜索方式，让用户选择
	 */
	private void showSearchDialog(){
    	View searchView=LayoutInflater.from(InfoUpdateActivity.this)
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
		
		AlertDialog.Builder builder=new AlertDialog.Builder(InfoUpdateActivity.this);
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
						ToastUtil.show(InfoUpdateActivity.this, "请输入搜索内容");
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
/*----------------------------------通过条形码扫描查询-------------------------------*/   
    /**
     * 开启扫描条码页面
     * @param flag flag=1 扫描用户ID,flag=3 扫描终端局号或表计局号
     */
    private void goToScan(int flag){
    	LogUtil.d(TAG, "goToScan");
    	//实例化intent
        Intent intent = new Intent();
        //设置跳转 的界面
        intent.setClass(InfoUpdateActivity.this, CaptureActivity.class);
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
/*-------------------------------------定位相关-----------------------------------*/
    public void getLocation(){
	    if(locationClient==null){
	    	locationClient = new AMapLocationClient(this);
			locationOption = new AMapLocationClientOption();
			// 设置定位模式为高精度模式
			locationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			locationOption.setOnceLocation(true);
			/**
			 * 设置是否优先返回GPS定位结果，如果30秒内GPS没有返回定位结果则进行网络定位
			 * 注意：只有在高精度模式下的单次定位有效，其他方式无效
			 */
			locationOption.setGpsFirst(true);
			locationClient.setLocationOption(locationOption);
			// 设置定位监听
			locationClient.setLocationListener(this);
	    }
		// 启动定位
		locationClient.startLocation();
		if(!ToastUtil.isShowing()){
			ToastUtil.showDialog(this, "正在定位...");
		}
			
    }

	@Override
	public void onLocationChanged(AMapLocation arg0) {
		// TODO Auto-generated method stub
		if(null!=arg0){
            if(ToastUtil.isShowing()){
                ToastUtil.dismissDialog();
            }

            if (arg0.getErrorCode() == 0) {
                Double lngDouble=arg0.getLongitude();
                Double latDouble=arg0.getLatitude();
                mCoordinateTextView.setText(lngDouble+" , "+latDouble);
            } else {
                LogUtil.e(TAG,"Location ERR:" + arg0.getErrorCode());
                ToastUtil.show(this,"定位失败，请检查GPS或网络后重新定位");
            }

        }
		
	}

}
