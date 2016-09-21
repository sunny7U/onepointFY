package com.ll.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
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
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



import com.amap.api.mapcore.util.bu;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.offlinemap.file.Utility;
import com.ll.Zxing.CaptureActivity;
import com.ll.data.SearchResultDialog.OnListItemClick;
import com.ll.utils.AMapUtil;
import com.ll.utils.HttpCallbackListener;
import com.ll.utils.LogUtil;
import com.ll.utils.FileUtil;
import com.ll.utils.PoiItem;
import com.ll.utils.ToastUtil;
import com.ll.utils.UploadUtil;
import com.ll.utils.VolleyUtil;
import com.ll.R;

/**
 * 采集路径过程中位置点信息的采集页面，分为用户点和其他点两种
 * @author Administrator
 */
public class TabhostActivity extends Activity implements OnClickListener,
        OnItemSelectedListener, UploadUtil.OnUploadProcessListener{
    private String TAG=TabhostActivity.class.getName();
    //request codes
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_AUDIO_RECODE = 200;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private int recordFlag = 0; //0表示未开始录音，1表示正在录音中
    //file url to store image/audio
    private Uri fileUri;
    private File mCurrentFile;  //表示最新拍得的图片或录制的语音的文件地址
    
    /**
     * 去上传文件
     */
    public static final int TO_UPLOAD_FILE = 1;
    /**
     * 上传文件响应
     */
    public static final int UPLOAD_FILE_DONE = 2;  //
    /**
     * 选择文件
     */
    public static final int TO_SELECT_PHOTO = 3;
    /**
     * 上传初始化
     */
    public static final int UPLOAD_INIT_PROCESS = 4;
    /**
     * 上传中
     */
    public static final int UPLOAD_IN_PROCESS = 5;
    /**
     * 取消上传
     */
    public static final int UPLOAD_CANCELLED = 6;
    /**
     * 图片上传的URL
     */
//    public static String FILE_UPLOAD_URL = "http://10.0.0.7:8000/api/";
//    /**
//     * 图片下载的URL
//     */
//    public static String FILE_DOWNLOAD_URL = "http://10.0.0.7:8000/api/";
    private String FILE_URL;
    private SharedPreferences preference;
    private SharedPreferences.Editor editor;
    
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
    private TextView mPhotoTextView;
    private Button mPhotoButton;
    private TextView mAudioTextView;
    private Button mAudioButton;
    private ImageView mImageView;
    private ProgressDialog mProgressDialog;
    private TextView mHint;
    private Button mDownloadPhoto;
    private Button mDownloadAudio;
    
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
        preference=getSharedPreferences("updatetime", MODE_MULTI_PROCESS);
        editor=preference.edit();
        FILE_URL = preference.getString("addrHead", "http://") + 
        		preference.getString("serverIP", "121.199.75.35") + 
        		preference.getString("addrTail", ":8000/api/");
        Log.d(TAG, FILE_URL);
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
    
    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // save file url in bundle as it will be null on screen orientation changes
        outState.putParcelable("file_uri", fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }

    /**
     * 1/Android相机应用会把拍好的照片编码为缩小的Bitmap，然后以extra value的方式添加到返回的Intent中，并传送给onActivityResult()
     * 取出Intent中的Bundle，图片对应的key为“data";但是将图片写入到File之后，Bundle中将不会有bitmap返回，Intent为null，所以要从File中加载图像
     * 2/对CaptureActivity返回的扫描结果做处理
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    	if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
            	if (mCurrentFile != null) {
            		mPhotoTextView.setText(Html.fromHtml("<u>"+mCurrentFile.getName()+"</u>"));
            		mPhotoTextView.setTextColor(getResources().getColor(R.color.light_blue));
            		mPhotoTextView.setClickable(true);
            		mPhotoTextView.setOnClickListener(TabhostActivity.this);
            		mImageView.setVisibility(View.VISIBLE);
            		mImageView.setImageBitmap(loadImageFromFile(mCurrentFile.getAbsolutePath()));
            	}
                mCurrentFile = null;
            } else if (resultCode == RESULT_CANCELED) {
                ToastUtil.show(this, "User cancelled image capture");
            } else {
            	ToastUtil.show(this, "Failed to capture image");
            }

        } else if (requestCode == REQUEST_AUDIO_RECODE && resultCode == RESULT_OK) {
            if (resultCode == RESULT_OK) {
            	
            } else if (resultCode == RESULT_CANCELED) {
            	ToastUtil.show(this, "User cancelled audio capture");
            } else {
            	ToastUtil.show(this, "Failed to record audio");
            }
        } else {
        	if (resultCode==RESULT_FIRST_USER) {
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
        mPhotoTextView=(TextView)findViewById(R.id.photo_addr_tv);
        mPhotoButton=(Button)findViewById(R.id.take_photo_btn);
        mAudioTextView=(TextView)findViewById(R.id.audio_addr_tv);
        mAudioButton=(Button)findViewById(R.id.record_audio_btn);
        mImageView=(ImageView)findViewById(R.id.imgae_iv);
        mImageView.setVisibility(View.GONE);
        mDownloadPhoto=(Button)findViewById(R.id.download_photo_btn);
        mDownloadPhoto.setOnClickListener(this);
        mDownloadAudio=(Button)findViewById(R.id.download_audio_btn);
        mDownloadAudio.setOnClickListener(this);
        
        mSearchUserButton.setOnClickListener(this);
        mRelocateButton.setOnClickListener(this);
        mSaveUserButton.setOnClickListener(this);
        mCancelUserButton.setOnClickListener(this);
        mPhotoButton.setOnClickListener(this);
        mAudioButton.setOnClickListener(this);
        
        mProgressDialog=new ProgressDialog(this);
        
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
            case R.id.take_photo_btn:
            	dispatchTakePictureIntent();
            	break;
            case R.id.record_audio_btn:
            	if(recordFlag == 0) {
            		if(startRecord()){
            			mAudioButton.setText("停止录音");
                		recordFlag = 1;
            		}
            	} else {
            		stopRecord();
            		mAudioButton.setText("开始录音");
            		recordFlag = 0;
            		if (mCurrentFile != null) {
                		mAudioTextView.setText(Html.fromHtml("<u>"+mCurrentFile.getName()+"</u>"));
                		mAudioTextView.setTextColor(getResources().getColor(R.color.light_blue));
                		mAudioTextView.setClickable(true);
                		mAudioTextView.setOnClickListener(TabhostActivity.this);
                	}
            		mCurrentFile = null;
            	}
            	break;
            case R.id.photo_addr_tv:
            	showMediaDialog(FileUtil.MEDIA_TYPE_IMAGE,
            			mPhotoTextView.getText().toString().trim());
            	break;
            case R.id.audio_addr_tv:
            	showMediaDialog(FileUtil.MEDIA_TYPE_AUDIO
            			, mAudioTextView.getText().toString().trim());
            	break;
//            case R.id.download_photo_btn:
//            	//应该在每次查询一个新用户时调用
//            	toDownloadFiles(mUserIdTextView.getText().toString().trim(),
//            			FileUtil.MEDIA_TYPE_IMAGE, new mDownloadListener());
//            	
//            case R.id.download_audio_btn:
//            	//应该在每次查询一个新用户时调用
//            	toDownloadFiles(mUserIdTextView.getText().toString().trim(),
//            			FileUtil.MEDIA_TYPE_AUDIO, new mDownloadListener());
            default:
                break;
        }
    }
/*-------------------------------拍照和录音相关函数-------------------------------*/  
    private void dispatchTakePictureIntent() {
    	LogUtil.d(TAG, "dispatchTakePictureIntent");
    	String user_id = mUserIdTextView.getText().toString().trim();
    	if(user_id != null && !user_id.equals("")) {
    		//intent本身，传递执行目的
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //resolveActivity()会返回能处理该intent的第一个Activity，ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the image should go
                File photoFile = FileUtil.getOutputMediaFile(FileUtil.MEDIA_TYPE_IMAGE, user_id);
                if(photoFile != null) {
                	mCurrentFile = photoFile;
                	LogUtil.d(TAG, "filename="+photoFile.getName());
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    //将图片uri存放在key为output的Extra中
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
    	} else {
    		ToastUtil.show(this, "户号不能为空");
    	}
        
    }
    
    private boolean startRecord() {
    	String user_id = mUserIdTextView.getText().toString().trim();
    	if(user_id != null && !user_id.equals("")) {
    		File audioFile = FileUtil.getOutputMediaFile(FileUtil.MEDIA_TYPE_AUDIO, user_id);
    		if( audioFile == null || audioFile.getAbsolutePath() == null){
                Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show();
                return false;
            }
    		mCurrentFile = audioFile;
            //创建MediaRecorder对象
            mRecorder = new MediaRecorder();
            //设置录音的声音来源为麦克风
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置录制的声音的输出格式（必须在编码格式之前设置）
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            //设置声音编码的格式
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(audioFile.getAbsolutePath());
            try{
                mRecorder.prepare();
                mRecorder.start();
            }catch (IllegalStateException e){
                e.printStackTrace();
                return false;
            }catch (IOException e){
                e.printStackTrace();
                return false;
            }
    	} else {
    		ToastUtil.show(this, "户号不能为空");
    		return false;
    	}
    	return true;
        
    }

    private void stopRecord(){
        if(mRecorder != null){
            mRecorder.stop();
            mRecorder.release();
        }
        mRecorder = null;
    }

    private void playAudio(String filepath){
        if(filepath != null){
            try{
                mPlayer = new MediaPlayer();
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mPlayer.release();
                        mPlayer = null;
                        return;
                    }
                });
                mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        LogUtil.e(TAG, "Error in MediaPlayer");
                        mPlayer.release();
                        mPlayer = null;
                        return false;
                    }
                });
                mPlayer.setDataSource(filepath);
                mPlayer.prepare();
                mPlayer.start();
            }catch (IOException E){
                E.printStackTrace();
            }

        }
    }
    
    private void readMedisFiles(int filetype, String filepath){
    	if(filetype == FileUtil.MEDIA_TYPE_IMAGE){
    		File photofile = FileUtil.isFileExist(filepath);
    		if(photofile != null){
    			mPhotoTextView.setText(Html.fromHtml("<u>"+photofile.getName()+"</u>"));
    			mPhotoTextView.setTextColor(getResources().getColor(R.color.light_blue));
    			mPhotoTextView.setClickable(true);
    			mPhotoTextView.setOnClickListener(TabhostActivity.this);
    			mImageView.setVisibility(View.VISIBLE);
    			mImageView.setImageBitmap(loadImageFromFile(photofile.getAbsolutePath()));
    		}else{
    			mImageView.setVisibility(View.GONE);
    			mPhotoTextView.setText(R.string.nothing);
    			mPhotoTextView.setTextColor(getResources().getColor(R.color.contents_text));
    			mPhotoTextView.setClickable(false);
    		}
    	}else{
    		File audiofile = FileUtil.isFileExist(filepath);
    		if(audiofile != null){
    			mAudioTextView.setText(Html.fromHtml("<u>"+audiofile.getName()+"</u>"));
        		mAudioTextView.setTextColor(getResources().getColor(R.color.light_blue));
        		mAudioTextView.setClickable(true);
        		mAudioTextView.setOnClickListener(TabhostActivity.this);
    		}else{
    			mAudioTextView.setText(R.string.nothing);
    			mAudioTextView.setTextColor(getResources().getColor(R.color.contents_text));
    			mAudioTextView.setClickable(false);
    		}
    	}
    }
    
    /**
     * 根据用户id去查询是否存在关于该用户的图片和语音，有则呈现资料地址
     * @param user_id
     */
    private void readMediaFiles(String user_id){
    	if(user_id != null && !user_id.equals("")){
    		File photofile = FileUtil.isFileExist(FileUtil.MEDIA_TYPE_IMAGE, user_id);
    		File audiofile = FileUtil.isFileExist(FileUtil.MEDIA_TYPE_AUDIO, user_id);
    		//如果本地有，就展示本地照片；否则去下载服务器端照片
    		if(photofile != null){
    			LogUtil.d(TAG, "local image");
    			mPhotoTextView.setText(Html.fromHtml("<u>"+photofile.getName()+"</u>"));
    			mPhotoTextView.setTextColor(getResources().getColor(R.color.light_blue));
    			mPhotoTextView.setClickable(true);
    			mPhotoTextView.setOnClickListener(TabhostActivity.this);
    			mImageView.setVisibility(View.VISIBLE);
    			mImageView.setImageBitmap(loadImageFromFile(photofile.getAbsolutePath()));
    		}else{
    			LogUtil.d(TAG, "download image");
    			mImageView.setVisibility(View.GONE);
    			mPhotoTextView.setText(R.string.nothing);
    			mPhotoTextView.setTextColor(getResources().getColor(R.color.contents_text));
    			mPhotoTextView.setClickable(false);
    			toDownloadFiles(user_id, FileUtil.MEDIA_TYPE_IMAGE, new mDownloadListener());
    		}
    		if(audiofile != null){
    			mAudioTextView.setText(Html.fromHtml("<u>"+audiofile.getName()+"</u>"));
        		mAudioTextView.setTextColor(getResources().getColor(R.color.light_blue));
        		mAudioTextView.setClickable(true);
        		mAudioTextView.setOnClickListener(TabhostActivity.this);
    		}else{
    			mAudioTextView.setText(R.string.nothing);
    			mAudioTextView.setTextColor(getResources().getColor(R.color.contents_text));
    			mAudioTextView.setClickable(false);
    			toDownloadFiles(user_id, FileUtil.MEDIA_TYPE_AUDIO, new mDownloadListener());
    		}
    	}
    }
    
    /**
     * 从文件加载图片。通过缩放图片到目标视图尺寸后再载入到内存，来降低内存的使用
     */
    public Bitmap loadImageFromFile(String photoPath) {
        if (photoPath == null || photoPath.length() < 1) {
            return null;
        }
        mImageView.setVisibility(View.VISIBLE);
        //Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        LogUtil.d(TAG, "W="+targetW + " , H ="+targetH);
        if(targetW == 0 || targetH == 0){
        	targetW = 720;
        	targetH = 400;
        }
        //Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        //Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
        try{
        	FileUtil.saveFile(bitmap, photoPath);
        }catch(Exception e){
        	e.printStackTrace();
        }
        
        return bitmap;
    }
 
