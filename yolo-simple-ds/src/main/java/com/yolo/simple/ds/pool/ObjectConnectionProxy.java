package com.yolo.simple.ds.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

public class ObjectConnectionProxy implements InvocationHandler{

	private static final String CLOSE = "close";
	private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };
	private final IObjectValue<Connection> objectValue;
	
	private Connection connPhysics;
	private Connection connProxy;
	
	public ObjectConnectionProxy(Connection connPhysics,IObjectValue<Connection> objectValue){
		this.connPhysics = connPhysics;
		this.objectValue = objectValue;
	    this.connProxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
	}
	
	
	public Connection getConnection(){
		return this.connProxy;
	}
	
	public Connection getConnPhysics(){
		return this.connPhysics;
	}
	
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		String methodName = method.getName();
	    if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
	    	objectValue.returnObjectValue();
	    	return null;
	    } else {
	    	Object obj = null;
	    	try {
	    		obj = method.invoke(connPhysics, args);
			} catch (Exception e) {
				objectValue.tagBad();
				throw e;
			}
	        return obj;
	    }
	}
	
	public static Connection unwrapConnection(Connection conn) {
		Connection c = null;
		if (Proxy.isProxyClass(conn.getClass())) {
	      InvocationHandler handler = Proxy.getInvocationHandler(conn);
	      if (handler instanceof ObjectConnectionProxy) {
	        c = ((ObjectConnectionProxy) handler).getConnPhysics();
	      }
	    }
	    return c;
	}
}
