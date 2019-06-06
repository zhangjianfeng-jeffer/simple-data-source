package com.yolo.simple.ds.pool;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class ObjectContainer<T extends IObjectValue<?>> implements IObjectContainer<T> {

	private final LinkedList<T> freeList = new LinkedList<T>();
	private final LinkedList<T> freeBadList = new LinkedList<T>();
	
	private final LinkedList<T> usedList = new LinkedList<T>();
	
	private final ReentrantLock resourcesLock=new ReentrantLock();
	
	public boolean add(T t) {
		boolean result = false;
		if(t!=null){
			try {
				this.resourcesLock.lock();
				result = freeList.add(t);
			}finally{
				this.resourcesLock.unlock();
			}
		}
		
		return result;
	}

	public boolean remove(T t) {
		boolean result = false;
		if(t!=null){
			try {
				this.resourcesLock.lock();
				result = usedList.remove(t);
			}finally{
				this.resourcesLock.unlock();
			}
		}
		return result;
	}

	public T use() {
		T t = null;
		if(!freeList.isEmpty()){
			try {
				this.resourcesLock.lock();
				if(!freeList.isEmpty()){
					t = freeList.getFirst();
					usedList.addLast(t);
					t.use();
				}
			}finally{
				this.resourcesLock.unlock();
			}
		}
		return t;
	}

	public boolean release(T t) {
		boolean result = false;
		if(t!=null){
			try {
				this.resourcesLock.lock();
				if(!usedList.isEmpty()){
					boolean flag = usedList.remove(t);
					if(flag){
						if(t.isValid()){
							freeList.addLast(t);
						}else{
							freeBadList.addLast(t);
						}
						t.release();
						result = true;
					}
				}
			}finally{
				this.resourcesLock.unlock();
			}
		}
		return result;
	}

	public int size() {
		int size;
		try {
			this.resourcesLock.lock();
			size = freeList.size()+usedList.size()+freeBadList.size();
		}finally{
			this.resourcesLock.unlock();
		}
		return size;
	}

	public int badSize() {
		return freeBadList.size();
	}

	public T useBad() {
		T t = null;
		if(!freeBadList.isEmpty()){
			try {
				this.resourcesLock.lock();
				if(!freeBadList.isEmpty()){
					t = freeBadList.getFirst();
					usedList.addLast(t);
					t.use();
				}
			}finally{
				this.resourcesLock.unlock();
			}
		}
		return t;
	}
}
