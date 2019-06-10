package com.yolo.simple.ds.pool;

public class PoolProperties {
	
	private String driver;
	private String url;
	private String username;
	private String password;
	
	private int coreSize;
	private int maxSize;
	private int maxWaitQueueSize;
	private int waitTimeOut;
	
	private int checkFreeMinTime;
	
	public String getDriver() {
		return driver;
	}
	public void setDriver(String driver) {
		this.driver = driver;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getCoreSize() {
		return coreSize;
	}
	public void setCoreSize(int coreSize) {
		this.coreSize = coreSize;
	}
	public int getMaxSize() {
		return maxSize;
	}
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	public int getMaxWaitQueueSize() {
		return maxWaitQueueSize;
	}
	public void setMaxWaitQueueSize(int maxWaitQueueSize) {
		this.maxWaitQueueSize = maxWaitQueueSize;
	}
	public int getWaitTimeOut() {
		return waitTimeOut;
	}
	public void setWaitTimeOut(int waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}
	public int getCheckFreeMinTime() {
		return checkFreeMinTime;
	}
	public void setCheckFreeMinTime(int checkFreeMinTime) {
		this.checkFreeMinTime = checkFreeMinTime;
	}
	

}
