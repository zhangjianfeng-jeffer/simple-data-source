package com.yolo.simple.ds.proess;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.yolo.simple.ds.util.StringUtils;

public class Process implements IMonitor, IProcess {
	private String name;
	private static final String separator="_";
	private final Map<String,ICall> callMap=new ConcurrentHashMap<String,ICall>();
	private final Map<String,CallParam> callParamMap=new ConcurrentHashMap<String,CallParam>();
	private final Map<String,CallParam> callParamTemporaryMap=new ConcurrentHashMap<String,CallParam>();
	private final ReentrantLock callParamLock=new ReentrantLock();
	private final ReentrantLock callCountLock=new ReentrantLock();
	private final Object waitObject=new Object();
	private volatile boolean isRunning=true;
	private volatile long callCount;
	
	private long minWaitTime;
	private RunningCheck runningCheck;
	private ProcessThread processThread;
	
	public Process(String name, long minWaitTime){
		this.name = name;
		this.minWaitTime = minWaitTime;
		this.runningCheck = new RunningCheck(5*1000+this.minWaitTime*3);
	}
	
	public synchronized void start(){
		if(processThread==null){
			processThread = new ProcessThread(this.name,minWaitTime) {
				@Override
				protected void runProcess() {
					execute();
				}
			};
		}
	}
	
	public synchronized boolean addCall(String name, ICall call)  {
		boolean flag = false;
		if(callMap.get(name) == null){
			callMap.put(name, call);
			flag = true;
		}
		return flag;
	}

	public boolean send(String name, String callType) {
		boolean result = false;
		if(StringUtils.isNotBlank(name)){
			ICall call = callMap.get(name);
			if(call!=null){
				CallParam callParam=null;
				String key=name+separator+callType;
				callParam=callParamMap.get(key);
				if(callParam==null){
					try{
						callParamLock.lock();
						callParam=callParamMap.get(key);
						if(callParam==null){
							callParam=new CallParam();
							callParam.setName(name);
							callParam.setCallType(callType);
							callParamMap.put(key, callParam);
						}
					}finally{
						callParamLock.unlock();
					}
				}
				int count=callParam.getCount();
				count++;
				callParam.setCount(count);
				result = true;
			}
		}
		if(result){
			try{
				callCountLock.lock();
				callCount++;
			}finally{
				callCountLock.unlock();
			}
			
			if(isRunning==false){
				synchronized (waitObject) {
					waitObject.notifyAll();
				}
			}
			runningCheck.callRun();
		}
		return result;
	}
	
	
	
	private void execute(){
		runningCheck.run();
		copyChange();
		proess();
		tryWait();
	}
	
	
	private void copyChange(){
		boolean isCopy=false;
		try{
			callCountLock.lock();
			if(callCount>0){
				callCount=0;
				isCopy=true;
			}
		}finally{
			callCountLock.unlock();
		}
		
		if(isCopy==false){
			return ;
		}
		
		if(!callParamMap.isEmpty()){
			Iterator<String> it=callParamMap.keySet().iterator();
			while(it.hasNext()){
				String key=it.next();
				CallParam callParam=callParamMap.get(key);
				if(callParam!=null && callParam.getCount()>0){
					CallParam callParamTemporary = callParamTemporaryMap.get(key);
					if(callParamTemporary==null){
						callParamTemporaryMap.put(key, callParam);
					}
					callParam.setCount(0);
				}
			}
		}
	}
	
	
	private void proess(){
		if(!callParamTemporaryMap.isEmpty()){
			Iterator<String> it=callParamTemporaryMap.keySet().iterator();
			while(it.hasNext()){
				String key=it.next();
				CallParam callParam=callParamTemporaryMap.get(key);
				boolean flag=false;
				if(callParam!=null){
					String name = callParam.getName();
					ICall call = callMap.get(name);
					if(call!=null){
						CallResult callResult = call.call(callParam.getCallType());
						if(callResult != null){
							boolean result = callResult.isResult();
							boolean keep = callResult.isKeep();
							if(result == true && keep == false){
								flag = true;
							}
						}
						runningCheck.run();
					}
				}
				if(flag){
					callParamTemporaryMap.remove(key);
				}
			}
		}
	}
	
	
	
	private void tryWait(){
		boolean waitFlag=false;
		try{
			callCountLock.lock();
			waitFlag=(callCount<=0 && callParamTemporaryMap.isEmpty());
		}finally{
			callCountLock.unlock();
		}
		
		if(waitFlag==true){
			synchronized (waitObject) {
				isRunning=false;
				try{
					callCountLock.lock();
					waitFlag=(callCount<=0 && callParamTemporaryMap.isEmpty());
				}finally{
					callCountLock.unlock();
				}
				if(waitFlag==true){
					try {
						waitObject.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isRunning=true;
			}
		}else{
			if(minWaitTime>0){
				try {
					Thread.sleep(minWaitTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				Thread.yield();
			}
		}
	}
	
	
	class RunningCheck{
		private long callTime;
		private long callFrontTime;
		private long runTime;
		private long maxTime=1000*10;
		
		public RunningCheck(){
			this(1000*10);
		}
		
		public RunningCheck(long maxTime){
			this.maxTime=maxTime;
		}
		
		public synchronized void run(){
			this.runTime=System.currentTimeMillis();
		}
		
		public synchronized void callRun(){
			long now=System.currentTimeMillis();
			if(now-callTime>maxTime){
				callFrontTime=callTime;
				callTime=now;
			}
		}
		
		public boolean isRunning(){
			boolean flag=true;
			if(runTime<callFrontTime){
				flag=false;
			}
			return flag;
		}
		
		@Override
		public String toString(){
			return "callTime:"+callTime+",callFrontTime:"+callFrontTime+",runTime:"+runTime+",isRunning:"+isRunning();
		}
	}
	
	
	public boolean isRunning(){
		return runningCheck.isRunning();
	}
	public boolean callReStart(){
		System.out.print("reStart---name:"+name);
		boolean flag=false;
		if(processThread!=null){
			flag=processThread.reStart();
		}
		System.out.print("reStart---name:"+name+",flag:"+flag);
		return flag;
	}
	public boolean monitor(){
		boolean flag = false;
		if(!runningCheck.isRunning()){
			flag = this.callReStart();
		}
		return flag;
	}

}