/*-------------------------------多媒体文件上传下载-------------------------------*/    
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPLOAD_CANCELLED:
                	mProgressDialog.dismiss();
                case UPLOAD_INIT_PROCESS:
                	mProgressDialog.setMax(msg.arg1);
                    break;
                case UPLOAD_IN_PROCESS:
                	mProgressDialog.setProgress(msg.arg1);
                    break;
                case UPLOAD_FILE_DONE:
                	View mView=LayoutInflater.from(TabhostActivity.this)
							.inflate(R.layout.dialog_alert, null);
			    	TextView mAlert=(TextView)mView.findViewById(R.id.alert_tv);
					AlertDialog.Builder builder=new AlertDialog.Builder(TabhostActivity.this);
					builder.setTitle("结果提示");
					builder.setView(mView);
                	if(msg.arg1 == UploadUtil.UPLOAD_SUCCESS_CODE){
                		mAlert.setText("上传成功:" + (String)msg.obj + "\n耗时："+UploadUtil.getRequestTime()+"秒");
                	}else if(msg.arg1 == UploadUtil.UPLOAD_FILE_NOT_EXISTS_CODE){
                		mAlert.setText("上传失败：文件不存在");
                	}else if(msg.arg1 == UploadUtil.UPLOAD_SERVER_ERROR_CODE){
                		mAlert.setText("上传失败:" + (String)msg.obj );
                	}
                	builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						// TODO Auto-generated method stub
    						closeDialog(true, dialog);
    					}
    				});
                	builder.create().show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }

    };
    
    private void toUploadFile(int filetype, String userId, String filepath){
        mProgressDialog.setTitle("正在上传请等待");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setProgressNumberFormat("%1d kb/%2d kb");
        mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Message errMsg = new Message();
				errMsg.arg1 = UPLOAD_CANCELLED;
				handler.sendMessage(errMsg);
			}
		} );
        mProgressDialog.show();
        String fileKey = "image";
        if(filetype == FileUtil.MEDIA_TYPE_AUDIO){
        	fileKey = "audio";
        }
        String requestURL = new StringBuilder(FILE_URL)
        					.append(fileKey).append("?name=").append(userId).toString();
        UploadUtil uploadUtil = UploadUtil.getInstance();;
        uploadUtil.setOnUploadProcessListener(this);  //设置监听器监听上传状态

        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", userId);
        uploadUtil.uploadFile( filepath, fileKey, requestURL, params);
    }
    
    /**
     * 上传服务器响应回调
     */
    @Override
    public void onUploadDone(int responseCode, String message) {
    	mProgressDialog.dismiss();
        Message msg = Message.obtain();
        msg.what = UPLOAD_FILE_DONE;
        msg.arg1 = responseCode;
        msg.obj = message;
        LogUtil.d(TAG, "DONE:"+(String)msg.obj);
        handler.sendMessage(msg);
    }
    
    @Override
    public void onUploadProcess(int uploadSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_IN_PROCESS;
        msg.arg1 = uploadSize/1024;  //byte到kb的转换，存在四舍五入
        LogUtil.d(TAG, "PROC:"+msg.arg1);
        handler.sendMessage(msg );
    }

    @Override
    public void initUpload(int fileSize) {
        Message msg = Message.obtain();
        msg.what = UPLOAD_INIT_PROCESS;
        msg.arg1 = fileSize/1024;
        LogUtil.d(TAG, "INIT:"+msg.arg1);
        handler.sendMessage(msg );
    }
    
    public class mDownloadListener implements HttpCallbackListener{

		@Override
		public void onFinish(String response) {
			// TODO Auto-generated method stub
			String[] result = response.split(";");
			readMedisFiles(Integer.parseInt(result[0]), result[1]);
		}

		@Override
		public void onError(Exception e) {
			// TODO Auto-generated method stub
			LogUtil.e(TAG, e.getMessage());
		}

		@Override
		public void onError(String response) {
			// TODO Auto-generated method stub
			LogUtil.d(TAG, response);
		}
    	
    }
    
    public void toDownloadFiles(String userId, int filetype, HttpCallbackListener downloadListener){
    	if(userId == null || userId.equals("")){
    		return;
    	}
    	if(filetype == FileUtil.MEDIA_TYPE_IMAGE){
    		VolleyUtil.getJson(TabhostActivity.this, FILE_URL + "image?name=" + userId, userId, FileUtil.MEDIA_TYPE_IMAGE, downloadListener);
    	}else if(filetype == FileUtil.MEDIA_TYPE_AUDIO){
    		VolleyUtil.getJson(TabhostActivity.this, FILE_URL + "audio?name=" + userId, userId, FileUtil.MEDIA_TYPE_AUDIO, downloadListener);
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
    
/*------------------------------显示多种搜索方式的对话框----------------------------*/	
    /**
	 * 用一个对话框显示搜索方式，让用户选择
	 */
	private void showMediaDialog(final int type, String filename){
		if(filename!=null && !filename.equals("") && !filename.equals("无")){
			int start = filename.indexOf("_");
			int end = filename.indexOf(".");
			if(start < 0 || start > end){
				return ;
			}
			final String userId = filename.substring(start + 1, end);
			final String filepath = FileUtil.getFilePath(type, userId);
			File file = FileUtil.isFileExist(filepath);
			View mView=LayoutInflater.from(TabhostActivity.this)
					.inflate(R.layout.dialog_alert, null);
	    	TextView mAlert=(TextView)mView.findViewById(R.id.alert_tv);
			AlertDialog.Builder builder=new AlertDialog.Builder(TabhostActivity.this);
			builder.setTitle("文件信息");
			builder.setView(mView);
			if(file != null){
				long time = file.lastModified();
				String lastModifiedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date(time));
				String size = FileUtil.getFileSize(filepath);
	            LogUtil.d(TAG, "time = " + lastModifiedTime + ", size = "+size);
		    	
				mAlert.setText("修改时间："+lastModifiedTime + "\n"+"文件大小："+size);
				builder.setPositiveButton("上传文件", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						toUploadFile(type, userId, filepath);
						closeDialog(true, dialog);
					}
				});
				if(type == FileUtil.MEDIA_TYPE_AUDIO){
					builder.setNeutralButton("播放语音", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							playAudio(filepath);
						}
					});
				}
			}else{
				mAlert.setText("没有文件");
			}
			builder.setNegativeButton("关闭窗口", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			});
			builder.create().show();
		}
	}
	
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
        readMediaFiles(user_id);
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
