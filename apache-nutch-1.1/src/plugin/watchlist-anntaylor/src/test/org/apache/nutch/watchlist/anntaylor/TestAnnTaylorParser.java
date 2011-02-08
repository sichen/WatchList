/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nutch.watchlist.anntaylor;

import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseUtil;
import org.apache.nutch.protocol.Content;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.NutchConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.*;
import java.net.URL;

import junit.framework.TestCase;

/**
 * Test lots of offline jcrew product pages
 * Since there are too many test cases, and I often got Java OutOfMemory Exception
 * I divided tests into several separate test function
 * 
 * @author Jun Yang
 */
public class TestAnnTaylorParser extends TestCase {

    private static final File testDir = new File(System.getProperty("test.input"));
    private int num = 0;

    public void testPages() throws Exception {
	System.out.println("Hello World");
	//	pageTest(new File(testDir, "30313.jsp"), "Sleeveless Tie Front Top", "http://www.anntaylor.com/catalog/product.jsp?productId=30313&N=1200005&pCategoryId=3939&categoryId=183");
    }

    public void pageTest(File file, String productName, String url) throws Exception {

        String contentType = "text/html";
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
        byte[] buffer = new byte[1024];
        int i;
        while ((i = in.read(buffer)) != -1) {
            out.write(buffer, 0, i);
        }
        in.close();
        byte[] bytes = out.toByteArray();
        Configuration conf = NutchConfiguration.create();

        Content content = new Content(url, url, bytes, contentType, new Metadata(), conf);
        Parse parse = new ParseUtil(conf).parseByExtensionId("parse-html", content).get(url);

        Metadata metadata = parse.getData().getContentMeta();
        assertEquals(productName, metadata.get(AnnTaylorParser.META_PRODUCT_TITLE));
	/*
        float price = Float.parseFloat(metadata.get(AnnTaylorParser.META_PRICE));
        assertTrue(price > 0);
        assertTrue(metadata.get(AnnTaylorParser.META_IMG_URL).length() > 10);
	*/
        // Indicate progress
        System.out.print('.');
        num++;
    }
}
