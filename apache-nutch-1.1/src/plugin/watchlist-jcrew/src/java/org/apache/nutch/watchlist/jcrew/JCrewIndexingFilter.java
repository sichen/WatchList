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

// Nutch imports
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.lucene.LuceneWriter;
import org.apache.hadoop.io.Text;
import org.apache.nutch.parse.Parse;

// Hadoop imports
import org.apache.hadoop.conf.Configuration;

/**
 * add Lucene index field(s) to the document.
 * 
 * @author Si Chen
 */
public class JCrewIndexingFilter implements IndexingFilter {

    private Configuration conf;

    public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum,
            Inlinks inlinks) throws IndexingException {

        // Fill brand into document, for Lucene to index
        String[] tags = parse.getData().getParseMeta().getValues(JCrewParser.META_BRAND);
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                doc.add(JCrewParser.META_BRAND, tags[i]);
            }
        }
        // Fill item number into document
        tags = parse.getData().getParseMeta().getValues(JCrewParser.META_ITEMNUM);
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                doc.add(JCrewParser.META_ITEMNUM, tags[i]);
            }
        }
        // Fill product title into document
        tags = parse.getData().getParseMeta().getValues(JCrewParser.META_PRODUCT_TITLE);
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                doc.add(JCrewParser.META_PRODUCT_TITLE, tags[i]);
            }
        }
        // Fill price into document
        tags = parse.getData().getParseMeta().getValues(JCrewParser.META_PRICE);
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                doc.add(JCrewParser.META_PRICE, tags[i]);
            }
        }
        // Fill html title into document, may be it is duplicated work because html parser
        // should also do the same thing, but we still do it to boost search score
        tags = parse.getData().getParseMeta().getValues(JCrewParser.META_JCREW_TITLE);
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                doc.add(JCrewParser.META_JCREW_TITLE, tags[i]);
            }
        }

        return doc;
    }

    public void addIndexBackendOptions(Configuration conf) {
        LuceneWriter.addFieldOptions(JCrewParser.META_BRAND, LuceneWriter.STORE.YES,
                LuceneWriter.INDEX.UNTOKENIZED, conf);
        LuceneWriter.addFieldOptions(JCrewParser.META_ITEMNUM, LuceneWriter.STORE.YES,
                LuceneWriter.INDEX.UNTOKENIZED, conf);
        LuceneWriter.addFieldOptions(JCrewParser.META_PRODUCT_TITLE, LuceneWriter.STORE.YES,
                LuceneWriter.INDEX.TOKENIZED, conf);
        LuceneWriter.addFieldOptions(JCrewParser.META_PRICE, LuceneWriter.STORE.YES,
                LuceneWriter.INDEX.UNTOKENIZED, conf);
        LuceneWriter.addFieldOptions(JCrewParser.META_JCREW_TITLE, LuceneWriter.STORE.YES,
                LuceneWriter.INDEX.TOKENIZED, conf);
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
