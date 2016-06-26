package com.ll.main;

import com.ll.R;
import com.ll.data.DataUpdateActivity;
import com.ll.data.InfoUpdateActivity;
import com.ll.event.LocateAttrsMessage;
import com.ll.map.LocCollectingActivity;
import com.ll.map.MapOfflineActivity;
import com.ll.map.RouteNaviActivity;
import com.ll.utils.Constants;
import com.ll.utils.LogUtil;

import de.greenrobot.event.EventBus;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;



public class MainActivity extends Activity implements OnClickListener{
	private String TAG=MainActivity.class.getName();
	private int locInterval=Constants.TWO_SEC;
	
	private TextView mLocationTextView;
	private TextView mNaviTextView;
	private TextView mUpdateUsersTextView;
	private TextView mUpdateDataTextView;
	private TextView mOfflineTextView;
	
	private SharedPreferences preference;
    private SharedPreferences.Editor editor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initViews();
		
		preference=getSharedPreferences("loc_interval", MODE_MULTI_PROCESS);
	    editor=preference.edit();
		
		if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		if(item.getItemId() == R.id.action_settings){
			Intent intent = new Intent();
            intent.setClass(this, SettingActivity.class);
            intent.putExtra("loc_interval",preference.getInt("loc_interval", Constants.TWO_SEC));
            startActivity(intent);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * 初始化控件
	 */
	private void initViews(){
		mLocationTextView = (TextView)findViewById(R.id.loc_collect_tv);
		mNaviTextView = (TextView)findViewById(R.id.route_navi_tv);
		mUpdateUsersTextView=(TextView)findViewById(R.id.user_data_update_tv);
		mUpdateDataTextView=(TextView)findViewById(R.id.data_update_tv);
		mOfflineTextView=(TextView)findViewById(R.id.offline_map_tv);
	
		
		mLocationTextView.setOnClickListener(this);
		mNaviTextView.setOnClickListener(this);
		mUpdateUsersTextView.setOnClickListener(this);
		mUpdateDataTextView.setOnClickListener(this);
		mOfflineTextView.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v){
		locInterval=preference.getInt("loc_interval", Constants.TWO_SEC);
		switch (v.getId()) {
		case R.id.loc_collect_tv:
			//采点绘制轨迹
			Intent collect_intent=new Intent(MainActivity.this,LocCollectingActivity.class);
			collect_intent.putExtra("loc_interval", this.locInterval);
			startActivity(collect_intent);
			break;
		case R.id.route_navi_tv:
			//查询路线并导航
			Intent navi_intent=new Intent(MainActivity.this,RouteNaviActivity.class);
			navi_intent.putExtra("loc_interval", this.locInterval);
			startActivity(navi_intent);
			break;
		case R.id.user_data_update_tv:
			//查询并更新用户信息
			Intent update_intent=new Intent(MainActivity.this,InfoUpdateActivity.class);
			startActivity(update_intent);
			break;
		case R.id.data_update_tv:
			//上传或下载服务器数据
			Intent upload_intent=new Intent(MainActivity.this,DataUpdateActivity.class);
			startActivity(upload_intent);
			break;
		case R.id.offline_map_tv:
			//离线地图管理
			Intent offline_intent=new Intent(MainActivity.this,MapOfflineActivity.class);
			startActivity(offline_intent);
			break;
		default:
			break;
		}
		
	}
	
    public void onEvent(LocateAttrsMessage message) {
    	LogUtil.d(TAG, "onEvent");
    	editor.putInt("loc_interval", message.getInterval());
        editor.commit();
    }
    

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}
	
	
	
}
