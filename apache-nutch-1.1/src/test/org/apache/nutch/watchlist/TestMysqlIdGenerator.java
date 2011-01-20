package org.apache.nutch.watchlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.EncodingDetector;
import org.apache.nutch.util.NutchConfiguration;

import junit.framework.TestCase;

public class TestMysqlIdGenerator extends TestCase {
    private static Configuration conf = NutchConfiguration.create();
    private boolean mysqlInstalled = false;
    Connection connection = null;

    protected void setUp() {
        Statement statement = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/mysql", "watchlist", "wrmfw");
            statement = connection.createStatement();
            String junitSQL = "CREATE DATABASE junit";
            statement.executeUpdate(junitSQL);
            junitSQL = "USE junit";
            statement.executeUpdate(junitSQL);
            junitSQL = "create table object(id BIGINT PRIMARY KEY AUTO_INCREMENT, orig_table VARCHAR(50))";
            statement.executeUpdate(junitSQL);
            statement.close();
            statement = null;
            mysqlInstalled = true;
            
        } catch (Exception ex) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }finally {
            if (statement != null) {
                try {
                  statement.close();
                } catch (SQLException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }
    
    protected void tearDown() {

            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost/mysql", "watchlist", "wrmfw");

                Statement statement = connection.createStatement();
                String junitSQL = "DROP DATABASE junit";
                statement.executeUpdate(junitSQL);
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

    }
    
    public void testGeneration() {
        if (mysqlInstalled != true) {
            fail("Please make sure MySQL is correctly installed.\n" +
            	 "For details please go to: https://sites.google.com/site/watchlistdocs/home/technical-document/install-mysql-on-ubuntu");
        }
        
        // Prepare the configuration
        conf.set(IdGenerator.JDBC_DRIVER, "com.mysql.jdbc.Driver");
        conf.set(IdGenerator.JDBC_URL, "jdbc:mysql://localhost:3306/junit");
        conf.set(IdGenerator.JDBC_USER, "watchlist");
        conf.set(IdGenerator.JDBC_PASSWORD, "wrmfw");
        conf.set(IdGenerator.JDBC_TABLE, "object");
        
        IdGenerator gen = new MysqlIdGenerator(conf);
        long result = gen.generate();
        assertTrue(result == 1);
        result = gen.generate("have fun");
        assertTrue(result == 2);
        result = gen.generate(" ");
        assertTrue(result == 3);
        
    }
}
