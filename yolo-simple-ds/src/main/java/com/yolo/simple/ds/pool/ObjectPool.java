package com.yolo.simple.ds.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yolo.simple.ds.proess.CallResult;
import com.yolo.simple.ds.proess.ICall;
import com.yolo.simple.ds.proess.IProess;
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
	/**
	 *等待队列
	 */
	private WaitQueue waitQueue = new WaitQueue();
	
	private Map<String,ICall> callMap;
	private ICall call;
	
	private IProess waitQueueProess;
	private IProess createObjProess;
	private IProess removeBadProess;
	private IProess cleanFreeProess;
	
	public ObjectPool(String name,IProess proess,IObjectFactory<T> objectFactory){
		this(name,proess,proess,proess,proess,objectFactory);
	}
	public ObjectPool(String name,IProess waitQueueProess,IProess createObjProess,IProess removeBadProess,IProess cleanFreeProess,IObjectFactory<T> objectFactory){
		this.name = name+"_"+String.valueOf(UUID.randomUUID().getMostSignificantBits());
		this.objectFactory = objectFactory;
		this.waitQueueProess = waitQueueProess;
		this.createObjProess = createObjProess;
		this.removeBadProess = removeBadProess;
		this.cleanFreeProess = cleanFreeProess;
		this.init();
	}
	
	private void init(){
		this.callMap = new HashMap<String, ICall>();
		this.callMap.put(ObjectPool.WAIT_QUEUE_TYPE, new ICall() {
			public CallResult call(String callType) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		this.callMap.put(ObjectPool.CREATE_OBJ_TYPE, new ICall() {
			public CallResult call(String callType) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		this.callMap.put(ObjectPool.REMOVE_BAD_TYPE, new ICall() {
			public CallResult call(String callType) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		this.callMap.put(ObjectPool.CLEAN_FREE_TYPE, new ICall() {
			public CallResult call(String callType) {
				// TODO Auto-generated method stub
				return null;
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
		
		this.waitQueueProess.addCall(this.name, this.call);
		this.createObjProess.addCall(this.name, this.call);
		this.removeBadProess.addCall(this.name, this.call);
		this.cleanFreeProess.addCall(this.name, this.call);
	}
	
	
	
	public T getObject()throws Exception{
		IObjectValue<T> result = this.getObjectCore();
		if(result == null){
			throw new Exception("object get null");
		}
		return result.getObject();
	}
	
	
	private IObjectValue<T> getObjectCore(){
		IObjectValue<T> result = null;
		//等待队列为空时直接去获取连接对象返回
		if(waitQueue.getSize()<=0){
			result = this.getObjectProess();
			if(result!=null){
				return result;
			}
		}
		
		//没有空闲的连接，将请求加入等待队列
		WaitObject waitObject=new WaitObject();
		waitObject.setStartTime(System.currentTimeMillis());
		waitObject.setOk(false);
		waitObject.setOccupied(true);
		waitObject.setState(true);
		
		boolean flag=false;
		try {
			flag=waitQueue.getArrayBlockingQueue().offer(waitObject);
		} catch (Exception e) {
			logger.error("waitQueue  offer waitObject failed "+waitObject);
		}
		this.waitQueueProess.send(this.name, ObjectPool.WAIT_QUEUE_TYPE);
		if(flag==false){
			return result;
		}
		synchronized (waitObject) {	
			do{
				try {
					waitObject.setOccupied(false);
					waitObject.wait();
				} catch (InterruptedException e) {
					logger.error("waitObject  wait error "+waitObject);
					waitObject.setOk(true);
					waitObject.setState(false);
					break;
				}
				//获取连接对象
				result = this.getObjectProess();
				waitObject.setOccupied(false);
				if(result!=null){
					waitObject.setOk(true);
					break;
				}
				//获取成功或等待超时跳出
			}while(waitObject.isOk()==false);
		}
		return result;
	}
	
	private IObjectValue<T> getObjectProess(){
		IObjectValue<T> obj = this.objectContainer.use();
		boolean createFlag = false;
		if(obj!=null){
			boolean validate = this.validate(obj);
			if(validate == false){
				obj.tagBad();
				this.objectContainer.release(obj);
				obj = null;
				this.removeBadProess.send(this.name, ObjectPool.REMOVE_BAD_TYPE);
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
			this.createObjProess.send(this.name, ObjectPool.CREATE_OBJ_TYPE);
		}
		if(this.isTimeToCheckFree()){
			this.cleanFreeProess.send(this.name, ObjectPool.CLEAN_FREE_TYPE);
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
		return false;
	}
	
	private boolean isLess(){
		return false;
	}
	
	private boolean isTimeToCheckFree(){
		return true;
	}
	public boolean returnObject(IObjectValue<T> t) {
		return objectContainer.release(t);
	}
}
