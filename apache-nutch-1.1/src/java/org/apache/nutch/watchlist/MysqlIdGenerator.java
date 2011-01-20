package org.apache.nutch.watchlist;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.conf.Configuration;

public class MysqlIdGenerator implements IdGenerator {

    private static String url;
    private static Connection connection;
    private static PreparedStatement insertStmt;
 
    public MysqlIdGenerator(Configuration conf) {
        //Register the JDBC driver for MySQL.
        try {
            String driver = conf.get(JDBC_DRIVER, "com.mysql.jdbc.Driver");
            LOG.info("Registering the JDBC driver for MySQL.");
            Class.forName(driver).newInstance();
            // Define URL of database server for
            // database.
            url = conf.get(JDBC_URL, "jdbc:mysql://localhost:3306/junkdb");
            LOG.info("JDBC url: " + url);
            String user = conf.get(JDBC_USER, "watchlist");
            String password = conf.get(JDBC_PASSWORD, "wrmfw");
            connection = DriverManager.getConnection(
                    url, user, password);
            String table = conf.get(JDBC_TABLE, "object");
            insertStmt = connection.prepareStatement(
                    "insert into " + table + "(orig_table) values(?)",
                    Statement.RETURN_GENERATED_KEYS);
            LOG.error("JDBC connected successfully.");

        } catch (SQLException e) {
            LOG.error("SQLException: " + e.getMessage());
            LOG.error("SQLState: " + e.getSQLState());
            LOG.error("VendorError: " + e.getErrorCode());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
   
    @Override
    public long generate() {
        long i = -1;
        try {
            insertStmt.setString(1, "unknown");
            insertStmt.executeUpdate();
            ResultSet rs = null;
            rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                i = rs.getLong(1);
                LOG.info("Generated ID: " + i);
            } else {
                LOG.error("Failed to generate ID");
            }
            rs.close();
            rs = null;
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            return i;
        }

    }

    @Override
    public long generate(String tableName) {
        long i = -1;
        try {
            insertStmt.setString(1, tableName.trim());
            insertStmt.executeUpdate();
            ResultSet rs = null;
            rs = insertStmt.getGeneratedKeys();
            if (rs.next()) {
                i = rs.getLong(1);
                LOG.info("Generated ID: " + i);
            } else {
                LOG.error("Failed to generate ID");
            }
            rs.close();
            rs = null;
        } catch (Exception e) {
            LOG.error(e.getMessage());
        } finally {
            return i;
        }

    }

}
