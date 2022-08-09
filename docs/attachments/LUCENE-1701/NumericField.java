package org.apache.lucene.document;

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

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.NumericTokenStream;

/**
 * TODO
 * @since 2.9
 */
public final class NumericField extends AbstractField {

  private final NumericTokenStream tokenStream;

  /** TODO */
  public NumericField(String name, int precisionStep, Field.Store store, boolean index) {
    super(name, store, index ? Field.Index.ANALYZED_NO_NORMS : Field.Index.NO, Field.TermVector.NO);
    setOmitTermFreqAndPositions(true);
    this.tokenStream = new NumericTokenStream(precisionStep);
  }

  /** Returns a {@link NumericTokenStream}. */
  public TokenStream tokenStreamValue()   {
    return tokenStream;
  }
  
  /** this returns always <code>null</code> for numeric fields */
  public byte[] binaryValue() {
    return null;
  }
  
  /** this returns always <code>null</code> for numeric fields */
  public Reader readerValue() {
    return null;
  }
    
  /** This returns the numeric value as a string (how it is stored, when {@link Field.Store#YES} is choosen). */
  public String stringValue()   {
    if (isStored && fieldsData == null)
      throw new IllegalStateException("The field was not initialized with a numeric value.");
    return (fieldsData == null) ? null : fieldsData.toString();
  }
  
  public Number getNumericValue() {
    return (Number) fieldsData;
  }
  
  /**
   * Initializes the field with the supplied <code>long</code> value.
   * @param value the numeric value
   * @return this instance, because of this you can use it the following way:
   * <code>document.add(new NumericField(name, precisionStep).setLongValue(value))</code>
   */
  public NumericField setLongValue(final long value) {
    tokenStream.setLongValue(value);
    fieldsData = new Long(value);
    return this;
  }
  
  /**
   * Initializes the field with the supplied <code>int</code> value.
   * @param value the numeric value
   * @return this instance, because of this you can use it the following way:
   * <code>document.add(new NumericField(name, precisionStep).setIntValue(value))</code>
   */
  public NumericField setIntValue(final int value) {
    tokenStream.setIntValue(value);
    fieldsData = new Integer(value);
    return this;
  }
  
  /**
   * Initializes the field with the supplied <code>double</code> value.
   * @param value the numeric value
   * @return this instance, because of this you can use it the following way:
   * <code>document.add(new NumericField(name, precisionStep).setDoubleValue(value))</code>
   */
  public NumericField setDoubleValue(final double value) {
    tokenStream.setDoubleValue(value);
    fieldsData = new Double(value);
    return this;
  }
  
  /**
   * Initializes the field with the supplied <code>float</code> value.
   * @param value the numeric value
   * @return this instance, because of this you can use it the following way:
   * <code>document.add(new NumericField(name, precisionStep).setFloatValue(value))</code>
   */
  public NumericField setFloatValue(final float value) {
    tokenStream.setFloatValue(value);
    fieldsData = new Float(value);
    return this;
  }

}
