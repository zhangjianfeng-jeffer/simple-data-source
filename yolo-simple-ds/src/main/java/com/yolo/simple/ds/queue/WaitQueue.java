package com.yolo.simple.ds.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WaitQueue<T> {
	/**
	 * 日志工具
	 */
	private static Logger logger = LoggerFactory.getLogger(WaitQueue.class);
	private long lastOfferTime;
	private BlockingQueue<WaitObject<T>> blockingQueue;
	private int queueSize=50;
	private long timeOut=1000;

	private ReentrantLock isDoingLock=new ReentrantLock();
	public WaitQueue(){
		this.blockingQueue=new LinkedBlockingQueue<WaitObject<T>>(this.queueSize);
	}
	public WaitQueue(int queueSize,long timeOut){
		this.queueSize=queueSize;
		this.timeOut=timeOut;
		this.blockingQueue=new LinkedBlockingQueue<WaitObject<T>>(this.queueSize);
		WaitQueue.logger.info("new WaitQueue");
	}
	
	public BlockingQueue<WaitObject<T>> getBlockingQueue() {
		return blockingQueue;
	}
	public long getTimeOut() {
		return timeOut;
	}
	
	public long getLastOfferTime(){
		return this.lastOfferTime;
	}
	
	public boolean offer(WaitObject<T> waitObject){
		this.lastOfferTime = System.currentTimeMillis();
		return blockingQueue.offer(waitObject);
	}

	public boolean process(){
		Boolean isDo = null;
		do{
			isDo = this.tryProcess();
		}while(isDo==null || isDo == true);
		return true;
	}
	
	
	public boolean flush(){
		Boolean result = null;
		int maxTimes = 3;
		for (int i = 0; i < maxTimes; i++) {
			result = this.tryProcess();
			if(result==null || result==false){
				break;
			}
		}
		if(result == null){
			return false;
		}
		return true;
	}
	
	
	
	private Boolean tryProcess(){
		Boolean result = null;
		if(isDoingLock.tryLock()){
			try{
				result = this.tryProcessSource();
			}finally {
				isDoingLock.unlock();
			}
		}
		return result;
	}
	
	
	
	private boolean tryProcessSource(){
		boolean result = false;
		if(blockingQueue.size()>0){
			WaitObject<T> wait=blockingQueue.peek();
			if(wait!=null){
				boolean flag=false;
				if(wait.getT()!=null){
					flag = true;
				}else{
					T t = this.tryGetSource();
					if(t != null){
						wait.setT(t);
						flag = true;
					}else{
						long now=System.currentTimeMillis();
						if(now-wait.getStartTime()>timeOut){
							flag = true;
						}
					}
				}
				if(flag == true){
					synchronized (wait) {
						wait.notifyAll();
					}
					blockingQueue.remove(wait);
					result = true;
				}
			}
		}
		return result;
	}
	
	protected abstract T tryGetSource();
	
	public int getSize(){
		return blockingQueue.size();
	}
	public Long getMaxWaitTime(){
		Long maxWaitTime = null;
		WaitObject<T> wait=blockingQueue.peek();
		if(wait!=null){
			maxWaitTime = wait.getStartTime();
		}
		return maxWaitTime;
	}

}
