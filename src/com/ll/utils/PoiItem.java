package com.ll.utils;

public class PoiItem {
	private String nameString;
	private String idString;
	private String detailString;
	
	public PoiItem(String name,String id,String detail){
		this.nameString=name;
		this.idString=id;
		this.detailString=detail;
	}
	
	public String getName(){
		return this.nameString;
	}
	
	public String getId(){
		return this.idString;
	}
	
	public String getDetail(){
		return this.detailString;
	}

}
