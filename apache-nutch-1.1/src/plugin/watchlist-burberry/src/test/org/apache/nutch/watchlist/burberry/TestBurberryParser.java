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

package org.apache.nutch.watchlist.burberry;

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
public class TestBurberryParser extends TestCase {

    private static final File testDir = new File(System.getProperty("test.input"));
    private int num = 0;

    public void testPages() throws Exception {
	pageTest(new File(testDir, "37400701001.htm"),
		 "http://us.burberry.com/store/womenswear/sport/view-all/reflective-graphic-t-shirt/sku-37400701001-reflective-graphic-t-shirt/",
		 "reflective graphic t shirt",
		 "37400701001",
		 10.0f);
		 
	pageTest(new File(testDir, "37566651.html"),
		 "http://us.burberry.com/store/womens-accessories/bags/the-show/bridle-patent-clutch-bag-with-strap/sku-37566651-bridle-patent-clutch-bag-with-strap/",
		 "bridle patent clutch bag with strap",
		 "37566651",
		 10.0f);

	pageTest(new File(testDir, "37304821001.html"),
		 "http://us.burberry.com/store/childrens/girls/2-6-years-clothing/exploded-check-dress/sku-37304821001-exploded-check-dress/",
		 "exploded check dress",
		 "37304821001",
		 10.0f);

	pageTest(new File(testDir, "37437071.html"),
		 "http://us.burberry.com/store/home-gifts/home/hand-knit-cashmere-hot-water-bottle-cover/sku-37437071-hand-knit-cashmere-hot-water-bottle-cover/",
		 "hand knit cashmere hot water bottle cover",
		 "37437071",
		 10.0f);

	pageTest(new File(testDir, "36624901.html"),
		 "http://us.burberry.com/store/home-gifts/men/burberry-sport-chronograph-watch/sku-36624901-burberry-sport-chronograph-watch/",
		 "burberry sport chronograph watch",
		 "36624901",
		 10.0f);

	pageTest(new File(testDir, "37480461.html"),
		 "http://us.burberry.com/store/beauty-fragrance/eyes/eye-definer/midnight-ash-no03/sku-37480461-midnight-ash-no03/",
		 "midnight ash no03",
		 "37480461",
		 10.0f);
		
    }

    public void pageTest(File file, String url, String productName, String productID, float productPrice)
	throws Exception {

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

        // Parse the test page
	Content content = new Content(url, url, bytes, contentType, new Metadata(), conf);
        Parse parse = new ParseUtil(conf).parseByExtensionId("parse-html", content).get(url);
        Metadata metadata = parse.getData().getContentMeta();

	// Check the product name, ID, and price
	assertTrue(productName.equalsIgnoreCase(metadata.get(BurberryParser.META_NAME)));
	assertEquals(productPrice,
		     Float.parseFloat(metadata.get(BurberryParser.META_PRICE)),
		     0.0001);
	assertEquals(productID, metadata.get(BurberryParser.META_ID));
        assertTrue(metadata.get(BurberryParser.META_IMG_URL).length() > 10);
       
        // Indicate progress
        System.out.print('.');
        num++;
    }
}
