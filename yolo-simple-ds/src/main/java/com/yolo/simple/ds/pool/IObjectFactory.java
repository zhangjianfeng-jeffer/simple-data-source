package com.yolo.simple.ds.pool;

public interface IObjectFactory<T> {

	public IObjectValue<T> create(IObjectPool<T> objectPool)throws Exception;
	
	public void destroy(IObjectValue<T> t)throws Exception;
	
	public boolean validate(IObjectValue<T> t)throws Exception;
	
}
