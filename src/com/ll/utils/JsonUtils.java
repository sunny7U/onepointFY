package com.ll.utils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;



import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.ll.data.MyDatabaseHelper;

public class JsonUtils {
    private static String TAG=JsonUtils.class.getName();

    /**
     * 向服务器POST数据之后得到的响应，反映服务器接收情况
     */
    public static String[] parseJSON(Context context,String jsonData){
        String[] msgs=new String[2];
        try {
            JSONObject tableObj=new JSONObject(jsonData);
            String msg=tableObj.getString("msg");
            int code=tableObj.getInt("code");
            msgs[0]=msg.toString().trim();
            msgs[1]=String.valueOf(code).trim();

            LogUtil.d("parseJSON", "msg="+msg+"code:"+code);
        } catch (Exception e) {
            // TODO: handle exception
            LogUtil.d("parseJSON", "响应结果解析异常");
            e.printStackTrace();
        }
        LogUtil.d("parseJSON", msgs.toString());
        return msgs;
    }

    /**
     * 下载路线JSON之后解析存库
     * {"route":[{"route_id":route_id,"start":start,"end":end,...,
     *             "Location":[{"longitude":longitude,"latitude":latitude},
     *			  			   {"longitude":longitude,"latitude":latitude},
     *           				  ...
     *           			   {"longitude":longitude,"latitude":latitude}]},
     *  		 ...
     *  		 {"route_id":route_id,"start":start,"end":end,...,
     *             "Location":[{"longitude":longitude,"latitude":latitude},
     *			  			   {"longitude":longitude,"latitude":latitude},
     *           				  ...
     *           			   {"longitude":longitude,"latitude":latitude}]}],
     *   "node":[ {"name":n1,"district_num":dist1},   
     * 			  {"name":n1,"district_num":dist1}, 
     * 		     	...
     * 		      {"name":n1,"district_num":dist1}]}       
     * 从服务器下载解析的路线is_new设置为false
     */
    public static boolean parseJSONToLoc(Context context,String jsonData){
    	MyDatabaseHelper dbHelper=new MyDatabaseHelper(context, "Locations.db", null, 2);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        Cursor rCursor=null,nCursor=null;
        try{
            Log.d(TAG, "parseJSONToLoc");
            //route表和location表
            String route_id="";
            String longitude="";
            String latitude="";

            String start="";
            String end="";
            double distance=0.0;
            String record_date="";
            String collector="";
           
            JSONArray locations;
            
            ContentValues rValues=new ContentValues();
            ContentValues lValues=new ContentValues();
            ContentValues nValues=new ContentValues();

            JSONObject routeObject=new JSONObject(jsonData);
            JSONArray routeArray=routeObject.getJSONArray("route");
            JSONArray nodeArray=routeObject.getJSONArray("node");
            int numsOfNode=nodeArray.length();
            int numsOfRoute=routeArray.length();

        	for(int i=0;i<numsOfRoute;i++){
                JSONObject rRecord=routeArray.getJSONObject(i);
                route_id=rRecord.getString("route_id");
                rCursor=db.query("Route", null, "route_id = ?", new String[]{route_id},null,null,null);
                //判重，只把本地不存在的路线存库
                if(!rCursor.moveToFirst()){
                    start=rRecord.getString("start");
                    end=rRecord.getString("end");
                    distance=rRecord.getDouble("distance");
                    record_date=rRecord.getString("record_date");
                    collector=rRecord.getString("collector");
                    locations=rRecord.getJSONArray("locations");
                    int numsOfLoc=locations.length();
                    for(int j=0;j<numsOfLoc;j++){
                        JSONObject lRecord=locations.getJSONObject(j);

                        longitude=lRecord.getString("lng");
                        latitude=lRecord.getString("lat");

                        lValues.put("route_id", route_id);
                        lValues.put("longitude", longitude);
                        lValues.put("latitude", latitude);
                        db.insert("Location", null, lValues);
                        lValues.clear();
                    }
                    rValues.put("route_id", route_id);
                    rValues.put("start", start);
                    rValues.put("end", end);
                    rValues.put("distance", distance);
                    rValues.put("record_date", record_date);
                    rValues.put("collector", collector);
                    rValues.put("is_new", false);

                    db.insert("Route", null, rValues);
                    rValues.clear();
                }else{
                    LogUtil.d("parseJSONToLoc","重复路线");
                }
                rCursor.close();
            }
           
            for(int i=0;i<numsOfNode;i++){
            	JSONObject nRecord=nodeArray.getJSONObject(i);
            	String name=nRecord.getString("name");
            	nCursor=db.query("Node", null, "name = ?", new String[]{name}, null, null, null);
            	//判重，当节点表中没有该节点时在存库
            	if(!nCursor.moveToFirst()){
            		String dis_num=nRecord.getString("dis_num");
                	String date=nRecord.getString("record_date");
                	String lngString=nRecord.getString("lng");
                	String latString=nRecord.getString("lat");
            		nValues.put("district_number", dis_num);
                	nValues.put("name", name);
                	nValues.put("addr_lng", lngString);
                	nValues.put("addr_lat", latString);
                    nValues.put("record_date", date);
                    nValues.put("is_new", false);
                    db.insert("Node", null, nValues);
            	}
            	nValues.clear();
            	nCursor.close();
            }
            return true;

        }catch(Exception e){
            LogUtil.e(TAG, "解析路线异常");
            e.printStackTrace();
            return false;
        }finally{
        	if(rCursor!=null&&!rCursor.isClosed())
        		rCursor.close();
        	if(nCursor!=null&&!nCursor.isClosed())
        		nCursor.close();
        	if(db!=null&&db.isOpen())
        		db.close();
        }
    }

