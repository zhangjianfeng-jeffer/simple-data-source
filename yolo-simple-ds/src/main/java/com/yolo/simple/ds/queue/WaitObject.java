package com.yolo.simple.ds.queue;



/**
 * 等待对象
 * @author zhangjianfeng
 *
 */
public class WaitObject<T> {
	
	/**
	 * 开始时间
	 */
	private long startTime;
	
	
	
	private T t;
	
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public T getT() {
		return t;
	}

	public void setT(T t) {
		this.t = t;
	}
}
