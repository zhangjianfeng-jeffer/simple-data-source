package com.yolo.simple.ds.queue;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitQueue {
	/**
	 * 日志工具
	 */
	private static Logger logger = LoggerFactory.getLogger(WaitQueue.class);
	
	private BlockingQueue<WaitObject> arrayBlockingQueue;
	private int queueSize=50;
	private long timeOut=1000;
	
	
	private boolean isDoing=false;
	private ReentrantLock isDoingLock=new ReentrantLock(); 
	public WaitQueue(){
		this.arrayBlockingQueue=new ArrayBlockingQueue<WaitObject>(this.queueSize,true);
	}
	public WaitQueue(int queueSize,long timeOut){
		this.queueSize=queueSize;
		this.timeOut=timeOut;
		this.arrayBlockingQueue=new ArrayBlockingQueue<WaitObject>(this.queueSize,true);
	}
	
	public BlockingQueue<WaitObject> getArrayBlockingQueue() {
		return arrayBlockingQueue;
	}
	public long getTimeOut() {
		return timeOut;
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
		if(arrayBlockingQueue.size()>0){
			WaitObject wait=arrayBlockingQueue.peek();
			if(wait!=null){
				if(wait.isState()==true){
					if(wait.isOccupied()==false){
						synchronized (wait) {
							if(wait.isOk()==true){
								//已经成功获取连接
								arrayBlockingQueue.remove(wait);
								flag=true;
							}else{
								long now=new Date().getTime();
								if(now-wait.getStartTime()>timeOut){
									//等待超时
									wait.setOk(true);
									arrayBlockingQueue.remove(wait);
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
					arrayBlockingQueue.remove(wait);
					flag=true;
				}
			}
		}
		
		return flag;
	}
	
	public int getSize(){
		return arrayBlockingQueue.size();
	}
}
