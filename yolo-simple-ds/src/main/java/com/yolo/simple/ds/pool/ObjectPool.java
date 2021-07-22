package com.yolo.simple.ds.pool;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yolo.simple.ds.proess.CallResult;
import com.yolo.simple.ds.proess.ICall;
import com.yolo.simple.ds.proess.IProcess;
import com.yolo.simple.ds.queue.WaitObject;
import com.yolo.simple.ds.queue.WaitQueue;

public class ObjectPool<T> implements IObjectPool<T>{

	private static final String WAIT_QUEUE_TYPE = "WAIT_QUEUE_TYPE";
	private static final String CREATE_OBJ_TYPE = "CREATE_OBJ_TYPE";
	private static final String REMOVE_BAD_TYPE = "REMOVE_BAD_TYPE";
	private static final String CLEAN_FREE_TYPE = "CLEAN_FREE_TYPE";
	
	private static final Logger logger = LoggerFactory.getLogger(ObjectPool.class);
	
	private final String name;
	private final IObjectContainer<IObjectValue<T>> objectContainer = new ObjectContainer<IObjectValue<T>>();
	private final IObjectFactory<T> objectFactory;
	
	private final PoolProperties poolProperties;
	/**
	 *等待队列
	 */
	private final WaitQueue<IObjectValue<T>> waitQueue;
	
	private Map<String,ICall> callMap;
	private ICall call;
	
	private IProcess waitQueueProcess;
	private IProcess createObjProcess;
	private IProcess removeBadProcess;
	private IProcess cleanFreeProcess;
	
	private long checkFreeTime;
	
	private volatile boolean onOff = true;
	
	public ObjectPool(String name, IProcess process, IObjectFactory<T> objectFactory, PoolProperties poolProperties)throws Exception{
		this(name,process,process,process,process,objectFactory,poolProperties);
	}
	public ObjectPool(String name, IProcess waitQueueProcess, IProcess createObjProcess, IProcess removeBadProcess, IProcess cleanFreeProcess, IObjectFactory<T> objectFactory, PoolProperties poolProperties)throws Exception{
		this.name = name+"_"+String.valueOf(UUID.randomUUID().getMostSignificantBits());
		this.objectFactory = objectFactory;
		this.poolProperties = poolProperties;
		this.waitQueueProcess = waitQueueProcess;
		this.createObjProcess = createObjProcess;
		this.removeBadProcess = removeBadProcess;
		this.cleanFreeProcess = cleanFreeProcess;
		this.waitQueue = new WaitQueue<IObjectValue<T>>(this.poolProperties.getMaxWaitQueueSize(),this.poolProperties.getWaitTimeOut()) {
			@Override
			protected IObjectValue<T> tryGetSource() {
				return getObjectProcess();
			}
		};
		this.init();
	}
	
	private void init()throws Exception{
		this.callMap = new HashMap<String, ICall>();
		this.callMap.put(ObjectPool.WAIT_QUEUE_TYPE, new ICall() {
			public CallResult call(String callType) {
				CallResult callResult = new CallResult();
				boolean flag = waitQueue.process();
				callResult.setResult(flag);
				if(waitQueue.getSize()>0){
					callResult.setKeep(true);
				}
				return callResult;
			}
		});
		this.callMap.put(ObjectPool.CREATE_OBJ_TYPE, new ICall() {
			public CallResult call(String callType) {
				CallResult callResult = new CallResult();
				createObj();
				callResult.setResult(true);
				callResult.setKeep(false);
				return callResult;
			}
		});
		this.callMap.put(ObjectPool.REMOVE_BAD_TYPE, new ICall() {
			public CallResult call(String callType) {
				CallResult callResult = new CallResult();
				removeBad();
				callResult.setResult(true);
				callResult.setKeep(false);
				return callResult;
			}
		});
		this.callMap.put(ObjectPool.CLEAN_FREE_TYPE, new ICall() {
			public CallResult call(String callType) {
				CallResult callResult = new CallResult();
				cleanFree();
				callResult.setResult(true);
				callResult.setKeep(false);
				return callResult;
			}
		});
		
		
		this.call = new ICall() {
			public CallResult call(String callType) {
				CallResult callResult = null;
				ICall call = callMap.get(callType);
				if(call!=null){
					callResult = call.call(callType);
				}
				return callResult;
			}
		};
		this.waitQueueProcess.addCall(this.name, this.call);
		this.createObjProcess.addCall(this.name, this.call);
		this.removeBadProcess.addCall(this.name, this.call);
		this.cleanFreeProcess.addCall(this.name, this.call);
	}
	
	
	
