package com.yolo.simple.ds.pool;

public interface IObjectValue<T> {

	public boolean isValid();
	
	public T getObject();
	
	public void setObject(T t);
	
	public void tagBad();
	
	public void use();
	
	public void release();
	
	public long getCreateTime();
	
	public long getLastUseTime();
	
	public long getLastReturnTime();
	
	public long getContinuedTime();
	
	
	public boolean returnObjectValue();
	
	
}