    /**
     * 下载用户JSON后解析存库
     *[{"unit":unit,"power_unit":power_unit, ... ,"record_date":record_date},
     * {"unit":unit,"power_unit":power_unit, ... ,"record_date":record_date},
     *   ...
     * {"unit":unit,"power_unit":power_unit, ... ,"record_date":record_date}]
     */
    public static boolean parseJSONToUsers(Context context,String jsonData){
        String unit="";
        String power_unit="";
        String district_number="";
        String district_name="";
        String user_id="";
        String user_name="";
        String user_addr="";
        String terminal_number="";
        String meter_number="";
        String logical_addr="";
        String collection_unit="";
        String addr_lng="";
        String addr_lat="";
        String record_date="";
        String remarks="";
        MyDatabaseHelper dbHelper=new MyDatabaseHelper(context, "Locations.db", null, 2);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        ContentValues uValues=new ContentValues();
        Cursor cursor=null;
        try{
            LogUtil.d(TAG, "parseJSONToUsers");
            JSONArray userArray=new JSONArray(jsonData);
            int numsOfUser=userArray.length();
        	for(int i=0;i<numsOfUser;i++){
                JSONObject uRecord=userArray.getJSONObject(i);
                user_id=uRecord.getString("user_id");

                unit=uRecord.getString("unit");
                power_unit=uRecord.getString("power_unit");
                district_number=uRecord.getString("district_number");
                district_name=uRecord.getString("district_name");
                user_name=uRecord.getString("user_name");
                user_addr=uRecord.getString("user_addr");
                terminal_number=uRecord.getString("terminal_number");
                meter_number=uRecord.getString("meter_number");
                logical_addr=uRecord.getString("logical_addr");
                collection_unit=uRecord.getString("collection_unit");
                addr_lng=uRecord.getString("addr_lng");
                addr_lat=uRecord.getString("addr_lat");
                record_date=uRecord.getString("record_date");
                remarks=uRecord.getString("remark1");

                uValues.put("unit", unit);
                uValues.put("power_unit", power_unit);
                uValues.put("district_number", district_number);
                uValues.put("district_name", district_name);
                uValues.put("user_id", user_id);
                uValues.put("user_name", user_name);
                uValues.put("user_addr", user_addr);
                uValues.put("terminal_number", terminal_number);
                uValues.put("meter_number", meter_number);
                uValues.put("logical_addr", logical_addr);
                uValues.put("collection_unit", collection_unit);
                uValues.put("addr_lng", addr_lng);
                uValues.put("addr_lat", addr_lat);
                uValues.put("record_date", record_date);
                uValues.put("is_new",false);
                uValues.put("remarks", remarks);

                cursor=db.query("Users", new String[]{"user_id"}, "user_id = ?", new String[]{user_id},null,null,null);
                if(cursor.moveToFirst()){
                    db.update("Users", uValues, "user_id = ?", new String[]{user_id});
                }else{
                    db.insert("Users", null, uValues);
                }
                cursor.close();
                uValues.clear();
            }
            LogUtil.d(TAG, "解析用户成功");
            return true;
        }catch(Exception e){
            e.printStackTrace();
            LogUtil.e(TAG, "解析用户失败");
            return false;
        }finally{
        	if(cursor!=null&&!cursor.isClosed())
        		cursor.close();
        	if(db!=null&&db.isOpen())
        		db.close();
        	uValues=null;
        }
    }

