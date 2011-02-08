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
import org.apache.nutch.watchlist.IdGenerator;
import org.apache.nutch.watchlist.MysqlIdGenerator;

// Hadoop imports
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;

/**
 * 
 * Parse JCrew product pages, and extract product information
 * 
 * @author Si Chen
 */
public class JCrewParser implements HtmlParseFilter {

    // The URL Pattern for matching JCrew Product Page.
    // JCrew Product Page has the form like:
    // http://www.jcrew.com/mens_category/sweaters/cottoncashmere/PRDOVR~29234/29234.jsp
    // Sometimes, items on sale can have weird URL like below:
    // http://www.jcrew.com/boys_category/boxers/wovenboxers/PRDOVR~18007/ENE~1+2+3+22+4294967294+20~~~20+18~15~~~~~~~/18007.jsp
    // "http://www.jcrew.com/AST/Navigation/Sale/AllProducts/PRDOVR~18212/99102181471/ENE~1+2+3+22+4294967294+20~~~20+18~15~~~~~~~/18212.jsp"
    private static final String JCREW_PRODUCT_URL_PATTERN = "http\\:\\/\\/www\\.jcrew\\.com\\/.*\\/PRDOVR~(\\d+).*\\/(\\d+)\\.jsp";
    private static final String JCREW_PRICE_PATTERN = "(\\d+\\.\\d+).*";
    private static final Pattern urlRegexp = Pattern.compile(JCREW_PRODUCT_URL_PATTERN);
    private static final Pattern priceRegexp = Pattern.compile(JCREW_PRICE_PATTERN);

    // The meta tags we are going to add into index
    public static final String META_BRAND = "brand";
    public static final String META_ITEMNUM = "itemNumber";
    public static final String META_PRODUCT_TITLE = "productTitle";
    public static final String META_PRICE = "price";
    public static final String META_IMG_URL = "imgURL";
    // If it is a true product page, the html title has very valuable
    // information
    // like Category, Brand, Product Title, or anything the seller wants to
    // tell.
    // We add it as a meta field, so we can boost its search score.
    public static final String META_JCREW_TITLE = "jcrew";

    public final static Log LOG = LogFactory.getLog(JCrewParser.class);
    private Configuration conf = null;
    private IdGenerator idGenerator = null;

