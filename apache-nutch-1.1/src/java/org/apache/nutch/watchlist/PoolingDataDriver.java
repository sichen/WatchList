package org.apache.nutch.watchlist;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

//                                                                                                                                       
// Here are the dbcp-specific classes.                                                                                                   
// Note that they are only used in the setupDriver                                                                                       
// method. In normal use, your classes interact                                                                                          
// only with the standard JDBC API                                                                                                       
//                                                                                                                                       
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.hadoop.conf.Configuration;

/*
 * The PollingDataDriver is a wrapper over JDBC Driver, it implements Connection Pooling.
 * 
 * Connection pool is a cache of database connections maintained so that the connections 
 * can be reused when future requests to the database are required. Connection pools are
 * used to enhance the performance of executing commands on a database.
 * 
 */

public class PoolingDataDriver {
    public static final Log LOG = LogFactory.getLog(PoolingDataDriver.class);
    private static final String jdbcDriver = "com.mysql.jdbc.Driver";
    private static final String POOL_NAME = "watchlist";
    private static String url;
    public PoolingDataDriver(Configuration conf) {
        url = conf.get(WatchListConfig.JDBC_URL.getAttributeString(),
                "jdbc:mysql://localhost:3306/junkdb");
        LOG.info("Loading underlying JDBC driver.");
        try {
            Class.forName(jdbcDriver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //                                                                                                                               
        // Then we set up and register the PoolingDriver.                                                                                
        // Normally this would be handled auto-magically by                                                                              
        // an external configuration, but in this example we'll                                                                          
        // do it manually.                                                                                                               
        //                                                                                                                               
        LOG.info("Setting up driver.");
        try {
            setupDriver(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info("PoolingDataDriver Initialization Done.");
    }
    
    public static void setupDriver(String connectURI) throws Exception {
        //                                                                                                                               
        // First, we'll need a ObjectPool that serves as the                                                                             
        // actual pool of connections.                                                                                                   
        //                                                                                                                               
        // We'll use a GenericObjectPool instance, although                                                                              
        // any ObjectPool implementation will suffice.                                                                                   
        //                                                                                                                               
        ObjectPool connectionPool = new GenericObjectPool(null);

        //                                                                                                                               
        // Next, we'll create a ConnectionFactory that the                                                                               
        // pool will use to create Connections.                                                                                          
        // We'll use the DriverManagerConnectionFactory,                                                                                 
        // using the connect string passed in the command line                                                                           
        // arguments.                                                                                                                    
        //                                                                                                                               
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI,null);
        //                                                                                                                               
        // Now we'll create the PoolableConnectionFactory, which wraps                                                                   
        // the "real" Connections created by the ConnectionFactory with                                                                  
        // the classes that implement the pooling functionality.                                                                         
        //                                                                                                                               
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);

        //                                                                                                                               
        // Finally, we create the PoolingDriver itself...                                                                                
        //                                                                                                                               
        Class.forName("org.apache.commons.dbcp.PoolingDriver");
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");

        //                                                                                                                               
        // ...and register our pool with it.                                                                                             
        //                                                                                                                               
        driver.registerPool(POOL_NAME, connectionPool);

        //                                                                                                                               
        // Now we can just use the connect string "jdbc:apache:commons:dbcp:watchlist"                                                     
        // to access our pool of Connections.                                                                                            
        //                                                   
    }
    
    public static void printDriverStats() throws Exception {
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        ObjectPool connectionPool = driver.getConnectionPool(POOL_NAME);

        System.out.println("NumActive: " + connectionPool.getNumActive());
        System.out.println("NumIdle: " + connectionPool.getNumIdle());
    }

    public static void shutdownDriver() throws Exception {
        PoolingDriver driver = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver.closePool(POOL_NAME);
    }

}
