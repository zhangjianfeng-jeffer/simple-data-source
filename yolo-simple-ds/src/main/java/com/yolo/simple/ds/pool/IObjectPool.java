package com.yolo.simple.ds.pool;

public interface IObjectPool<T> {

	public boolean returnObject(IObjectValue<T> t);
	
	public T getObject()throws Exception;
	
}
