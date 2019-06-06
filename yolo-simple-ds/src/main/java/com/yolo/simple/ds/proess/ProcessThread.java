package com.yolo.simple.ds.proess;


import java.util.concurrent.locks.ReentrantLock;



public abstract class ProcessThread{
			
	private String name;
	private long startCount;
	private long startCallCount;
	private long startCallTime;
	private volatile boolean startFlag;
	private ReentrantLock startLock=new ReentrantLock();
	private long startTime;
	private long minWaitTime;
	private ProcessMainThread processMainThread;
	
	public ProcessThread(String name,long minWaitTime){
		this.name=name;
		this.minWaitTime=minWaitTime;
		start();
	}
	private void start(){
		if(this.processMainThread == null){
			this.startCallCount = 0;
			this.processMainThread = new ProcessMainThread(this.name) {
				@Override
				protected void runProcess() {
					doing();
				}
			};
			this.startTime=System.currentTimeMillis();
			this.startCount++;
			System.out.print("ProcessThread start :"+this.name+", startCount:"+this.startCount);
		}
	}
	
	public boolean reStart(){
		long now=System.currentTimeMillis();
		this.startCallCount++;
		if(this.startCallCount<=1){
			this.startCallTime = now;
		}
		if(this.startCallCount<2){
			return false;
		}
		if(now - this.startCallTime < this.minWaitTime){
			return false;
		}
		
		boolean flag=false;
		try{
			this.startLock.lock();
			if(this.startFlag==true){
				return flag;
			}
			this.startFlag=true;
		}finally{
			this.startLock.unlock();
		}
		
		try{
			if(now-this.startTime>this.minWaitTime){
				if(this.processMainThread!=null){
					this.processMainThread.stop();
					if(this.processMainThread.isStop() || this.processMainThread.isStopTimeOut()){
						this.processMainThread = null;
					}
				}
				if(this.processMainThread == null){
					start();
				}
			}
		}finally{
			this.startFlag=false;
		}
		System.out.print("ProcessThread reStart :"+name+", flag:"+flag);
		return flag;
	}
	
	protected abstract void runProcess();

	private void doing() {
		this.startCallCount = 0;
		this.runProcess();
		this.startCallCount = 0;
	}
}
