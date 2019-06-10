package com.yolo.simple.ds.pool;

public abstract class ObjectFactory<T> implements IObjectFactory<T>{

	public IObjectValue<T> create(IObjectPool<T> objectPool)throws Exception{
		final IObjectPool<T> pool = objectPool;
		IObjectValue<T> objectValue = new ObjectValue<T>() {
			public boolean returnObjectValue() {
				return pool.returnObject(this);
			}
		};
		this.createObj(objectValue);
		return objectValue;
	}
	
	public boolean validate(IObjectValue<T> t)throws Exception{
		boolean validate = false;
		if(t != null){
			long lastReturnTime = t.getLastReturnTime();
			long now = System.currentTimeMillis();
			if(now - lastReturnTime < 1000*60*60*2){
				validate = true;
			}
		}
		return validate;
	}
	
	public abstract void createObj(IObjectValue<T> objectValue)throws Exception;
}
