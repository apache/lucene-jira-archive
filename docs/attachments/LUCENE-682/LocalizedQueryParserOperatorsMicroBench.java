package org.apache.lucene.queryParser;

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

import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.Query;

public class LocalizedQueryParserOperatorsMicroBench extends TestCase {

  public static Random R = new Random(13);
  public static String[] rawinputs = new String[] {
    "uno ",
    "uno dos ",
    "uno +dos ",
    "+uno -dos ",
    "uno \"dos tres\" ",
    "uno +(dos tres) ",
    "uno AND dos OR tres ",
    "NOT uno AND NOT dos AND tres ",
    "how now brown cow the dish ran awaay with the spoon ",
    "uno* dos~ count:[tres TO cinco] ",
    "(uno AND dos)^12 OR (tres AND quatro)^34 OR \"some other language entirely\"~2 ",
  };

  public static String[] fullinputs = new String[rawinputs.length^2];
  static {
    for (int i = 0; i < fullinputs.length; i++) {
      StringBuffer buf = new StringBuffer();
      buf.append(rawinputs[R.nextInt(rawinputs.length)]);
      for (int j = 0; j <= i; j++) {
        boolean how = R.nextBoolean();
        if (how) buf.append(" AND (");
        buf.append(rawinputs[R.nextInt(rawinputs.length)]);
        if (how) buf.append(") ");
      }
      fullinputs[i] = buf.toString();
    }
  }

  public void testLegacyOperatorSingleUse() throws Exception {
    doTest(10000,1);
  }
  public void testLegacyOperatorFiveUses() throws Exception {
    doTest(10000,5);
  }

  public void doTest(int iters, int uses) throws Exception {

    for (int i = 0; i < iters; i++) {
      QueryParser p = getParser(null);
      for (int j = 0; j < uses; j++) {
        Query q = p.parse(fullinputs[R.nextInt(fullinputs.length)]);
      }
    }
  }


  public QueryParser getParser(Analyzer a) throws Exception {
    if (a == null)
      a = new SimpleAnalyzer();
    QueryParser qp = new QueryParser("field", a);
    qp.setDefaultOperator(QueryParser.OR_OPERATOR);
    return qp;
  }

}
