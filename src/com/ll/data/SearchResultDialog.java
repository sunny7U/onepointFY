package com.ll.data;

import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.ll.data.PoiSearchAdapter;
import com.ll.utils.PoiItem;
import com.ll.R;

public class SearchResultDialog extends Dialog implements OnItemClickListener{
	private List<PoiItem> poiItems;
	private Context context;
	private PoiSearchAdapter adapter;
	protected OnListItemClick mOnClickListener;
/*---------------------------构造器----------------------------------*/	
	public SearchResultDialog(Context context) {
		// TODO Auto-generated constructor stub
		this(context,android.R.style.Theme_Dialog);
	}
	public SearchResultDialog(Context context,int theme) {
		// TODO Auto-generated constructor stub
		super(context, theme);
	}
	public SearchResultDialog(Context context,List<PoiItem> poiItems) {
		// TODO Auto-generated constructor stub
		this(context, android.R.style.Theme_Dialog);
		this.poiItems = poiItems;
		this.context = context;
		adapter = new PoiSearchAdapter(context, poiItems);
	}
/*----------------------------生命周期函数-----------------------------*/
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_results_list);
		ListView listView = (ListView) findViewById(R.id.results_lv);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				dismiss();
				mOnClickListener.onListItemClick(SearchResultDialog.this,
						poiItems.get(position));
			}
		});

	};
	
/*--------------------------------监听器-----------------------------------------*/
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
	}

	public interface OnListItemClick {
		public void onListItemClick(SearchResultDialog dialog, PoiItem item);
	}

	public void setOnListClickListener(OnListItemClick l) {
		mOnClickListener = l;
	}

}
