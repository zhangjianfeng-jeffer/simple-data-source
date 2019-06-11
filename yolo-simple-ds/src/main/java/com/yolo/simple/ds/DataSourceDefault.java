package com.yolo.simple.ds;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.yolo.simple.ds.pool.IObjectFactory;
import com.yolo.simple.ds.pool.IObjectPool;
import com.yolo.simple.ds.pool.IObjectValue;
import com.yolo.simple.ds.pool.ObjectConnectionProxy;
import com.yolo.simple.ds.pool.ObjectFactory;
import com.yolo.simple.ds.pool.ObjectPool;
import com.yolo.simple.ds.pool.PoolProperties;
import com.yolo.simple.ds.proess.Monitor;
import com.yolo.simple.ds.proess.Proess;
import com.yolo.simple.ds.util.StringUtils;

public class DataSourceDefault implements DataSource{
	private static Proess proessQueue = new Proess("DataSourceDefault_proessQueue", 1);
	private static Proess proessOther = new Proess("DataSourceDefault_proessOther", 50);
	private static Monitor monitorOne = new Monitor("monitorOne",1000);
	private static Monitor monitorDouble = new Monitor("monitorDouble",1000);
	
	private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<String, Driver>();
	static {
		proessQueue.start();
		proessOther.start();
		monitorOne.add(proessQueue);
		monitorOne.add(proessOther);
		monitorOne.add(monitorDouble);
		monitorDouble.add(proessQueue);
		monitorDouble.add(proessOther);
		monitorDouble.add(monitorOne);
		
	    Enumeration<Driver> drivers = DriverManager.getDrivers();
	    while (drivers.hasMoreElements()) {
	      Driver driver = drivers.nextElement();
	      registeredDrivers.put(driver.getClass().getName(), driver);
	    }
	}
	
	
	private String dataSourceName;
	private Properties properties;
	private IObjectPool<Connection> objectPool;
	
	public DataSourceDefault(String dataSourceName,Properties properties)throws Exception{
		this.dataSourceName = dataSourceName;
		this.properties = properties;
		this.init();
	}
	
	private void init()throws Exception{
		final PoolProperties poolProperties = this.createPoolProperties(this.properties);
		this.initDriver(poolProperties.getDriver());
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
		this.objectPool = new ObjectPool<Connection>(this.dataSourceName , DataSourceDefault.proessQueue, DataSourceDefault.proessOther,  DataSourceDefault.proessOther,  DataSourceDefault.proessOther, objectFactory, poolProperties);
		DataSourceDefault.proessQueue.start();
		DataSourceDefault.proessOther.start();
	}
	
	
	private PoolProperties createPoolProperties(Properties properties)throws Exception{
		PoolProperties poolProperties = new PoolProperties();
		Field[] fieldArray = PoolProperties.class.getDeclaredFields();
		if(fieldArray!=null&&fieldArray.length>0){
			for(int i=0;i<fieldArray.length;i++){
				Field field=fieldArray[i];
				field.setAccessible(true);
				String name = field.getName();
				String value = properties.getProperty(name);
				if(StringUtils.isBlank(value)){
					throw new Exception("properties can not find :"+name);
				}
				field.set(poolProperties, this.convertValue(field.getType(), value));
			}
		}
		return poolProperties;
	}
	
	private Object convertValue(Class<?> targetType, String value) {
	    Object convertedValue = value;
	    if (targetType == Integer.class || targetType == int.class) {
	      convertedValue = Integer.valueOf(value);
	    } else if (targetType == Long.class || targetType == long.class) {
	      convertedValue = Long.valueOf(value);
	    } else if (targetType == Boolean.class || targetType == boolean.class) {
	      convertedValue = Boolean.valueOf(value);
	    }
	    return convertedValue;
	}
	
	private void initDriver(String jdbcdriver)throws Exception{
		if(registeredDrivers.get(jdbcdriver)==null){
			Driver driverObject = (Driver) Class.forName(jdbcdriver).newInstance();
			DriverManager.registerDriver(driverObject);
			registeredDrivers.put(jdbcdriver, driverObject);
		}
	}

	public PrintWriter getLogWriter() throws SQLException {
		return DriverManager.getLogWriter();
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		DriverManager.setLogWriter(out);
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		DriverManager.setLoginTimeout(seconds);
	}

	public int getLoginTimeout() throws SQLException {
		return DriverManager.getLoginTimeout();
	}

	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	public Connection getConnection() throws SQLException {
		return this.getConn();
	}

	public Connection getConnection(String username, String password)
			throws SQLException {
		return this.getConn();
	}
	
	private Connection getConn()throws SQLException{
		Connection conn = null;
		try {
			conn = this.objectPool.getObject();
		} catch (Exception e) {
			e.printStackTrace();
			throw new SQLException(e.getMessage());
		}
		return conn;
	}
	
}