    /**
     * 把路线和点信息转换为JSON数据
     */
    public static String convertLocToJSON(Context context){
        LogUtil.d(TAG, "convertLocToJSON");

        String route_id="";
        String start="";
        String end="";
        double distance=0.0;
        String record_date="";
        String collector="";
        Double longitude=0.0;
        Double latitude=0.0;
        String dis_num="";
        String name="";

        MyDatabaseHelper dbHelper=new MyDatabaseHelper(context, "Locations.db", null, 2);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        Cursor rCursor=null,lCursor=null,nCursor=null;
        //上传新路线，上传成功后要把对应路线设为false
        try {
        	rCursor=db.query("Route", null, "is_new = ?", new String[]{"1"}, null, null, null);
        	if(rCursor.moveToFirst()){
        		LogUtil.d("convertLocToJSON", "有线");
        		JSONStringer jsonData=new JSONStringer();
                jsonData.object();
                jsonData.key("route");
                jsonData.array();
                for(int i=0;i<rCursor.getCount();i++){
                    jsonData.object();

                    route_id=rCursor.getString(rCursor.getColumnIndex("route_id"));
                    start=rCursor.getString(rCursor.getColumnIndex("start"));
                    end=rCursor.getString(rCursor.getColumnIndex("end"));
                    distance=rCursor.getDouble((rCursor.getColumnIndex("distance")));
                    record_date=rCursor.getString(rCursor.getColumnIndex("record_date"));
                    collector=rCursor.getString(rCursor.getColumnIndex("collector"));

                    jsonData.key("route_id");
                    jsonData.value(route_id);
                    jsonData.key("start");
                    jsonData.value(start);
                    jsonData.key("end");
                    jsonData.value(end);
                    jsonData.key("distance");
                    jsonData.value(distance);
                    jsonData.key("record_date");
                    jsonData.value(record_date);
                    jsonData.key("collector");
                    jsonData.value(collector);
                    jsonData.key("locations");
                    jsonData.array();

                    lCursor=db.query("Location", null, "route_id = ?", new String[]{route_id}, null, null, null);
                    if(lCursor.moveToFirst()){
                        LogUtil.d("convertLocToJSON", "有点");
                        do{
                            jsonData.object();
                            longitude=lCursor.getDouble(lCursor.getColumnIndex("longitude"));
                            latitude=lCursor.getDouble(lCursor.getColumnIndex("latitude"));

                            jsonData.key("lng");
                            jsonData.value(longitude);
                            jsonData.key("lat");
                            jsonData.value(latitude);
                            jsonData.endObject();
                        }while(lCursor.moveToNext());
                    }else{
                        LogUtil.d("convertLocToJSON", "查不到点");
                    }
                    lCursor.close();
                    jsonData.endArray();
                    jsonData.endObject();
                    rCursor.moveToNext();
                }
                rCursor.close();
                jsonData.endArray();
                jsonData.key("node");
                jsonData.array();
                //只上传新节点
                if(!db.isOpen())
                	db=dbHelper.getWritableDatabase();
                nCursor=db.query("Node", null, "is_new = ?",new String[]{"1"}, null, null, null);
                if(nCursor.moveToFirst()){
                    LogUtil.d("convertLocToJSON", "Node有新节点");
                    do{
                    	
                        jsonData.object();
                        dis_num=nCursor.getString(nCursor.getColumnIndex("district_number"));
                        name=nCursor.getString(nCursor.getColumnIndex("name"));
                        longitude=nCursor.getDouble(nCursor.getColumnIndex("addr_lng"));
                        latitude=nCursor.getDouble(nCursor.getColumnIndex("addr_lat"));
                        record_date=nCursor.getString(nCursor.getColumnIndex("record_date"));
                        jsonData.key("name");
                        jsonData.value(name);
                        jsonData.key("dis_num");
                        jsonData.value(dis_num);
                        jsonData.key("lng");
                        jsonData.value(longitude);
                        jsonData.key("lat");
                        jsonData.value(latitude);
                        jsonData.key("record_date");
                        jsonData.value(record_date);
                        jsonData.endObject();
                        Log.d(TAG, "name"+name);
                    }while(nCursor.moveToNext());
                    nCursor.close();
                }
                jsonData.endArray();
                jsonData.endObject();
                return jsonData.toString();
        	}else{
                LogUtil.d(TAG, "没有新路线");
                if(ToastUtil.isShowing()){
                    ToastUtil.dismissDialog();
                }
                ToastUtil.show(context, "没有新路线可以上传");
                return null;
            }
		} catch (Exception e) {
			// TODO: handle exception
			 LogUtil.e(TAG, "查询出现异常");
             if(ToastUtil.isShowing()){
                 ToastUtil.dismissDialog();
             }
             ToastUtil.show(context, "查询出现异常");
             e.printStackTrace();
             return null;
		}finally{
			if(rCursor!=null&&!rCursor.isClosed())
        		rCursor.close();
        	if(nCursor!=null&&!nCursor.isClosed())
        		nCursor.close();
        	if(lCursor!=null&&!lCursor.isClosed())
        		lCursor.close();
        	if(db!=null&&db.isOpen())
        		db.close();
		}
    }

