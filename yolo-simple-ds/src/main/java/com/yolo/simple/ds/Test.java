package com.yolo.simple.ds;

import java.sql.Connection;

import com.yolo.simple.ds.pool.IObjectFactory;
import com.yolo.simple.ds.pool.IObjectValue;
import com.yolo.simple.ds.pool.ObjectConnectionProxy;
import com.yolo.simple.ds.pool.ObjectFactory;
import com.yolo.simple.ds.pool.ObjectPool;
import com.yolo.simple.ds.proess.Proess;

public class Test {

	public static void main(String[] args) {
		
		final Proess proess = new Proess("test", 5);
		IObjectFactory<Connection> objectFactory = new ObjectFactory<Connection>() {

			public void destroy(IObjectValue<Connection> t) throws Exception {
				
			}

			public boolean validate(IObjectValue<Connection> t)
					throws Exception {
				return false;
			}

			@Override
			public void createObj(IObjectValue<Connection> objectValue)
					throws Exception {
				final IObjectValue<Connection> obj = objectValue;
				ObjectConnectionProxy objectConnectionProxy = new ObjectConnectionProxy(null) {
					@Override
					public void tagBad() {
						obj.tagBad();
					}
					@Override
					public boolean release() {
						return obj.returnObjectValue();
					}
				};
				objectValue.setObject(objectConnectionProxy.getConnection());
			}

			
			
		};
		
		ObjectPool<Connection> objectPool = new ObjectPool<Connection>("objectPool" ,proess,objectFactory);
		proess.start();
		try {
			Connection conn = objectPool.getObject();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