    /**
     * Scan the HTML document looking at product information
     */
    public ParseResult filter(Content content, ParseResult parseResult, HTMLMetaTags metaTags,
            DocumentFragment doc) {

        String itemNumber = null;

        // Step 1: ignore non-product pages from JCrew
        URL base;
        try {
            base = new URL(content.getBaseUrl());
            if (LOG.isInfoEnabled()) {
                LOG.info("Start parsing: " + base.toString());
	    }
            Matcher matcher = urlRegexp.matcher(base.toString());
            if (matcher.find()) {
                int major, minor;
                major = Integer.parseInt(matcher.group(1));
                minor = Integer.parseInt(matcher.group(2));
                if (major != minor) {
                    throw new MalformedURLException("URL Product Item number doesn't match.");
                } else {
                    // We got the Product Item number from the URL
                    itemNumber = Integer.toString(major);
                }
            } else {
                // matcher cannot find it
                throw new MalformedURLException("URL is not a JCrew Product Page.");
            }

        } catch (Exception e) {

            return parseResult;
        }

        // Step 2: Extract the Product Title
        LOG.info("Started parsing title for JCrew Product Item: " + itemNumber);

        try {
            // Trying to find the document's rel-tags
            Parser parser = new Parser(doc, itemNumber);
            String productTitle = parser.getProductTitle();
            String price = parser.getPrice();
            String imgURL = parser.getImageURL();
            if (productTitle != null && price != null && imgURL != null) {
                // Parse successful
                Parse parse = parseResult.get(content.getUrl());
                Metadata meta = parse.getData().getContentMeta();
                // 1. associate meta data to the index of this page
                LOG.info("Adding brand: jcrew");
                meta.add(META_BRAND, "jcrew");
                LOG.info("Adding item: " + itemNumber);
                meta.add(META_ITEMNUM, itemNumber);
                LOG.info("Adding productTitle: " + productTitle);
                meta.add(META_PRODUCT_TITLE, productTitle);
                LOG.info("Adding price: " + price);
                meta.add(META_PRICE, price);
                LOG.info("Adding imgURL: " + imgURL);
                meta.add(META_IMG_URL, imgURL);

                String htmlProductTitle = parse.getData().getTitle();
                LOG.info("Adding jcrew title: " + htmlProductTitle);
                meta.add(META_JCREW_TITLE, htmlProductTitle);

                // 2. write these information into database

                long id = idGenerator.generate("");
                LOG.info("Got ID: " + id);

            } else {
                long id = idGenerator.generate("");
                LOG.info("Got ID: " + id);
                // At least something wrong
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Partially parsed " + base + ": ");
                    if (productTitle == null) {
                        LOG.warn("productTitle is null.");
                    }
                    if (price == null) {
                        LOG.warn("price is null.");
                    }
                    if (imgURL == null) {
                        LOG.warn("imgURL is null.");
                    }
                }
            }

        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to parse: " + base + ": " + e);
                e.printStackTrace();
            }
            // e.printStackTrace();
            return parseResult;
        }

        // Finally, log the finish of parsing this product
        LOG.info("Finished parsing title for JCrew Product Item: " + itemNumber);

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

        // Test if a string matches a valid float number
        // Used to make sure the price is valid
        // Return: the string that matches float
        // null, if not matching a float number
        private String matchFloat(String input) {
            Matcher matcher = priceRegexp.matcher(input);
            if (matcher.find()) {
                // call parseFloat to make sure the
                // float is valid
                Float.parseFloat(matcher.group(1));
                return matcher.group(1);
            }
            return null;
        }

        void parse(Node node) {

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                // Look for <td> tag, searching for product title
                // In Browser I can use following javascript:
                // document.getElementsByClassName("prodtitle")[0]
                // to strip it out, but Java w3c DOM package doesn't
                // have document.getElementsByClassName method, so
                // we will just search for all <td> and do comparison
                // by ourselves.
                if ("td".equalsIgnoreCase(node.getNodeName())) {
                    NamedNodeMap attrs = node.getAttributes();
                    Node classNode = attrs.getNamedItem("class");
                    // Checks that it contains a class attribute
                    if (classNode != null) {

                        if ("prodtitle".equalsIgnoreCase(classNode.getNodeValue())
                                || "producttitle".equalsIgnoreCase(classNode.getNodeValue())) {
                            // Search for title: checks that class="prodtitle"
                            // Note: if productTitle is not null, it means it is
                            // already extracted
                            // and we won't extract it again. This is because
                            // some product pages
                            // will contain multiple products related to the
                            // main product, so the
                            // first one is the correct.
                            if (productTitle == null) {
                                productTitle = node.getFirstChild().getNodeValue();
                                if (productTitle != null) {
                                    productTitle = productTitle.trim();
                                }
                            }
                            if (productTitle != null && !StringUtil.isEmpty(productTitle)) {
                                if (LOG.isInfoEnabled()) {
                                    LOG.info("Got the productTitle for JCrew Product Item: "
                                            + itemNumber + ":" + productTitle);
                                }
                            }
                        } else if ("standard_nopad".equalsIgnoreCase(classNode.getNodeValue())
                                || "standard".equalsIgnoreCase(classNode.getNodeValue())) {
                            // Search for price: checks that
                            // class="standard_nopad" and
                            // string value ends with "item itemNumber"
                            // JCrew has the format like: $88.00 item 33774
                            String tempStr = node.getFirstChild().getNodeValue();
                            String tempStr2 = null;
                            String[] textArray = null;
                            int lastIndex = -1;
                            if (tempStr != null) {
                                tempStr = tempStr.trim();
                                textArray = tempStr.split("\\s+");
                                lastIndex = textArray.length - 1;
                            }
                            if (lastIndex > 0 && textArray[lastIndex].endsWith(itemNumber)) {
                                // Price could happen in any substring, so I
                                // will just
                                // use the original string to find "$" to make
                                // sure it is price.
                                int idxDollar = tempStr.indexOf("$");
                                if (idxDollar >= 0) {
                                    // strip out the "$"
                                    String priceStr = tempStr.substring(idxDollar + 1);
                                    price = matchFloat(priceStr);
                                    LOG.info("Got the price for JCrew Product Item: " + itemNumber
                                            + ":" + price);

                                }

                            } else if ((tempStr2 = node.getLastChild().getNodeValue()) != null
                                    && tempStr2.trim().endsWith(itemNumber)) {
                                // Sometimes, JCrew has price drop for a short
                                // time, it could
                                // have the format like:
                                // <td class="standard_nopad">
                                // was&nbsp;$59.50&nbsp;&nbsp;
                                // <font
                                // color="red">select&nbsp;colors&nbsp;$39.99&nbsp;</font>
                                // item&nbsp;29234
                                // </td>
                                int idx = tempStr.lastIndexOf("$");
                                if (idx >= 0 && idx < tempStr.length() - 1) {
                                    String oldPriceStr = tempStr.substring(idx + 1).trim();
                                    Matcher matcher = priceRegexp.matcher(oldPriceStr);
                                    if (matcher.find()) {
                                        float oldPrice = Float.parseFloat(matcher.group(1));

                                        if (LOG.isInfoEnabled()) {
                                            LOG.info("Got the old price for JCrew Product Item: "
                                                    + itemNumber + ":" + oldPrice);
                                        }
                                    }
                                    tempStr = node.getFirstChild().getNextSibling().getFirstChild()
                                            .getNodeValue().trim();
                                    idx = tempStr.lastIndexOf("$");
                                    if (idx >= 0 && idx < tempStr.length() - 1) {
                                        String priceStr = tempStr.substring(idx + 1);
                                        price = matchFloat(priceStr);
                                        LOG.info("Got the price for JCrew Product Item: "
                                                + itemNumber + ":" + price);

                                    }
                                }
                            }
                        }
                    }
                } else if ("img".equalsIgnoreCase(node.getNodeName())) {
                    // Look for the product image URL
                    // JCrew main product image has the form:
                    // <img id="mainImg" width="393" height="393" border="0"
                    // onclick="popBlowup('29234', this.src, 'mainImg','true');"
                    // alt="Cotton-cashmere crewneck sweater"
                    // name="productOnFigureImage"
                    // src="http://images.jcrew.com/erez4/erez?src=images/eiec/29/29234/29234_PR5481.tif&tmp=prdDtIm">
                    // The id="mainImg" is the search criteria
                    NamedNodeMap attrs = node.getAttributes();
                    Node idNode = attrs.getNamedItem("id");
                    String idStr = (idNode != null) ? idNode.getNodeValue() : null;
                    // Checks that it contains a class attribute
                    if (idNode != null && "mainImg".equalsIgnoreCase(idStr)) {
                        Node srcNode = attrs.getNamedItem("src");
                        imageURL = srcNode.getNodeValue();
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Got the imageURL for JCrew Product Item: " + itemNumber + ":"
                                    + imageURL);
                        }
                    } else if (idNode != null && idStr.startsWith("productOnFigureImage")
                            && idStr.endsWith(itemNumber)) {
                        // Some product pages will contain multiple products
                        // that are related together.
                        // On those pages, we don't have id="mainImg", but have
                        // id="productOnFigureImage"+itemNum
                        // For example:
                        // <img name="productOnFigureImage16564"
                        // id="productOnFigureImage16564"
                        // src="http://images.jcrew.com/erez4/erez?src=images/onFigure/16/16564/16564_GY6480_m.tif&tmp=prdDtIm"
                        // alt="Italian wool Aldridge two-button suit jacket with center vent"
                        // onClick="popBlowup('16564',this.src, this.id,true);"
                        // width="393" height="393" border="0">
                        Node srcNode = attrs.getNamedItem("src");
                        imageURL = srcNode.getNodeValue();
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Got the imageURL for JCrew Product Item: " + itemNumber + ":"
                                    + imageURL);
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

    }

    /*
     * ----------------------------- * <implementation:Configurable> *
     * -----------------------------
     */
    public void setConf(Configuration conf) {
        this.conf = conf;
        idGenerator = new MysqlIdGenerator(getConf());
    }

    public Configuration getConf() {
        return this.conf;
    }

    /*
     * ------------------------------ * </implementation:Configurable> *
     * ------------------------------
     */
}
