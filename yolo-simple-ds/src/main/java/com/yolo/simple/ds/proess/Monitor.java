package com.yolo.simple.ds.proess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class Monitor implements IMonitor{
	private static Logger logger = LoggerFactory.getLogger(Monitor.class);
	private String name;
	private List<IMonitor> monitorList;
	private long sleepTime;
	private long runTime;
	private ProcessThread processThread;
	private long frontTime =0;
	
	public Monitor(){
		this("MonitorManager",1000);
	}
	
	public Monitor(String name,long sleepTime){
		this.name=name;
		this.sleepTime=sleepTime;
		this.runTime=System.currentTimeMillis();
		this.monitorList= Collections.synchronizedList(new ArrayList<IMonitor>());
		this.processThread=new ProcessThread(name+"MonitorManager",50) {
			@Override
			protected void runProcess() {
				process();
			}
		};
	}
	
	public boolean add(IMonitor monitor){
		return monitorList.add(monitor);
	}
	
	private void process(){
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		runTime=System.currentTimeMillis();
		processP();
		
		if(runTime- frontTime > 1000*60){
			frontTime =runTime;
			logger.info(name+" have been alive!!!");
		}
	}
	
	private void processP(){
		if(monitorList!=null && !monitorList.isEmpty()){
			boolean flag;
			for (IMonitor monitor : monitorList) {
				flag=monitor.isRunning();
				if(flag==false){
					logger.error(monitor.toString()+" is run false!!!");
					System.out.println(monitor.toString());
					boolean result=monitor.callReStart();
					if(result==true){
						logger.info(monitor.toString()+" reStart success!!!");
					}else{
						logger.error(monitor.toString()+" reStart faild!!!");
					}
				}
				runTime=System.currentTimeMillis();
			}
		}
	}
	
	
	public boolean callReStart(){
		return processThread.reStart();
	}
	
	public boolean isRunning(){
		boolean flag=true;
		long now=System.currentTimeMillis();
		if(now-runTime>5*sleepTime){
			flag=false;
		}
		return flag;
	}
	
	
	@Override
	public String toString(){
		return name;
	}

	public boolean monitor() {
		boolean flag = false;
		if(!this.isRunning()){
			flag = this.callReStart();
		}
		return flag;
	}
	
}
