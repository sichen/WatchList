package org.apache.nutch.watchlist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.nutch.fetcher.Fetcher;



/** IdGenerator generates ID for all the database rows to use as primary key.
 *  The generated ID is guaranteed to be unique across the whole system.
 *  Since we are using long type, we actually get 63 bits, because long is signed.
 */
public interface IdGenerator {
    /** The name of the IdGenerator. */
    final static String NAME = IdGenerator.class.getName();
    public static final Log LOG = LogFactory.getLog(IdGenerator.class);

    public static final String JDBC_DRIVER =
      "watchlist.idgenerator.jdbc.driver";
    public static final String JDBC_URL =
        "watchlist.idgenerator.jdbc.url";
    public static final String JDBC_USER =
        "watchlist.idgenerator.jdbc.user";
    public static final String JDBC_PASSWORD =
        "watchlist.idgenerator.jdbc.password";
    public static final String JDBC_TABLE =
        "watchlist.idgenerator.jdbc.table";

    /** Generate an ID without parameters*/
    public long generate();
    /** Generate and ID with a table name*/
    public long generate(String tableName);
}
