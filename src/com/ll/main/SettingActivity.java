package com.ll.main;


import com.ll.R;
import com.ll.event.LocateAttrsMessage;
import com.ll.utils.Constants;

import de.greenrobot.event.EventBus;
import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class SettingActivity extends Activity{
	 private TextView textView;
	 private RadioGroup radioGroup;
	 
	 private int interval;
	
	 

	 private static final String TAG = MainActivity.class.getSimpleName();

	 
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        Log.i(TAG,"onCreate");
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_setting);
	        
	        ActionBar ab = getActionBar();
	        ab.setDisplayHomeAsUpEnabled(true);
	        initView();
	    }
	    
	    public void initView(){
	    	textView=(TextView)findViewById(R.id.interval_tv);
	        radioGroup=(RadioGroup)findViewById(R.id.interval_rgroup);
	        RadioButton radioButton;
	        switch (getIntent().getIntExtra("loc_interval", Constants.TWO_SEC)) {
				case Constants.TWO_SEC:
					radioButton=(RadioButton)findViewById(R.id.two_sec_rb);
					break;
				case Constants.FIVE_SEC:
					radioButton=(RadioButton)findViewById(R.id.five_sec_rb);		
					break;
				case Constants.TEN_SEC:
					radioButton=(RadioButton)findViewById(R.id.ten_sec_rb);
					break;
				case Constants.FIFTEEN_SEC:
					radioButton=(RadioButton)findViewById(R.id.fifteen_sec_rb);
					break;
				case Constants.TWENTY_SEC:
					radioButton=(RadioButton)findViewById(R.id.twenty_sec_rb);
					break;
				default:
					radioButton=(RadioButton)findViewById(R.id.two_sec_rb);
					break;
			}
	        radioButton.setChecked(true);
	        radioGroup.setOnCheckedChangeListener(new myListener());
	    }
	    

	    @Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        // Inflate the menu; this adds items to the action bar if it is present.
//	        getMenuInflater().inflate(R.menu.menu_setting, menu);
	        return true;
	    }

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        Log.i(TAG,"onOptionsItemSelected");
	        // Handle action bar item clicks here. The action bar will
	        // automatically handle clicks on the Home/Up button, so long
	        // as you specify a parent activity in AndroidManifest.xml.
	        int id = item.getItemId();

	        //noinspection SimplifiableIfStatement
	        if (id == android.R.id.home) {
	            finish();
	            return true;
	        }

	        return super.onOptionsItemSelected(item);
	    }

	    class myListener implements OnCheckedChangeListener{

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				switch (checkedId) {
				case R.id.two_sec_rb:
					interval=Constants.TWO_SEC;
					break;
				case R.id.five_sec_rb:
					interval=Constants.FIVE_SEC;
					break;
				case R.id.ten_sec_rb:
					interval=Constants.TEN_SEC;
					break;
				case R.id.fifteen_sec_rb:
					interval=Constants.FIFTEEN_SEC;
					break;
				case R.id.twenty_sec_rb:
					interval=Constants.TWENTY_SEC;
					break;
				default:
					interval=Constants.TWO_SEC;
					break;
				}
				EventBus.getDefault().post(new LocateAttrsMessage(interval));
			}
	    	
	    }
}
