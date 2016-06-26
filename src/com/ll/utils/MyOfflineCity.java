package com.ll.utils;

public class MyOfflineCity {
	private String cityName;
	private String completeRate;
	private Boolean isNew;
	
	public MyOfflineCity(String cityName,String completeRate,Boolean isNew) {
		// TODO Auto-generated constructor stub
		this.cityName=cityName;
		this.completeRate=completeRate;
		this.isNew=isNew;
	}
	public String getName(){
		return this.cityName;
	}
     
	public String getRate(){
		return this.completeRate;
	}
	public Boolean getVersion(){
		return this.isNew;
	}
	public void setName(String name){
		this.cityName=name;
	}
	public void setRate(String rate){
		this.completeRate=rate;
	}
	public void setVersion(Boolean hasNew){
		this.isNew=!hasNew;
	}
	
}
