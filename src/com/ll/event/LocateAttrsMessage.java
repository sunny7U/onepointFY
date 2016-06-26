package com.ll.event;

public class LocateAttrsMessage {
	private int interval;
	public LocateAttrsMessage(int interval){
		this.interval=interval;
	}
	
	public int getInterval(){
		return interval;
	}
}
