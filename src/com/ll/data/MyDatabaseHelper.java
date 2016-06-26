package com.ll.data;

import com.ll.utils.LogUtil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper{
	private String TAG=MyDatabaseHelper.class.getName();
	
	public static final String CREATE_LOCATION="create table Location("
			+"_id integer primary key autoincrement,"
			+"route_id varchar(10),"
			+"longitude decimal(10,6) not null,"
			+"latitude decimal(10,6) not null)";
	
	public static final String CREATE_ROUTE="create table Route("
			+"_id integer primary key autoincrement,"
			+"route_id varchar(10),"
			+"start varchar not null,"
			+"end varchar not null,"
			+"distance decimal(10,6),"
			+"record_date date,"
			+"collector varchar,"
			+"is_new boolean default true)";
	
	public static final String CREATE_NODE="create table Node("
			+"_id integer primary key autoincrement,"
			+"district_number varchar,"
			+"name varchar not null,"
			+"addr_lng decimal(10,6),"
			+"addr_lat decimal(10,6),"
			+"record_date date,"
			+"is_new boolean default true)";
	
	public static final String CREATE_USERS="create table Users("
			+"_id integer primary key autoincrement,"
			+"unit varchar ,"
			+"power_unit varchar,"
			+"district_number varchar,"
			+"district_name varchar,"
			+"user_id char(10) unique,"
			+"user_name varchar,"
			+"user_addr varchar,"
			+"terminal_number char(22),"
			+"meter_number char(22),"
			+"logical_addr char(22),"
			+"collection_unit varchar,"
			+"addr_lng decimal(10,6),"
			+"addr_lat decimal(10,6),"
			+"record_date datetime,"
			+"is_new boolean default false,"
			+"remarks varchar)";
	
	public static final String CREATE_TEMP_USERS="alter table Users rename to _temp_users";
	
	public static final String INSERT_DATA="insert into Users select *,'' from _temp_users";
	
	public static final String DROP_TEMP_USERS="drop table _temp_users";

	public MyDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		LogUtil.d(TAG, "onCreate");
		
		db.execSQL(CREATE_ROUTE);
		db.execSQL(CREATE_LOCATION);
		db.execSQL(CREATE_USERS);
		db.execSQL(CREATE_NODE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		LogUtil.d(TAG, "onUpgrade：oldVersion="+oldVersion+";newVersion="+newVersion);
		db.beginTransaction();//开始事务
		try {
			switch (newVersion) {
			case 2:
				db.execSQL(CREATE_TEMP_USERS);
				db.execSQL(CREATE_USERS);
				db.execSQL(INSERT_DATA);
				db.execSQL(DROP_TEMP_USERS);
				break;
			default:
				break;
			}
			db.setTransactionSuccessful();//设置事务标志为成功，在事务结束时才会提供事务。否则回滚事务
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally{
			db.endTransaction();//如果没有成功则回滚事务
		}
		
	}
}
