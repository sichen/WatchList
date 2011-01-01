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

package org.apache.nutch.watchlist.jcrew;

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
 * 
 * @author Si Chen
 */
public class TestJCrewParser extends TestCase {

    private static final File testDir = new File(System.getProperty("test.input"));

    public void testPages() throws Exception {
        pageTest(new File(testDir, "72977.jsp"), "Broken-in pocket crewneck tee",
                "http://www.jcrew.com/mens_category/polostees/brokeninjersey/PRDOVR~72977/72977.jsp");

        pageTest(new File(testDir, "29234.jsp"), "Cotton-cashmere crewneck sweater",
                "http://www.jcrew.com/mens_category/sweaters/cottoncashmere/PRDOVR~29234/29234.jsp");
        
        pageTest(new File(testDir, "33807.jsp"), "Harbour paisley ruched tank",
        "http://www.jcrew.com/womens_category/swim/patternsandprints/PRDOVR~33807/33807.jsp");
        
        pageTest(new File(testDir, "32453.jsp"), "Solid Sundrine flannel shirtdress",
        "http://www.jcrew.com/womens_category/dresses/day/PRDOVR~32453/32453.jsp");
        
        pageTest(new File(testDir, "32940.jsp"), "Girls' bateau tee",
        "http://www.jcrew.com/girls_feature/NewArrivals/knitstees/PRDOVR~32940/32940.jsp");
        
        pageTest(new File(testDir, "99215.jsp"), "Girls' solid tights",
        "http://www.jcrew.com/girls_feature/NewArrivals/jewelryaccessories/PRDOVR~99215/99215.jsp");
        
        pageTest(new File(testDir, "92002.jsp"), "Boys' Italian chino sportcoat",
        "http://www.jcrew.com/boys_category/jacketsandvests/jackets/PRDOVR~92002/92002.jsp");
        
        pageTest(new File(testDir, "37905.jsp"), "Sailor sweater",
        "http://www.jcrew.com/womens_category/sweaters/crewnecksandvnecks/PRDOVR~37905/37905.jsp");
        
        pageTest(new File(testDir, "35905.jsp"), "Indian voile boy shirt",
        "http://www.jcrew.com/womens_category/shirtstops/casualshirts/PRDOVR~35905/35905.jsp");
        
        pageTest(new File(testDir, "29521.jsp"), "Lacey top",
        "http://www.jcrew.com/womens_category/shirtstops/jcrewcollection/PRDOVR~29521/29521.jsp");
        
        pageTest(new File(testDir, "35351.jsp"), "Vintage cotton mixed lawn shirt",
        "http://www.jcrew.com/womens_category/knitstees/longsleevetees/PRDOVR~35351/35351.jsp");
        
        pageTest(new File(testDir, "33418.jsp"), "Always pocket cardigan",
        "http://www.jcrew.com/womens_category/jcrewweekend/PRDOVR~33418/33418.jsp");
        
        pageTest(new File(testDir, "18007.jsp"), "Boys' yarn-dyed cotton boxers",
        "http://www.jcrew.com/boys_category/boxers/wovenboxers/PRDOVR~18007/ENE~1+2+3+22+4294967294+20~~~20+18~15~~~~~~~/18007.jsp");
        
        pageTest(new File(testDir, "18212.jsp"), "Boys' print cotton boxers",
        "http://www.jcrew.com/AST/Navigation/Sale/AllProducts/PRDOVR~18212/99102181471/ENE~1+2+3+22+4294967294+20~~~20+18~15~~~~~~~/18212.jsp");
        
       
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
        assertEquals(productName, metadata.get(JCrewParser.META_PRODUCT_TITLE));
        float price = Float.parseFloat(metadata.get(JCrewParser.META_PRICE));
        assertTrue(price > 0);

    }
}
