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
import org.w3c.dom.Text;
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

// Hadoop imports
import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.io.Text;

/**
 * 
 * Parse Burberry product pages, and extract product information
 * 
 * @author Jun Yang
 */
public class BurberryParser implements HtmlParseFilter {

    // The URL Pattern for matching Burberry Product Page.
    // http://us.burberry.com/store/womenswear/sport/view-all/reflective-graphic-t-shirt/sku-37400701001-reflective-graphic-t-shirt/
    private static final String PRODUCT_URL_PATTERN = "http\\:\\/\\/us\\.burberry\\.com\\/.*\\/(.+)\\/sku-(\\d+)-(.+)\\/";
    private static final String PRICE_PATTERN = "$(\\d+\\.\\d+)";

    private static final Pattern urlRegexp = Pattern.compile(PRODUCT_URL_PATTERN);
    private static final Pattern priceRegexp = Pattern.compile(PRICE_PATTERN);

    // The meta tags we are going to add into index
    public static final String META_BRAND = "brand";
    public static final String META_ID    = "id";
    public static final String META_NAME  = "name";
    public static final String META_PRICE = "price";
    public static final String META_IMG_URL = "imgURL";

    // If it is a true product page, the html title has very valuable
    // information like Category, Brand, Product Title, or anything the
    // seller wants to tell. We add it as a meta field, so we can boost
    // its search score.
    public static final String META_PAGE_TITLE = "title";

    public final static Log LOG = LogFactory.getLog(BurberryParser.class);
    private Configuration conf = null;

    /**
     * Scan the HTML document looking at product information
     */
    public ParseResult filter(Content content,
			      ParseResult parseResult,
			      HTMLMetaTags metaTags,
			      DocumentFragment doc) {

        // Step 1: ignore non-product pages from Burberry
	String itemNumber = null;
	String productTitle = null;
        URL baseURL;
        try {
            baseURL = new URL(content.getBaseUrl());
	    LOG.info("Start parsing: " + baseURL.toString());

            Matcher matcher = urlRegexp.matcher(baseURL.toString());
            if (matcher.find()) {
		if (!matcher.group(1).equalsIgnoreCase(matcher.group(3))) {
		    throw new MalformedURLException("Burberry product title doesn't match.");
		}
		itemNumber = matcher.group(2);
		productTitle = matcher.group(3).replace('-', ' ');
	    } else {
                // matcher cannot find it, so this is not a burberry product page
                throw new MalformedURLException("URL is not a Burberry Product Page.");
            }
        } catch (Exception e) {
	    // Print out the message and move on to the next filter
	    LOG.info("Exception: " + e.getMessage());
            return parseResult;
        }

	// Step 2: Extract the product details (image URL and price)
	LOG.info("Parse Burberry product " + itemNumber + " (" + productTitle + ")");
	try {
            Parser parser = new Parser(doc, itemNumber);
            String price = parser.getPrice();
            String imgURL = parser.getImageURL();
            if (price != null && imgURL != null) {
                // Parse successful, fill out the metadata
                Parse parse = parseResult.get(content.getUrl());
                Metadata meta = parse.getData().getContentMeta();
                meta.add(META_BRAND, "burberry");
                meta.add(META_ID, itemNumber);
                meta.add(META_NAME, productTitle);
                meta.add(META_PRICE, price);
                meta.add(META_IMG_URL, imgURL);
                meta.add(META_PAGE_TITLE, parse.getData().getTitle());

                // TODO(jyang): Write these information into database
            } else {
                // At least something wrong
		LOG.warn("Partially parsed " + baseURL + ": ");
		if (price == null) {
		    LOG.warn("price is null.");
		}
		if (imgURL == null) {
		    LOG.warn("imgURL is null.");
		}
            }
        } catch (Exception e) {
	    LOG.warn("Failed to parse " + baseURL + ": " + e);
	    e.printStackTrace();
            return parseResult;
        }

        // Finally, log the finish of parsing this product
        LOG.info("Finish parsing Burberry product: " + itemNumber);
	return parseResult;
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

    // A private class for parsing Burberry product page
    private static class Parser {
        String itemNumber = null;
        String price = null;
        String imageURL = null;

        Parser(Node node, String itemNum) {
            itemNumber = itemNum;
            parse(node);
        }

        String getPrice() {
            return "10";
        }

        String getImageURL() {
            return imageURL;
        }

        void parse(Node node) {
	    if (checkElementNodeWithClass(node, "span", "product-price-amount")) {
		node.normalize();
		if (node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
		    Node priceNode = node.getFirstChild();
		    Matcher matcher = priceRegexp.matcher(priceNode.getNodeValue());
		    if (matcher.find()) {
			price = matcher.group(1);
			LOG.info("Price for Burberry product " + itemNumber + ": " + price);
		    }
		}
	    } else if (checkElementNodeWithClass(node, "div", "product-image")) {
		// Extract the image URL of the item
		Node anchorNode = getNamedChild(node, "a");
		Node imgNode = getNamedChild(node, "img");
		if (imgNode == null && anchorNode != null) {
		    imgNode = getNamedChild(anchorNode, "img");
		}
		if (imgNode != null) {
		    String imgURL = getNamedAttribute(imgNode, "src");
		    if (imgURL != null) {
			imageURL = imgURL;
			LOG.info("Image URL for Burberry product " + itemNumber + ": " + imageURL);
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

	Boolean checkElementNode(Node node, String name) {
	    return checkNode(node, Node.ELEMENT_NODE, name, null);
	}

	Boolean checkNodeWithAttr(Node node, short type, String name, String attrName, String attrValue) {
	    if (checkNode(node, type, name, null) == false) {
		return false;
	    }
	    Node attr = node.getAttributes().getNamedItem(attrName);
	    return checkNode(attr, Node.ATTRIBUTE_NODE, null, attrValue);
	}

	Boolean checkNodeWithClass(Node node, short type, String name, String attrValue) {
	    return checkNodeWithAttr(node, type, name, "class", attrValue);
	}

	Boolean checkElementNodeWithClass(Node node, String name, String attrValue) {
	    return checkNodeWithClass(node, Node.ELEMENT_NODE, name, attrValue);
	}

	Node getChildWithTypeAndName(Node parent, short type, String name) {
	    NodeList children = parent.getChildNodes();
	    for (int i = 0; i < children.getLength(); i++) {
		if (children.item(i).getNodeType() == type &&
		    (name == null || name.equalsIgnoreCase(children.item(i).getNodeName()))) {
		    return children.item(i);
		}
	    }
	    return null;
	}
    
	Node getNamedChild(Node parent, String name) {
	    return getChildWithTypeAndName(parent, Node.ELEMENT_NODE, name);
	}
    
	String getNamedAttribute(Node node, String attrName) {
	    if (node.getNodeType() != Node.ELEMENT_NODE) {
		return null;
	    }
	    Node attrImgSrc = node.getAttributes().getNamedItem("src");
	    if (attrImgSrc == null) {
		return null;
	    }
	    return attrImgSrc.getNodeValue();
	}
    }
}
