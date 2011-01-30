package org.apache.nutch.watchlist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;

public class InitDatabase {
    /**                                                                                                                                                                                                         
     * Runs an SQL script using the                                                                                                                                         
     * connection passed in.
     * It can only process MySql script for now.                                                                                                                                                                                   
     * @throws SQLException 
     **/
    private static void runSqlScript(Connection con, String script) throws SQLException {
        BufferedReader in = null;
        Statement statement = con.createStatement();;
        int totalLines = 0;
        try {
            try {
                String line;
                StringBuffer command = null;
                in = new BufferedReader(new FileReader(script));
                while ((line = in.readLine()) != null) {
                    if (command == null) {
                        command = new StringBuffer();
                        }
                    totalLines++;
                    String trimmedLine = line.trim();
                    if (trimmedLine.startsWith("--")) {
                        // Do nothing for comment lines
                        } else if (trimmedLine.endsWith(";")) {
                            // It is time to execute
                            command.append(trimmedLine);
                            statement.execute(command.toString());
                            command = null;
                        } else {
                            // in the middle of a command
                            command.append(trimmedLine).append("\n");
                        }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
                }
                if (statement != null) {
                    statement.close();
                }
            }
        } catch (IOException e) {
            totalLines = -1;
        }
        System.out.println("processed " + totalLines + " lines in " + script);
    }

    private static void initMySqlDatabase(Configuration conf, String initScriptPath)
            throws Exception {
        // Register the JDBC driver for MySQL.
        Connection connection = null;

        try {
            String driver = conf.get(WatchListConfig.JDBC_DRIVER.getAttributeString(),
                    "com.mysql.jdbc.Driver");
            Class.forName(driver).newInstance();
            // Define URL of database server for
            // database.
            String url = conf.get(WatchListConfig.JDBC_URL.getAttributeString(),
                    "jdbc:mysql://localhost:3306/watchlist");

            String user = conf.get(WatchListConfig.JDBC_USER.getAttributeString(), "watchlist");
            String password = conf.get(WatchListConfig.JDBC_PASSWORD.getAttributeString(), "wrmfw");
            connection = DriverManager.getConnection(url, user, password);
            runSqlScript(connection, initScriptPath);

        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        } finally {

            if (connection != null) {
                connection.close();
            }
        }

    }

    public static void main(String[] args) {
        String sqlScriptPath = null;
        if (args.length < 1) {
            System.err.println("Usage: InitDatabase <script_path>");
            System.exit(-1);
        }
        sqlScriptPath = args[0];
        Configuration conf = NutchConfiguration.create();
        try {
            initMySqlDatabase(conf, sqlScriptPath);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

}
