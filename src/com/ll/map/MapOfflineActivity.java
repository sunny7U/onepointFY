package com.ll.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.maps.AMapException;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.offlinemap.OfflineMapCity;
import com.amap.api.maps.offlinemap.OfflineMapManager;
import com.amap.api.maps.offlinemap.OfflineMapManager.OfflineMapDownloadListener;
import com.amap.api.maps.offlinemap.OfflineMapProvince;
import com.amap.api.maps.offlinemap.OfflineMapStatus;
import com.ll.utils.LogUtil;
import com.ll.utils.OffLineMapUtils;
import com.ll.utils.ToastUtil;
import com.ll.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapOfflineActivity extends Activity implements OfflineMapDownloadListener {
    private String TAG=MapOfflineActivity.class.getName();
    private EditText mCityEditText;
    private Button mDownloadButton;
    private ExpandableListView expandableListView;
    
    // 刚进入该页面时初始化弹出的dialog
 	private ProgressDialog initDialog;
    // 长按弹出的dialog
 	private Dialog todoDialog;
 	
    private ArrayList<OfflineMapProvince> provinceList=new ArrayList<OfflineMapProvince>();//保存一级目录的省直辖市
    private HashMap<Object, List<OfflineMapCity>> cityMap=new HashMap<Object, List<OfflineMapCity>>();//保存二级目录的市
    private int groupPosition = -1;// 记录一级目录的position
	private int childPosition = -1;// 记录二级目录的position
	private boolean isStart = false;// 判断是否开始下载,true表示开始下载，false表示下载失败
	private boolean[] isOpen;// 记录一级目录是否打开

    private OfflineMapManager  amapManager = null;// 离线地图下载控制器
    private MapView mapView;
    private final static int UPDATE_LIST = 0;
	private final static int DISMISS_INIT_DIALOG = 1;
	private final static int SHOW_INIT_DIALOG = 2;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case UPDATE_LIST:
				((BaseExpandableListAdapter) adapter).notifyDataSetChanged();
				break;
			case DISMISS_INIT_DIALOG:
				initDialog.dismiss();
				initData();
				handler.sendEmptyMessage(UPDATE_LIST);
				break;
			case SHOW_INIT_DIALOG:
				if (initDialog != null) {
					initDialog.show();
				}

				break;
			default:
				break;
			}
		}

	};
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        LogUtil.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        //设置离线地图存储目录，
        MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
        
        setContentView(R.layout.activity_offline);
        initDialog();
        
        mCityEditText=(EditText)findViewById(R.id.city_name_et);
        mDownloadButton=(Button)findViewById(R.id.download_map);
        expandableListView = (ExpandableListView) findViewById(R.id.list);
		expandableListView.setGroupIndicator(null);
		expandableListView.setAdapter(adapter);
    }
    
    /**
     *  初始化如果已下载的城市多的话，会比较耗时
	 */
	private void initDialog() {

		initDialog = new ProgressDialog(this);
		initDialog.setMessage("正在获取离线城市列表");
		initDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		initDialog.setCancelable(false);
		initDialog.show();

		handler.sendEmptyMessage(SHOW_INIT_DIALOG);

		new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();

				final Handler handler1 = new Handler();
				handler1.postDelayed(new Runnable() {
					@Override
					public void run() {
						// Do Work
						init();
						handler.sendEmptyMessage(DISMISS_INIT_DIALOG);

						handler.removeCallbacks(this);
						Looper.myLooper().quit();
					}
				}, 10);

				Looper.loop();

			}
		}).start();
	}
    
    private void init() {
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        // 此版本限制，使用离线地图，请初始化一个MapView
 		mapView = new MapView(this);

		amapManager = new OfflineMapManager(this, this);
		//获取省列表
		provinceList = amapManager.getOfflineMapProvinceList();

		List<OfflineMapProvince> bigCityList = new ArrayList<OfflineMapProvince>();// 以省格式保存直辖市、港澳、全国概要图
		List<OfflineMapCity> cityList = new ArrayList<OfflineMapCity>();// 以市格式保存直辖市、港澳、全国概要图
		List<OfflineMapCity> gangaoList = new ArrayList<OfflineMapCity>();// 保存港澳城市
		List<OfflineMapCity> gaiyaotuList = new ArrayList<OfflineMapCity>();// 保存概要图
		for (int i = 0; i < provinceList.size(); i++) {
			//获取列表中的一个省
			OfflineMapProvince offlineMapProvince = provinceList.get(i);
			//为该省创建对应的城市列表
			List<OfflineMapCity> city = new ArrayList<OfflineMapCity>();
			//把一个省对象转化成一个市对象
			OfflineMapCity aMapCity = getCicy(offlineMapProvince);
			//获取当前省的所有可下载城市列表
			if (offlineMapProvince.getCityList().size() != 1) {
				city.add(aMapCity);//在一个省组下面包括整个省的地图
				city.addAll(offlineMapProvince.getCityList());//也包括子城市地图
			} else {
				cityList.add(aMapCity);
				bigCityList.add(offlineMapProvince);
			}
			cityMap.put(i + 3, city);
		}
		OfflineMapProvince title = new OfflineMapProvince();

		title.setProvinceName("概要图");
		provinceList.add(0, title);
		title = new OfflineMapProvince();
		title.setProvinceName("直辖市");
		provinceList.add(1, title);
		title = new OfflineMapProvince();
		title.setProvinceName("港澳");
		provinceList.add(2, title);
		provinceList.removeAll(bigCityList);

		for (OfflineMapProvince aMapProvince : bigCityList) {
			if (aMapProvince.getProvinceName().contains("香港")
					|| aMapProvince.getProvinceName().contains("澳门")) {
				gangaoList.add(getCicy(aMapProvince));
			} else if (aMapProvince.getProvinceName().contains("全国概要图")) {
				gaiyaotuList.add(getCicy(aMapProvince));
			}
		}
		try {
			cityList.remove(4);// 从List集合体中删除香港
			cityList.remove(4);// 从List集合体中删除澳门
			cityList.remove(4);// 从List集合体中删除澳门
		} catch (Throwable e) {
			e.printStackTrace();
		}
		cityMap.put(0, gaiyaotuList);// 在HashMap中第0位置添加全国概要图
		cityMap.put(1, cityList);// 在HashMap中第1位置添加直辖市
		cityMap.put(2, gangaoList);// 在HashMap中第2位置添加港澳
		isOpen = new boolean[provinceList.size()];
	}

	/**
	 * 为列表绑定数据源
	 */
	private void initData() {
		expandableListView
				.setOnGroupCollapseListener(new OnGroupCollapseListener() {

					@Override
					public void onGroupCollapse(int groupPosition) {
						;
						isOpen[groupPosition] = false;
					}
				});

		expandableListView
				.setOnGroupExpandListener(new OnGroupExpandListener() {

					@Override
					public void onGroupExpand(int groupPosition) {
						isOpen[groupPosition] = true;
					}
				});
		// 设置二级item点击的监听器
		expandableListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {

				String name = cityMap.get(groupPosition).get(childPosition)
						.getCity();
				mCityEditText.setText(name);
				
				// 保存当前正在正在下载省份或者城市的position位置
				if (isStart) {
					MapOfflineActivity.this.groupPosition = groupPosition;
					MapOfflineActivity.this.childPosition = childPosition;
				}

				handler.sendEmptyMessage(UPDATE_LIST);
				return false;
			}
		});

		expandableListView
				.setOnItemLongClickListener(new OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
							groupPosition = ExpandableListView
									.getPackedPositionGroup(id);
							childPosition = ExpandableListView
									.getPackedPositionChild(id);

							OfflineMapCity mapCity = cityMap.get(groupPosition)
									.get(childPosition);

							showDialog(mapCity.getCity());

						}
						return false;
					}
				});
	}

	public void download(View v){
		LogUtil.d(TAG, "download");
		String cityname=mCityEditText.getText().toString().trim();
	    if(null==cityname ||"".equals(cityname)){
	    	ToastUtil.show(this, "请输入要下载或更新的城市名称");
	    }else{
	    	try{
	    		amapManager.downloadByCityName(cityname);	
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    }
	}
	/**
	 * 长按弹出提示框
	 */
	public void showDialog(final String name) {
		AlertDialog.Builder builder = new Builder(MapOfflineActivity.this);

		builder.setTitle(name);
		builder.setSingleChoiceItems(new String[] { "暂停(暂停正在下载的)", "继续", "删除",
				"检查更新", "停止" }, -1, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				todoDialog.dismiss();
				if (amapManager == null) {
					return;
				}
				switch (arg1) {
				case 0:
					amapManager.pause();
					break;
				case 1:
					try {
						amapManager.downloadByCityName(name);
					} catch (AMapException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 2:
					amapManager.remove(name);
					break;
				case 3:
					try {
						amapManager.updateOfflineCityByName(name);
					} catch (AMapException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 4:
					amapManager.stop();
					break;

				default:
					break;
				}
			}
		});
		builder.setNegativeButton("取消", null);
		todoDialog = builder.create();
		todoDialog.show();
	}

	/**
	 * 把一个省的对象转化为一个市的对象
	 */
	public OfflineMapCity getCicy(OfflineMapProvince aMapProvince) {
		OfflineMapCity aMapCity = new OfflineMapCity();
		aMapCity.setCity(aMapProvince.getProvinceName());
		aMapCity.setSize(aMapProvince.getSize());
		aMapCity.setCompleteCode(aMapProvince.getcompleteCode());
		aMapCity.setState(aMapProvince.getState());
		aMapCity.setUrl(aMapProvince.getUrl());
		return aMapCity;
	}


	final ExpandableListAdapter adapter = new BaseExpandableListAdapter() {

		@Override
		public int getGroupCount() {
			return provinceList.size();
		}

		/**
		 * 获取一级标签内容
		 */
		@Override
		public Object getGroup(int groupPosition) {
			return provinceList.get(groupPosition).getProvinceName();
		}

		/**
		 * 获取一级标签的ID
		 */
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		/**
		 * 获取一级标签下二级标签的总数
		 */
		@Override
		public int getChildrenCount(int groupPosition) {
			return cityMap.get(groupPosition).size();
		}

		/**
		 * 获取一级标签下二级标签的内容
		 */
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return cityMap.get(groupPosition).get(childPosition).getCity();
		}

		/**
		 * 获取二级标签的ID
		 */
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		/**
		 * 指定位置相应的组视图
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * 对一级标签进行设置
		 */
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			TextView group_text;
			ImageView group_image;
			if (convertView == null) {
				convertView = (RelativeLayout) RelativeLayout.inflate(
						getBaseContext(), R.layout.listitem_offmap_group, null);
			}
			group_text = (TextView) convertView.findViewById(R.id.group_text);
			group_image = (ImageView) convertView
					.findViewById(R.id.group_image);
			group_text.setText(provinceList.get(groupPosition)
					.getProvinceName());
			if (isOpen[groupPosition]) {
				group_image.setImageDrawable(getResources().getDrawable(
						R.drawable.downarrow));
			} else {
				group_image.setImageDrawable(getResources().getDrawable(
						R.drawable.rightarrow));
			}
			return convertView;
		}

		/**
		 * 对一级标签下的二级标签进行设置
		 */
		@Override
		public View getChildView(final int groupPosition,
				final int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {

			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = (RelativeLayout) RelativeLayout.inflate(
						getBaseContext(), R.layout.listitem_offmap_child, null);

				holder.cityName = (TextView) convertView
						.findViewById(R.id.name);
				holder.citySize = (TextView) convertView
						.findViewById(R.id.name_size);
				holder.cityDown = (TextView) convertView
						.findViewById(R.id.download_progress_status);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.cityName.setText(cityMap.get(groupPosition)
					.get(childPosition).getCity());
			holder.citySize.setText((cityMap.get(groupPosition).get(
					childPosition).getSize())
					/ (1024 * 1024f) + "MB");

			OfflineMapCity mapCity = cityMap.get(groupPosition).get(
					childPosition);
			// 通过getItem方法获取最新的状态
			if (groupPosition == 0 || groupPosition == 1 || groupPosition == 2) {
				// 全国，直辖市，港澳，按照城市处理
				mapCity = amapManager.getItemByCityName(mapCity.getCity());
			} else {
				if (childPosition == 0) {
					// 省份
					mapCity = getCicy(amapManager.getItemByProvinceName(mapCity
							.getCity()));
				} else {
					// 城市
					mapCity = amapManager.getItemByCityName(mapCity.getCity());
				}
			}
			int state = mapCity.getState();
			int completeCode = mapCity.getcompleteCode();
			if (state == OfflineMapStatus.SUCCESS) {
				holder.cityDown.setText("安装完成");
			} else if (state == OfflineMapStatus.LOADING) {
				if(completeCode==0){
					holder.cityDown.setText("未下载");
				}else{
					holder.cityDown.setText("正在下载" + completeCode + "%");
				}
			} else if (state == OfflineMapStatus.WAITING) {
				holder.cityDown.setText("等待中");
			} else if (state == OfflineMapStatus.UNZIP) {
				holder.cityDown.setText("正在解压" + completeCode + "%");
			} else if (state == OfflineMapStatus.PAUSE) {
				holder.cityDown.setText("暂停中");
			} 
			return convertView;
		}

		class ViewHolder {
			TextView cityName;
			TextView citySize;
			TextView cityDown;
		}

		/**
		 * 当选择子节点的时候，调用该方法
		 */
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (amapManager != null) {
			amapManager=null;
		}
	}

	/**
	 * 离线地图下载回调方法
	 */
	@Override
	public void onDownload(int status, int completeCode, String downName) {

		switch (status) {
		case OfflineMapStatus.SUCCESS:
			ToastUtil.show(this, "地图已成功下载安装");
			break;
		case OfflineMapStatus.LOADING:
			LogUtil.d(TAG, "download: " + completeCode + "%" + ","
					+ downName);
			break;
		case OfflineMapStatus.UNZIP:
			LogUtil.d(TAG, "unzip: " + completeCode + "%" + "," + downName);
			break;
		case OfflineMapStatus.WAITING:
			break;
		case OfflineMapStatus.PAUSE:
			LogUtil.d(TAG, "pause: " + completeCode + "%" + "," + downName);
			break;
		case OfflineMapStatus.STOP:
			break;
		case OfflineMapStatus.ERROR:
			LogUtil.e(TAG, "download: " + " ERROR " + downName);
			break;
		default:
			break;
		}
		handler.sendEmptyMessage(UPDATE_LIST);

	}

	@Override
	public void onCheckUpdate(boolean hasNew, String name) {
		LogUtil.i("amap-demo", "onCheckUpdate " + name + " : " + hasNew);

	}

	@Override
	public void onRemove(boolean success, String name, String describe) {
		LogUtil.i("amap-demo", "onRemove " + name + " : " + success + " , "
				+ describe);
		handler.sendEmptyMessage(UPDATE_LIST);
	}
}

     
  