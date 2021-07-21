package com.yolo.simple.ds;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import com.yolo.simple.ds.proess.Process;

public class Test {
	public static long totalSize = 50000;
	public static long count;
	
	private static ThreadPoolExecutor executor = null;
	static{
		BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(100);
		int size = 200;
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
		properties.put("url", "jdbc:mysql://10.0.31.40:3306/yolo?useSSL=false");
		properties.put("username", "root");
		properties.put("password", "root");
		properties.put("coreSize", "10");
		properties.put("maxSize", "25");
		properties.put("maxWaitQueueSize", "800");
		properties.put("waitTimeOut", "10000");
		properties.put("checkFreeMinTime", "30000");
		final DataSource dataSource = new DataSourceDefault("mysql_db_01",properties);
		Connection conn =dataSource.getConnection();
		if(conn != null){
			conn.close();
		}

		Test.start = System.currentTimeMillis();
		for (int i = 0; i < totalSize ; i++) {
			Test.sub(dataSource);
		}

		Thread.sleep(5000);
		Test.start = System.currentTimeMillis();
		for (int i = 0; i < totalSize ; i++) {
			Test.sub(dataSource);
		}

		Thread.sleep(5000);
		Test.start = System.currentTimeMillis();
		for (int i = 0; i < totalSize ; i++) {
			Test.sub(dataSource);
		}


		Thread.sleep(5000);
		Test.sub(dataSource);

		Thread.sleep(50000);
	}
	
	
	
	
	private static void sub(final DataSource dataSource){
    	while (true) {
        	int max = executor.getMaximumPoolSize();
            int currPoolSize = executor.getPoolSize();
            int capacity = executor.getQueue().remainingCapacity();
            if(capacity==0 && currPoolSize==max){
            	try{
            		Thread.sleep(1);
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
					long time = System.currentTimeMillis();
					Test.testquery(dataSource);
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					Test.add();
				}
			}
		};
    	executor.submit(run);
	}

	public static void test1(DataSource dataSource)throws Exception{
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
	}
	
	public static void testquery(DataSource dataSource)throws Exception{
		long time1 = System.currentTimeMillis();
		Connection conn = dataSource.getConnection();
		if(conn == null){
			return;
		}
		long time2 = System.currentTimeMillis();
		long time3 = 0;
		long time4 = 0;
		StringBuffer strB = new StringBuffer();
		try {
			conn.setAutoCommit(false);
			String sql = "SELECT * FROM activity_content order by ID limit "+(int)(Math.random()*100)+",1 ";
			PreparedStatement pstmt=conn.prepareStatement(sql);
			ResultSet rs=pstmt.executeQuery();
			time3 = System.currentTimeMillis();
			while(rs.next()){
				for (int i = 1; i < 5; i++) {
					strB.append(rs.getObject(i)+",");
				}
				strB.append("\r\n");
			}
			conn.commit();
			pstmt.close();
			time4 = System.currentTimeMillis();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(conn!=null){
				conn.close();
			}
		}
		long now = System.currentTimeMillis();
		System.out.println("time--all-->:"+(now-time1)+",getConn-->:"+(time2-time1)+",sql-execute-->:"+(time4-time2)+",close-->:"+(now-time4));
		//System.out.println(strB.toString());
	}
	
	
	public void test()throws Exception{
		final Process proess = new Process("test", 5);
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
