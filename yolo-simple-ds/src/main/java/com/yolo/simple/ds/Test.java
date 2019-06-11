package com.yolo.simple.ds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.yolo.simple.ds.pool.IObjectFactory;
import com.yolo.simple.ds.pool.IObjectPool;
import com.yolo.simple.ds.pool.IObjectValue;
import com.yolo.simple.ds.pool.ObjectConnectionProxy;
import com.yolo.simple.ds.pool.ObjectFactory;
import com.yolo.simple.ds.pool.ObjectPool;
import com.yolo.simple.ds.pool.PoolProperties;
import com.yolo.simple.ds.proess.Proess;

public class Test {
	public static long totalSize = 4000;
	public static long count;
	
	private static ThreadPoolExecutor executor = null;
	static{
		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(200);
		int size = 60;
    	executor = new ThreadPoolExecutor(size,size, 3,  TimeUnit.SECONDS, queue);
	}
	
	public static void add(){
		synchronized (Test.class) {
			count++;
		}
		System.out.println("count=========="+count);
		if(count>=totalSize){
			System.out.println("total time :"+(System.currentTimeMillis()-start));
		}
	}
	public static long start;
	public static void main(String[] args)throws Exception {
		Properties properties = new Properties();
		properties.put("driver", "com.mysql.jdbc.Driver");
		properties.put("url", "jdbc:mysql://localhost:3306/yolo?useSSL=false");
		properties.put("username", "root");
		properties.put("password", "root");
		properties.put("coreSize", "2");
		properties.put("maxSize", "50");
		properties.put("maxWaitQueueSize", "100");
		properties.put("waitTimeOut", "1000");
		properties.put("checkFreeMinTime", "100000");
		final DataSource dataSource = new DataSourceDefault("mysql_db_01",properties);
		
		
		Test.start = System.currentTimeMillis();
		for (int i = 0; i < totalSize ; i++) {
			Test.sub(dataSource);
		}
	}
	
	
	
	
	private static void sub(final DataSource dataSource){
    	while (true) {
        	int max = executor.getMaximumPoolSize();
            int currPoolSize = executor.getPoolSize();
            int capacity = executor.getQueue().remainingCapacity();
            if(capacity==0 && currPoolSize==max){
            	try{
            		Thread.sleep(10);
            	}catch(Exception e){
            		e.printStackTrace();
            	}
            }else{
            	break;
            }
        }
    	Runnable run = new Runnable() {
			public void run() {
				try {
					Test.test1(dataSource);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
    	executor.submit(run);
	}

	private static void test1(DataSource dataSource)throws Exception{
		Connection conn = dataSource.getConnection();
		if(conn == null){
			return;
		}
		try {
			long time = System.currentTimeMillis();
			conn.setAutoCommit(false);
			String sql = "INSERT INTO activity_content (ID, CONTENT) VALUES  (?, ? )";
			PreparedStatement pstmt=conn.prepareStatement(sql);
			pstmt.setObject(1, UUID.randomUUID().getMostSignificantBits());
			pstmt.setObject(2, UUID.randomUUID().toString());
			pstmt.executeUpdate();
			conn.commit();
			System.out.println("time===="+(System.currentTimeMillis()-time));
			pstmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(conn!=null){
				conn.close();
			}
		}
		Test.add();
	}
	
	
	public void test()throws Exception{
		final Proess proess = new Proess("test", 5);
		final PoolProperties poolProperties = new PoolProperties();
		
		IObjectFactory<Connection> objectFactory = new ObjectFactory<Connection>() {
			public void destroy(IObjectValue<Connection> t) throws Exception {
				if(t != null){
					Connection conn = t.getObject();
					Connection connR = ObjectConnectionProxy.unwrapConnection(conn);
					if(connR != null){
						connR.close();
					}
				}
			}
			@Override
			public void createObj(IObjectValue<Connection> objectValue)
					throws Exception {
				Connection conn = DriverManager.getConnection(poolProperties.getUrl(), poolProperties.getUsername(), poolProperties.getPassword());
				ObjectConnectionProxy objectConnectionProxy = new ObjectConnectionProxy(conn,objectValue);
				objectValue.setObject(objectConnectionProxy.getConnection());
			}
		};
		
		IObjectPool<Connection> objectPool = new ObjectPool<Connection>("objectPool" ,proess,objectFactory,poolProperties);
		proess.start();
		try {
			Connection conn = objectPool.getObject();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
