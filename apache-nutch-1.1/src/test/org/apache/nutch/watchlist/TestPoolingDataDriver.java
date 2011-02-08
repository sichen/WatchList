package org.apache.nutch.watchlist;

import java.sql.Connection;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;

public class TestPoolingDataDriver extends TestCase {
    private static Configuration conf = NutchConfiguration.create();
    Connection connection = null;
    
    public void testGeneration() {
    }
}
