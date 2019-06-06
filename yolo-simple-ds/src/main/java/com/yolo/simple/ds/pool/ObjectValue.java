package com.yolo.simple.ds.pool;

public abstract class ObjectValue<T> implements IObjectValue<T>{

	protected boolean isValid = true;
	protected long createTime = System.currentTimeMillis();
	protected long lastUseTime;
	protected long lastReturnTime;
	protected long usageCount;
	
	protected T obj;
	
	public boolean isValid() {
		return this.isValid;
	}
	public T getObject() {
		return this.obj;
	}
	public void setObject(T t) {
		this.obj=t;
	}
	public void tagBad() {
		this.isValid = false;
	}
	
	public void use(){
		this.lastUseTime = System.currentTimeMillis();
		this.usageCount++;
	}

	public void release(){
		this.lastReturnTime = System.currentTimeMillis();
	}
	
	public long getCreateTime() {
		return createTime;
	}
	public long getLastUseTime() {
		return lastUseTime;
	}
	public long getLastReturnTime() {
		return lastReturnTime;
	}
	public long getContinuedTime() {
		long continuedTime = 0;
		if(lastReturnTime<lastUseTime){
			continuedTime = System.currentTimeMillis() - lastUseTime;
		}
		return continuedTime;
	}

	
}
