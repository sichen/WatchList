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

// JDK imports
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

// Commons Logging imports
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// Nutch imports
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseResult;
import org.apache.nutch.parse.HtmlParseFilter;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.parse.ParseText;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.util.StringUtil;

/// Hadoop imports
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;

/**
 * Parse Ann Taylor product pages, and extract product info.
 *
 * @author Jun Yang
 */
public class AnnTaylorParser implements HtmlParseFilter {

    // http://www.anntaylor.com/catalog/product.jsp?productId=28307&viewAll=true&Ns=SALE_SEQUENCE_NO&categoryId=3944&Nr=AND%28CAT_SALE_FLAG:Y%29&loc=SLP&N=1200040&defaultColor=Black%20Raspberry&defaultSizeType=Petite
    // TODO(juny78): add the parsing of categories
    private static final String PRODUCT_URL_PATTERN = "http\\:\\/\\/www\\.anntaylor\\.com\\/catalog\\/product.jsp\\?productId=(\\d+).*"; 
    //    private static final String PRICE_PATTERN = ;
 
    private static final Pattern PRODUCT_URL_REGEXP = Pattern.compile(PRODUCT_URL_PATTERN);
    // private static final Pattern PRICE_REGEXP = Patern.compile(PRICE_PATTERN);

    // The meta tags we are going to add into index
    public static final String META_PAGE_TITLE = "pageTitle";
    public static final String META_BRAND = "brand";
    public static final String META_ITEMNUM = "itemNum";
    public static final String META_PRODUCT_TITLE = "productTitle";
    public static final String META_PRICE = "price";
    public static final String META_IMG_URL = "imgURL";

    public final static Log LOG = LogFactory.getLog(AnnTaylorParser.class);
    private Configuration conf = null;
    
    /**
     * Scan the HTML document looking at product information
     */
    public ParseResult filter(Content content,
			      ParseResult parseResult,
			      HTMLMetaTags metaTags,
			      DocumentFragment doc) {
	String itemNumber;
	URL url;
	LOG.info("Start parsing!");
	try {
	    url = new URL(content.getUrl());
	    LOG.info("Start parsing: " + url);
	    Matcher matcher = PRODUCT_URL_REGEXP.matcher(url.toString());
	    if (matcher.find()) {
		int productID = Integer.parseInt(matcher.group(1));
		itemNumber = Integer.toString(productID);
		LOG.info("Parsed the AnnTaylor product ID: " + itemNumber);
	    } else {
                // matcher cannot find it
                throw new MalformedURLException("URL is not a AnnTaylor Product Page.");
            }
	} catch (Exception e) {
	    return parseResult;
	}
	
	try {
	    Parser parser = new Parser(doc, itemNumber);
	} catch (Exception e) {
	    if (LOG.isWarnEnabled()) {
		LOG.warn("Failed to AnnTaylor product page: " + url + ": " + e);
		e.printStackTrace();
	    }
	    return parseResult;
	}
	
	// Finally, log the finish of parsing this product
	LOG.info("Finished parsing AnnTaylor product: " + itemNumber);
	return parseResult;
    }
    
    private static class Parser {
	
	String itemNumber = null;
	String productTitle = null;
	String price = null;
	String imageURL = null;
	    
	Parser(Node node, String itemNum) {
	    itemNumber = itemNum;
	    parse(node);
	}

	String getProductTitle() {
            return productTitle;
        }

        String getPrice() {
            return price;
        }

        String getImageURL() {
            return imageURL;
        }

	void parse(Node node) {
	    // Checks if this is a <div class="hd-info">
	    if (checkNodeWithAttr(node, Node.ELEMENT_NODE, "div", "class", "hd-info")) {
		NodeList childNodes = node.getChildNodes();
		
		// The first child node should be a 'h1' node for the product title
		if (checkNode(childNodes.item(0), Node.ELEMENT_NODE, "h1", null)) {
		    productTitle = childNodes.item(0).getNodeValue();
		    LOG.info("Got the price for AnnTaylor product: " + itemNumber + ":" + price);
		}
		    
		// The second child node is a <div class='price'>, and it should have a child
		// as <p class='price'>, which in turn has two children as <sup class='dollars'>
		// and the price number
		if (checkNodeWithAttr(childNodes.item(1), Node.ELEMENT_NODE, "div", "class", "price")) {
		    Node priceNode = childNodes.item(1).getFirstChild();
		    if (checkNodeWithAttr(priceNode, Node.ELEMENT_NODE, "p", "class", "price")) {
			if (checkNodeWithAttr(priceNode.getFirstChild(), Node.ELEMENT_NODE, "sup", "class", "dollars") &&
			    checkNode(priceNode.getLastChild(), Node.TEXT_NODE, null, null)) {
			    price = priceNode.getLastChild().getNodeValue();
			    LOG.info("Got the price for AnnTaylor product: " + itemNumber + ":" + price);
			}
		    }
		}
	    }
	    
	    // Recurse
            NodeList children = node.getChildNodes();
            for (int i = 0; children != null && i < children.getLength(); i++) {
                parse(children.item(i));
            }
	}

	Boolean checkNode(Node node, short type, String name, String value) {
	    return
		node != null &&
		node.getNodeType() == type &&
		(name == null || name.equalsIgnoreCase(node.getNodeName())) &&
		(value == null || value.equalsIgnoreCase(node.getNodeValue()));
	}

	Boolean checkNodeWithAttr(Node node, short type, String name, String attrName, String attrValue) {
	    if (checkNode(node, type, name, null) == false) {
		return false;
	    }
	    Node attr = node.getAttributes().getNamedItem(attrName);
	    return checkNode(attr, Node.ATTRIBUTE_NODE, null, attrValue);
	}

    }
 
    /*
     * ----------------------------- * <implementation:Configurable> *
     * -----------------------------
     */
    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    public Configuration getConf() {
        return this.conf;
    }

    /*
     * ------------------------------ * </implementation:Configurable> *
     * ------------------------------
     */
}