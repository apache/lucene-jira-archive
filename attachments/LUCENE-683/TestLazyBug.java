package org.apache.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;


/**
 * Test demonstrating EOF bug on the last field of the last doc 
 * if other docs have allready been accessed.
 */
public class TestLazyBug extends TestCase {

  public static int BASE_SEED = 13;

  public static int NUM_DOCS = 500;
  public static int NUM_FIELDS = 100;

  private static String[] data = new String[] {
    "asdf qqwert lkj weroia lkjadsf kljsdfowq iero ",
    " 8432 lkj nadsf w3r9 lk 3r4 l,sdf 0werlk anm adsf rewr ",
    "lkjadf ;lkj kjlsa; aoi2winm lksa;93r lka adsfwr90 ",
    ";lkj ;lak -2-fdsaj w309r5 klasdfn ,dvoawo oiewf j;las;ldf w2 ",
    " ;lkjdsaf; kwe ;ladsfn [0924r52n ldsanf jt498ut5a nlkma oi49ut ",
    "lkj asd9u0942t ;lkndv moaiewjut 09sadlkf 43wt [j'sadnm at [ualknef ;a43 "
  };
  
  private static String MAGIC_FIELD = "f"+Integer.valueOf(NUM_FIELDS / 3);
  
  private static FieldSelector SELECTOR = new FieldSelector() {
      public FieldSelectorResult accept(String f) {
        if (f.equals(MAGIC_FIELD)) {
          return FieldSelectorResult.LOAD;
        }
        return FieldSelectorResult.LAZY_LOAD;
      }
    };
  
  private static Directory makeIndex() throws RuntimeException { 
    Directory dir = new RAMDirectory();
    try {
      Random r = new Random(BASE_SEED + 42) ; 
      Analyzer analyzer = new SimpleAnalyzer();
      IndexWriter writer = new IndexWriter(dir, analyzer, true);
      
      writer.setUseCompoundFile(false);
      
      for (int d = 1; d <= NUM_DOCS; d++) {
        Document doc = new Document();
        for (int f = 1; f <= NUM_FIELDS; f++ ) {
          doc.add(new Field("f"+f, 
                            data[f % data.length] 
                            + data[r.nextInt(data.length)], 
                            Field.Store.YES, 
                            Field.Index.TOKENIZED));
        }
        writer.addDocument(doc);
      }
      writer.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return dir;
  }
  
  public static void doTest(int[] docs) throws Exception {
    Directory dir = makeIndex();
    IndexReader reader = IndexReader.open(dir);
    for (int i = 0; i < docs.length; i++) {
      Document d = reader.document(docs[i], SELECTOR);
      String trash = d.get(MAGIC_FIELD);
      
      List fields = d.getFields();
      for (Iterator fi = fields.iterator(); fi.hasNext(); ) {
        Fieldable f=null;
        try {
          f = (Fieldable) fi.next();
          assertNotNull(docs[i]+" FIELD: "+f.name(), f.stringValue());
        } catch (Exception e) {
          throw new Exception(docs[i]+" WTF: "+f.name(), e);
        }
      }
    }
    reader.close();
  }

  public void testLazyWorks() throws Exception {
    doTest(new int[] { 399 });
  }
  
  public void testLazyAlsoWorks() throws Exception {
    doTest(new int[] { 399, 150 });
  }

  public void testLazyBroken() throws Exception {
    doTest(new int[] { 150, 399 });
  }

}
