package com.yolo.simple.ds.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public abstract class ObjectConnectionProxy implements InvocationHandler{

	private static final String CLOSE = "close";
	private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };
	
	private Connection connPhysics;
	private Connection connProxy;
	
	public ObjectConnectionProxy(Connection connPhysics){
		this.connPhysics = connPhysics;
	    this.connProxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
	}
	
	
	public Connection getConnection(){
		return this.connProxy;
	}
	
	
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		String methodName = method.getName();
	    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
	    	this.release();
	    	return null;
	    } else {
	    	Object obj = null;
	    	try {
	    		obj = method.invoke(connPhysics, args);
			} catch (Exception e) {
				this.tagBad();
				throw e;
			}
	        return obj;
	    }
	}
	
	public abstract boolean release();
	public abstract void tagBad();
}