	public T getObject()throws Exception{
		IObjectValue<T> result = this.getObjectCore();
		System.out.println("totalSize:"+objectContainer.size()+",usedSize:"+objectContainer.usedSize()+",freeSize:"+objectContainer.freeSize()+",freeBadSize:"+objectContainer.badSize()+",waitSize:"+waitQueue.getSize());
		if(result == null){
			throw new Exception("timeout or wait queue is full ,get failed");
		}
		return result.getObject();
	}
	
	
	private IObjectValue<T> getObjectCore(){
		IObjectValue<T> result = null;
		//等待队列为空时直接去获取连接对象返回
		if(waitQueue.getSize()<=0){
			result = this.getObjectProcess();
			if(result!=null){
				return result;
			}
		}
		
		//没有空闲的连接，将请求加入等待队列
		WaitObject<IObjectValue<T>> waitObject=new WaitObject<IObjectValue<T>>();
		waitObject.setStartTime(System.currentTimeMillis());
		synchronized (waitObject) {
			boolean flag=waitQueue.offer(waitObject);
			this.waitQueueProcess.send(this.name, ObjectPool.WAIT_QUEUE_TYPE);
			if(flag){
				try {
					waitObject.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
		result = waitObject.getT();
		return result;
	}
	
	private IObjectValue<T> getObjectProcess(){
		if(!this.onOff){
			return null;
		}
		IObjectValue<T> obj = this.objectContainer.use();
		boolean createFlag = false;
		if(obj!=null){
			boolean validate = this.validate(obj);
			if(validate == false){
				obj.tagBad();
				this.objectContainer.release(obj);
				obj = null;
				this.removeBadProcess.send(this.name, ObjectPool.REMOVE_BAD_TYPE);
				createFlag = true;
			}
			if(createFlag == false){
				if(this.isLess()){
					createFlag = true;
				}
			}
		}else{
			if(!this.isFull()){
				createFlag = true;
			}
		}
		if(createFlag){
			this.createObjProcess.send(this.name, ObjectPool.CREATE_OBJ_TYPE);
		}
		freeCheck.logNum(this.objectContainer.usedSize());
		if(this.isTimeToCheckFree()){
			this.cleanFreeProcess.send(this.name, ObjectPool.CLEAN_FREE_TYPE);
		}
		return obj;
	}
	
	private boolean validate(IObjectValue<T> objectValue){
		boolean flag = false;
		try {
			flag = objectFactory.validate(objectValue);
		} catch (Exception e) {
			objectValue.tagBad();
			e.printStackTrace();
		}
		return flag;
	}
	
	private boolean isFull(){
		boolean isFull = false;
		int maxSize = this.poolProperties.getMaxSize();
		if(this.objectContainer.size()>=maxSize){
			isFull = true;
		}
		return isFull;
	}

	private boolean isMore(){
		boolean isMore = false;
		int coreSize = this.poolProperties.getCoreSize();
		if(this.objectContainer.size()>coreSize){
			isMore = true;
		}
		return isMore;
	}
	
	private boolean isLess(){
		boolean isLess = false;
		int coreSize = this.poolProperties.getCoreSize();
		if(this.objectContainer.size()<coreSize){
			isLess = true;
		}
		return isLess;
	}
	
	
	private boolean isTimeToCheckFree(){
		boolean isTimeToCheckFree = false;
		long now = System.currentTimeMillis();
		if(now - this.checkFreeTime > this.poolProperties.getCheckFreeMinTime()){
			this.checkFreeTime = now;
			isTimeToCheckFree = true;
		}
		return isTimeToCheckFree;
	}
	public boolean returnObject(IObjectValue<T> t) {
		boolean result = objectContainer.release(t);
		if(this.waitQueue.getSize()>0){
			this.waitQueue.flush();
		}
		return result;
	}
	

	private int lastQueueSize = 0;
	private synchronized void createObj(){
		if(!this.onOff){
			return;
		}
		boolean flag = false;
		if(this.isLess()){
			flag = true;
		}else if(!this.isFull()){
			int size = this.waitQueue.getSize();
			if(size>0){
				if(size>this.lastQueueSize){
					flag = true;
				}else{
					int num = this.lastQueueSize - size;
					if(num<size){
						flag = true;
					}
				}
			}
			this.lastQueueSize = size;
		}
		if(flag == true){
			try {
				IObjectValue<T> obj = this.objectFactory.create(this);
				if(obj != null){
					boolean result = this.objectContainer.add(obj);
					if(result == false){
						this.objectFactory.destroy(obj);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(this.isLess()){
			this.createObjProcess.send(this.name, ObjectPool.CREATE_OBJ_TYPE);
		}
	}
	
	private synchronized void removeBad(){
		if(!this.onOff){
			return;
		}
		if(this.objectContainer.badSize()>0){
			while(true){
				IObjectValue<T> obj = this.objectContainer.useBad();
				if(obj != null){
					this.removeObject(obj);
				}else{
					break;
				}
			}
		}
		if(this.objectContainer.badSize()>0){
			this.removeBadProcess.send(this.name, ObjectPool.REMOVE_BAD_TYPE);
		}
	}


	private FreeCheck freeCheck = new FreeCheck(10,10);
	private synchronized void cleanFree(){
		if(!this.onOff){
			return;
		}
		int size = this.waitQueue.getSize();
		if(this.isMore() && size <= 0 ){
			Integer maxNum = freeCheck.getMaxNum();
			if(maxNum!=null && maxNum<this.objectContainer.size()-1){
				int coreSize = this.poolProperties.getCoreSize();
				int times;
				if(maxNum>=coreSize){
					times = (this.objectContainer.size()-1 - maxNum)/2;
				}else{
					times = (this.objectContainer.size() - coreSize)/2;
				}
				long now = System.currentTimeMillis();
				int count = 0;
				while(true){
					if(!this.isMore()){
						break;
					}
					if(count>=times){
						break;
					}
					long lastOfferTime = this.waitQueue.getLastOfferTime();
					if(now - lastOfferTime > 1000*60){
						IObjectValue<T> obj = this.objectContainer.use();
						this.removeObject(obj);
					}else{
						break;
					}
					count ++;
				}
			}
		}
	}
	
	private void removeObject(IObjectValue<T> obj){
		if(obj!=null){
			boolean removeFlag = this.objectContainer.remove(obj);
			if(removeFlag){
				try {
					this.objectFactory.destroy(obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	protected void finalize() throws Throwable {
	    closeAll();
	    super.finalize();
	}
	
	private void closeAll(){
		this.onOff = false;
		IObjectValue<T> obj = null;
		while(true){
			obj = objectContainer.use();
			this.removeObject(obj);
			obj = objectContainer.useBad();
			this.removeObject(obj);
			
			if(objectContainer.size()<=0){
				break;
			}
		}
	}


	public static void main(String[] args)throws Exception {
		FreeCheck freeCheck = new FreeCheck(3,10*1000);
		while (true){
			int num = (int)(Math.random()*10);
			freeCheck.logNum(num);
			System.out.println(freeCheck.toString());
			Thread.sleep(1000);
		}
	}
	
}


class FreeCheck{
	private int size;
	private int timeStep;
	private LinkedList<CheckPoint> useNumList;
	private ReentrantLock logLock=new ReentrantLock();

	private volatile long currentTimePoint;
	private volatile int currentMaxUseNum;

	public FreeCheck(int size,int timeStep){
		this.size = size;
		this.timeStep = timeStep;
		this.useNumList = new LinkedList<CheckPoint>();
	}

	public void logNum(int num){
		long time = System.currentTimeMillis();
		long timePoint =  time/timeStep;
		if(timePoint == currentTimePoint){
			if(num>currentMaxUseNum){
				currentMaxUseNum = num;
			}
		}else{
			if(logLock.tryLock()){
				try{
					CheckPoint checkPoint = new CheckPoint();
					checkPoint.setTimePoint(currentTimePoint);
					checkPoint.setMaxUseNum(currentMaxUseNum);
					currentTimePoint = timePoint;
					currentMaxUseNum = num;
					useNumList.add(checkPoint);
					this.removeExpireCheckPoint();
				}finally {
					logLock.unlock();
				}
			}
		}
	}

	private void removeExpireCheckPoint(){
		boolean overFlag = false;
		while(true){
			overFlag = true;
			if(useNumList.size()>0){
				CheckPoint checkPoint = useNumList.getFirst();
				if(checkPoint!=null){
					long timePoint = checkPoint.getTimePoint();
					if(currentTimePoint - timePoint>size){
						useNumList.removeFirst();
						overFlag = false;
					}
				}
			}
			if(overFlag){
				break;
			}
		}
	}

	public Integer getMaxNum(){
		Integer num = null;
		try {
			logLock.lock();
			if(useNumList.size() >= 0){
				for (CheckPoint checkPoint:useNumList) {
					if(checkPoint!=null){
						int maxUseNum = checkPoint.getMaxUseNum();
						if(num == null || maxUseNum>num){
							num = maxUseNum;
						}
					}
				}
			}
		}finally {
			logLock.unlock();
		}
		return num;
	}

	@Override
	public String toString(){
		String result = null;
		try {
			logLock.lock();
			result = "currentTimePoint:"+currentTimePoint+",currentMaxUseNum:"+currentMaxUseNum+useNumList.toString();
		}finally {
			logLock.unlock();
		}
		return result;
	}
}

class CheckPoint{
	private long timePoint;
	private int maxUseNum;


	public long getTimePoint() {
		return timePoint;
	}

	public void setTimePoint(long timePoint) {
		this.timePoint = timePoint;
	}

	public int getMaxUseNum() {
		return maxUseNum;
	}

	public void setMaxUseNum(int maxUseNum) {
		this.maxUseNum = maxUseNum;
	}

	@Override
	public String toString(){
		return "timePoint:"+timePoint+",maxUseNum:"+maxUseNum;
	}
}
