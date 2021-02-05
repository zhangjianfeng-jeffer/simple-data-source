package com.yolo.simple.ds.pool;

public interface IObjectContainer<T> {
	
	
	public boolean add(T t);
	
	public boolean remove(T t);
	
	public T use();
	
	public boolean release(T t);

	public int size();
	public int usedSize();
	public int freeSize();
	
	public int badSize();
	public T useBad();
}
