package org.apache.lucene.analysis;

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

import org.apache.lucene.util.LuceneTestCase;

/** 
 * Base class for all Lucene unit tests that use TokenStreams.  
 * <p>
 * This class runs all tests twice, one time with {@link TokenStream#setOnlyUseNewAPI} <code>false</code>
 * and after that one time with <code>true</code>.
 */
public abstract class BaseTokenStreamTestCase extends LuceneTestCase {

  public BaseTokenStreamTestCase() {
    super();
  }

  public BaseTokenStreamTestCase(String name) {
    super(name);
  }

  protected void runTest() throws Throwable {
    try {
      TokenStream.setOnlyUseNewAPI(false);
      super.runTest();
    } catch (Throwable e) {
      System.out.println("Test failure of "+getName()+" occurred with TokenStream.setOnlyUseNewAPI(false)");
      throw e;
    }

    try {
      TokenStream.setOnlyUseNewAPI(true);
      super.runTest();
    } catch (Throwable e) {
      System.out.println("Test failure of "+getName()+" occurred with TokenStream.setOnlyUseNewAPI(true)");
      throw e;
    }
  }

}
