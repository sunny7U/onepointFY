package com.ll.data;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ll.utils.PoiItem;
import com.ll.R;

public class PoiSearchAdapter extends BaseAdapter{

	private Context context;
	private List<PoiItem> poiItems = null;
	private LayoutInflater mInflater;

	public PoiSearchAdapter(Context context, List<PoiItem> poiItems) {
		this.context = context;
		this.poiItems = poiItems;
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return poiItems.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.listitem_result, null);
		}

		TextView PoiName = ((TextView) convertView.findViewById(R.id.name_tv));
		TextView PoiID = (TextView) convertView.findViewById(R.id.id_tv);
		TextView PoiAddress = (TextView) convertView.findViewById(R.id.addr_tv);
		PoiName.setText(poiItems.get(position).getName());
		PoiID.setText(poiItems.get(position).getId());
		PoiAddress.setText(poiItems.get(position).getDetail());
		return convertView;
	}

}
