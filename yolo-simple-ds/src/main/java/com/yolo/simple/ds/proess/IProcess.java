package com.yolo.simple.ds.proess;

public interface IProcess {

	public boolean addCall(String name,ICall call);
	
	public boolean send(String name,String callType);
	
}
