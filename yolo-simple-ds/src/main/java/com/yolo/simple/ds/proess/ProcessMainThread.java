package com.yolo.simple.ds.proess;


public abstract class ProcessMainThread implements Runnable{
	private String name;
	private volatile boolean interrupt=false;
	private volatile boolean isClosed=false;
	private Thread t;
	private long startTime;
	private long stopTime;
	
	public ProcessMainThread(String name){
		this.name = name;
		_start();
		
	}
	
	protected abstract void runProcess();
	
	private void _start(){
		if(t==null){
			this.interrupt=false;
			this.isClosed=false;
			startTime=System.currentTimeMillis();
			t=new Thread(this,"ProcessMainThread--"+this.name+"--"+startTime);
			t.start();
		}
	}
	
	
	public void run() {
		try{
			while(true){
				if(interrupt==true){
					break;
				}
				this.runProcess();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			t = null;
			isClosed=true;
		}
	}
	
	public boolean isStop(){
		return this.isClosed;
	}
	
	public boolean isStopTimeOut(){
		long now = System.currentTimeMillis();
		if(now - stopTime > 1000*10){
			return true;
		}
		return false;
	}
	
	public void stop(){
		if(interrupt == false){
			stopTime = System.currentTimeMillis();
			interrupt = true;
		}
		Thread tP = t;
		if(tP!=null){
			tP.interrupt();
		}
	}
	
}
