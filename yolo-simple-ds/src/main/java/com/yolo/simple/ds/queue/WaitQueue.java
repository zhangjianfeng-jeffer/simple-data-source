package com.yolo.simple.ds.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitQueue {
	/**
	 * 日志工具
	 */
	private static Logger logger = LoggerFactory.getLogger(WaitQueue.class);
	private long lastOfferTime;
	private BlockingQueue<WaitObject> blockingQueue;
	private int queueSize=50;
	private long timeOut=1000;
	
	
	private boolean isDoing=false;
	private ReentrantLock isDoingLock=new ReentrantLock(); 
	public WaitQueue(){
		this.blockingQueue=new LinkedBlockingQueue<WaitObject>(this.queueSize);
	}
	public WaitQueue(int queueSize,long timeOut){
		this.queueSize=queueSize;
		this.timeOut=timeOut;
		this.blockingQueue=new LinkedBlockingQueue<WaitObject>(this.queueSize);
	}
	
	public BlockingQueue<WaitObject> getBlockingQueue() {
		return blockingQueue;
	}
	public long getTimeOut() {
		return timeOut;
	}
	
	public long getLastOfferTime(){
		return this.lastOfferTime;
	}
	
	public boolean offer(WaitObject waitObject){
		this.lastOfferTime = System.currentTimeMillis();
		return blockingQueue.offer(waitObject);
	}
	
	
	public boolean proess(){
		isDoingLock.lock();
		try{
			if(isDoing==true){
				return false;
			}
			isDoing=true;
		}finally{
			isDoingLock.unlock();
		}
		
		boolean isDo=true;
		do{
			isDo=doing();
		}while(isDo==true);
		
		isDoing=false;
		
		return true;
	}
	
	
	/**
	 * 从队列中获取等待对象进行处理
	 * @return
	 */
	private boolean doing(){
		boolean flag=false;
		if(blockingQueue.size()>0){
			WaitObject wait=blockingQueue.peek();
			if(wait!=null){
				if(wait.isState()==true){
					if(wait.isOccupied()==false){
						synchronized (wait) {
							if(wait.isOk()==true){
								//已经成功获取连接
								blockingQueue.remove(wait);
								flag=true;
							}else{
								long now=System.currentTimeMillis();
								if(now-wait.getStartTime()>timeOut){
									//等待超时
									wait.setOk(true);
									blockingQueue.remove(wait);
									flag=true;
									logger.error("连接超时！");
								}
							}
							wait.setOccupied(true);
							//唤醒等待线程
							wait.notifyAll();
						}
					}
				}else{
					wait.setOk(true);
					blockingQueue.remove(wait);
					flag=true;
				}
			}
		}
		
		return flag;
	}
	
	public int getSize(){
		return blockingQueue.size();
	}
	public Long getMaxWaitTime(){
		Long maxWaitTime = null;
		WaitObject wait=blockingQueue.peek();
		if(wait!=null){
			maxWaitTime = wait.getStartTime();
		}
		return maxWaitTime;
	}
}
