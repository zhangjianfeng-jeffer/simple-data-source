package com.yolo.simple.ds.queue;



/**
 * 等待对象
 * @author zhangjianfeng
 *
 */
public class WaitObject {
	
	/**
	 * 开始时间
	 */
	private long startTime;

	
	/**
	 * 是否等待完成
	 */
	private volatile boolean isOk;
	
	
	/**
	 * 是否占用中
	 */
	private volatile boolean isOccupied;
	
	
	/**
	 * 状态
	 */
	private volatile boolean state;
	
	
	
	public boolean isState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public boolean isOccupied() {
		return isOccupied;
	}

	public void setOccupied(boolean isOccupied) {
		this.isOccupied = isOccupied;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public boolean isOk() {
		return isOk;
	}

	public void setOk(boolean isOk) {
		this.isOk = isOk;
	}

	@Override
	public String toString() {
		return "WaitObject [startTime=" + startTime + ", isOk=" + isOk
				+ ", isOccupied=" + isOccupied + ", state=" + state + "]";
	}
}
