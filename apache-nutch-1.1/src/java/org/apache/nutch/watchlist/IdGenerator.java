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

    /** Generate an ID without parameters*/
    public long generate();
    /** Generate and ID with a table name*/
    public long generate(String tableName);
}