    /**
     * 把用户信息转换为JSON数据
     */
    public static String convertUsersToJSON(Context context){
        LogUtil.d(TAG, "convertUsersToJSON");

        String unit="";
        String power_unit="";
        String district_number="";
        String district_name="";
        String user_id="";
        String user_name="";
        String user_addr="";
        String terminal_number="";
        String meter_number="";
        String logical_addr="";
        String collection_unit="";
        Double addr_lng=0.0;
        Double addr_lat=0.0;
        String record_date="";
        String remarks="";

        MyDatabaseHelper dbHelper=new MyDatabaseHelper(context, "Locations.db", null, 2);
        SQLiteDatabase db=dbHelper.getWritableDatabase();
        Cursor uCursor=db.query("Users", null, "is_new = ?", new String[]{"1"}, null, null, null);
        try{
            if(uCursor.moveToFirst()){
	            JSONStringer jsonData=new JSONStringer();
	            jsonData.array();
	            do{ unit=uCursor.getString(uCursor.getColumnIndex("unit"));
	                power_unit=uCursor.getString(uCursor.getColumnIndex("power_unit"));
	                district_number=uCursor.getString(uCursor.getColumnIndex("district_number"));
	                district_name=uCursor.getString(uCursor.getColumnIndex("district_name"));
	                user_id=uCursor.getString(uCursor.getColumnIndex("user_id"));
	                user_name=uCursor.getString(uCursor.getColumnIndex("user_name"));
	                user_addr=uCursor.getString(uCursor.getColumnIndex("user_addr"));
	                terminal_number=uCursor.getString(uCursor.getColumnIndex("terminal_number"));
	                meter_number=uCursor.getString(uCursor.getColumnIndex("meter_number"));
	                logical_addr=uCursor.getString(uCursor.getColumnIndex("logical_addr"));
	                collection_unit=uCursor.getString(uCursor.getColumnIndex("collection_unit"));
	                addr_lng=uCursor.getDouble(uCursor.getColumnIndex("addr_lng"));
	                addr_lat=uCursor.getDouble(uCursor.getColumnIndex("addr_lat"));
	                record_date=uCursor.getString(uCursor.getColumnIndex("record_date"));
	                remarks=uCursor.getString(uCursor.getColumnIndex("remarks"));
	                jsonData.object();
	                jsonData.key("unit");
	                jsonData.value(unit);
	                jsonData.key("power_unit");
	                jsonData.value(power_unit);
	                jsonData.key("district_number");
	                jsonData.value(district_number);
	                jsonData.key("district_name");
	                jsonData.value(district_name);
	                jsonData.key("user_id");
	                jsonData.value(user_id);
	                jsonData.key("user_name");
	                jsonData.value(user_name);
	                jsonData.key("user_addr");
	                jsonData.value(user_addr);
	                jsonData.key("terminal_number");
	                jsonData.value(terminal_number);
	                jsonData.key("meter_number");
	                jsonData.value(meter_number);
	                jsonData.key("logical_addr");
	                jsonData.value(logical_addr);
	                jsonData.key("collection_unit");
	                jsonData.value(collection_unit);
	                jsonData.key("addr_lng");
	                jsonData.value(addr_lng);
	                jsonData.key("addr_lat");
	                jsonData.value(addr_lat);
	                jsonData.key("record_date");
	                jsonData.value(record_date);
	                jsonData.key("remark1");
	                jsonData.value(remarks);
	                jsonData.endObject();
	            }while(uCursor.moveToNext());
	            jsonData.endArray();
	            LogUtil.d("convertUsersToJSON", jsonData.toString());
	            return jsonData.toString();
	        }else{
	            LogUtil.d(TAG, "没有新用户");
	            if(ToastUtil.isShowing()){
	                ToastUtil.dismissDialog();
	            }
	            ToastUtil.show(context, "没有新用户可以上传");
	            return null;
	        }
        }catch (Exception e) {
            // TODO: handle exception
            LogUtil.e(TAG, "查询出现异常");
            if(ToastUtil.isShowing()){
                ToastUtil.dismissDialog();
            }
            ToastUtil.show(context, "查询出现异常");
            e.printStackTrace();
            return null;
        }finally{
        	if(uCursor!=null&&!uCursor.isClosed())
        		uCursor.close();
        	if(db!=null&&db.isOpen())
	            db.close();
        }
    }
}
