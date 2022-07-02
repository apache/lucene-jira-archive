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

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

import com.ironmountain.bedrock.search.v2.lucene.FieldCacheRangeFilter;

/**
 * Documents are the unit of indexing and search. Values of various types can be
 * added to the document, though text is the most typical type. These values may
 * be optionally stored and indexed.
 * <p>
 * <b>Note:</b> No implicit type conversion occurs. If you add a value as an
 * integer, then it will only match queries for integers. It is possible to use
 * the same name for different types but the two values remain distinct.
 * <p>
 * This class also contains static helper methods to produce appropriate
 * {@link Term}, {@link Query} and {@link SortField} objects for the typed
 * fields.
 * 
 * @author rnewson
 * 
 */
public class ExtendedDocument implements Serializable {

	public static final int MAX_BINARY_LENGTH = 16 * 1024;

	private static final String BINARY = "<binary>";

	private static final String BOOL = "<bool>";

	private static final String DATE = "<date>";

	private static final String DOUBLE = "<double>";

	private static final String FLOAT = "<float>";

	private static final String INT = "<int>";

	private static final String LONG = "<long>";

	private static final long serialVersionUID = 1L;

	private static final String SHORT = "<short>";

	private static final String STRING = "<string>";

	/**
	 * The binary form of the supplied field name.
	 */
	public static String binaryName(final String name) {
		return name + BINARY;
	}

	/**
	 * The boolean form of the supplied field name.
	 */
	public static String boolName(final String name) {
		return name + BOOL;
	}

	/**
	 * The {@link SortField} for the boolean form of the supplied field name.
	 */
	public static SortField boolSort(final String name) {
		return new SortField(boolName(name), SortField.STRING);
	}

	/**
	 * The {@link Date} form of the supplied field name.
	 */
	public static String dateName(final String name) {
		return name + DATE;
	}

	/**
	 * The {@link SortField} for the {@link Date} form of the supplied field
	 * name.
	 */
	public static SortField dateSort(final String name) {
		return new SortField(dateName(name), SortField.LONG);
	}

	/**
	 * The double form of the supplied field name.
	 */
	public static String doubleName(final String name) {
		return name + DOUBLE;
	}

	/**
	 * The {@link SortField} for the double form of the supplied field name.
	 */
	public static SortField doubleSort(final String name) {
		return new SortField(doubleName(name), SortField.DOUBLE);
	}

	/**
	 * The float form of the supplied field name.
	 */
	public static String floatName(final String name) {
		return name + FLOAT;
	}

	/**
	 * The {@link SortField} for the float form of the supplied field name.
	 */
	public static SortField floatSort(final String name) {
		return new SortField(floatName(name), SortField.FLOAT);
	}

	/**
	 * The int form of the supplied field name.
	 */
	public static String intName(final String name) {
		return name + INT;
	}

	/**
	 * The {@link SortField} for the int form of the supplied field name.
	 */
	public static SortField intSort(final String name) {
		return new SortField(intName(name), SortField.INT);
	}

	/**
	 * The long form of the supplied field name.
	 */
	public static String longName(final String name) {
		return name + LONG;
	}

	/**
	 * The {@link SortField} for the long form of the supplied field name.
	 */
	public static SortField longSort(final String name) {
		return new SortField(longName(name), SortField.LONG);
	}

	/**
	 * The short form of the supplied field name.
	 */
	public static String shortName(final String name) {
		return name + SHORT;
	}

	/**
	 * The {@link SortField} for the short form of the supplied field name.
	 */
	public static SortField shortSort(final String name) {
		return new SortField(boolName(name), SortField.SHORT);
	}

	/**
	 * The String form of the supplied field name.
	 */
	public static String stringName(final String name) {
		return name + STRING;
	}

	/**
	 * A {@link Query} object that matches the boolean form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final boolean value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the {@link Date} form of the supplied
	 * name and value.
	 */
	public static Query toQuery(final String name, final Date value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the double form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final double value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the float form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final float value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the int form of the supplied name and
	 * value.
	 */
	public static Query toQuery(final String name, final int value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the long form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final long value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the short form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final short value) {
		return new TermQuery(toTerm(name, value));
	}

	/**
	 * A {@link Query} object that matches the String form of the supplied name
	 * and value.
	 */
	public static Query toQuery(final String name, final String value) {
		return new TermQuery(toTerm(name, value));
	}

	public static Filter toRangeFilter(final String name, final Date from,
			final Date to) {
		return new FieldCacheRangeFilter.LongFilter(dateName(name),
				from == null ? 0 : from.getTime(), to == null ? Long.MAX_VALUE
						: to.getTime());
	}

	public static Filter toRangeFilter(final String name, final double from,
			final double to) {
		return new FieldCacheRangeFilter.DoubleFilter(doubleName(name), from,
				to);
	}

	public static Filter toRangeFilter(final String name, final float from,
			final float to) {
		return new FieldCacheRangeFilter.FloatFilter(floatName(name), from, to);
	}

	public static Filter toRangeFilter(final String name, final int from,
			final int to) {
		return new FieldCacheRangeFilter.IntegerFilter(intName(name), from, to);
	}

	public static Filter toRangeFilter(final String name, final long from,
			final long to) {
		return new FieldCacheRangeFilter.LongFilter(longName(name), from, to);
	}

	public static Filter toRangeFilter(final String name, final short from,
			final short to) {
		return new FieldCacheRangeFilter.ShortFilter(shortName(name), from, to);
	}

	/**
	 * A {@link Term} object that matches the boolean form of the supplied name
	 * and value.
	 */
	public static Term toTerm(final String name, final boolean value) {
		return new Term(boolName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the {@link Date} form of the supplied
	 * name and value.
	 */
	public static Term toTerm(final String name, final Date value) {
		return new Term(dateName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the double form of the supplied name
	 * and value.
	 */
	public static Term toTerm(final String name, final double value) {
		return new Term(doubleName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the float form of the supplied name
	 * and value.
	 */
	public static Term toTerm(final String name, final float value) {
		return new Term(floatName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the int form of the supplied name and
	 * value.
	 */
	public static Term toTerm(final String name, final int value) {
		return new Term(intName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the long form of the supplied name and
	 * value.
	 */
	public static Term toTerm(final String name, final long value) {
		return new Term(longName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the short form of the supplied name
	 * and value.
	 */
	public static Term toTerm(final String name, final short value) {
		return new Term(boolName(name), toString(value));
	}

	/**
	 * A {@link Term} object that matches the {@link String} form of the
	 * supplied name and value.
	 */
	public static Term toTerm(final String name, final String value) {
		return new Term(stringName(name), value);
	}

	/**
	 * Converts a boolean value to the indexed String form.
	 */
	private static String toString(final boolean value) {
		return Boolean.toString(value);
	}

	/**
	 * Converts a Date value to the indexed String form.
	 */
	private static String toString(final Date value) {
		return toString(value.getTime());
	}

	/**
	 * Converts a double value to the indexed String form.
	 */
	private static String toString(final double value) {
		return Double.toString(value);
	}

	/**
	 * Converts a float value to the indexed String form.
	 */
	private static String toString(final float value) {
		return Float.toString(value);
	}

	/**
	 * Converts a int value to the indexed String form.
	 */
	private static String toString(final int value) {
		return Integer.toString(value);
	}

	/**
	 * Converts a long value to the indexed String form.
	 */
	private static String toString(final long value) {
		return Long.toString(value);
	}

	/**
	 * Converts a boolean value to the indexed String form.
	 */
	private static String toString(final short value) {
		return Short.toString(value);
	}

	private final org.apache.lucene.document.Document delegate;

	/**
	 * Create a new, empty document.
	 */
	public ExtendedDocument() {
		delegate = new org.apache.lucene.document.Document();
	}

	/**
	 * Provide a type-safe wrapper around a Lucene document.
	 */
	public ExtendedDocument(final org.apache.lucene.document.Document doc) {
		this.delegate = doc;
	}

	/**
	 * Add a binary value to the document. Values over
	 * {@link #MAX_BINARY_LENGTH} bytes are not permitted.
	 * 
	 * @throws IllegalArgumentException
	 *             if byte array exceeds the limit.
	 */
	public final void addBinary(final String name, final byte[] value) {
		if (value.length > MAX_BINARY_LENGTH) {
			throw new IllegalArgumentException("byte array exceeds maximum.");
		}
		addField(new Field(binaryName(name), value, Store.YES));
	}

	/**
	 * Add a boolean value to the document.
	 */
	public final void addBoolean(final String name, final boolean value,
			final boolean store, final boolean index) {
		addField(new Field(boolName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add a date value to the document. Dates are recorded to second
	 * resolution.
	 */
	public final void addDate(final String name, final Date value,
			final boolean store, final boolean index) {
		addField(new Field(dateName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add a double value to the document.
	 */
	public final void addDouble(final String name, final double value,
			final boolean store, final boolean index) {
		addField(new Field(doubleName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add a float value to the document.
	 */
	public final void addFloat(final String name, final float value,
			final boolean store, final boolean index) {
		addField(new Field(floatName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add an int value to the document.
	 */
	public final void addInt(final String name, final int value,
			final boolean store, final boolean index) {
		addField(new Field(intName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add a long value to the document.
	 */
	public final void addLong(final String name, final long value,
			final boolean store, final boolean index) {
		addField(new Field(longName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add a short value to the document.
	 */
	public final void addShort(final String name, final short value,
			final boolean store, final boolean index) {
		addField(new Field(shortName(name), toString(value), store(store),
				index(index)));
	}

	/**
	 * Add text to the document.
	 */
	public final void addString(final String name, final String value,
			final Store store, final Index index) {
		addField(new Field(stringName(name), value, store, index));
	}

	/**
	 * @return the binary value of the field.
	 */
	public final byte[] getBinary(final String name) {
		return delegate.getBinaryValue(binaryName(name));
	}

	/**
	 * @return the boolean value of the field.
	 */
	public final boolean getBoolean(final String name) {
		return Boolean.parseBoolean(get(boolName(name)));
	}

	/**
	 * 
	 * @return the date value of the field.
	 */
	public final Date getDate(final String name) {
		return new Date(Long.parseLong(get(dateName(name))));
	}

	/**
	 * @return the double value of the field.
	 */
	public final double getDouble(final String name) {
		return Double.parseDouble(get(doubleName(name)));
	}

	/**
	 * @return the float value of the field.
	 */
	public final float getFloat(final String name) {
		return Float.parseFloat(get(floatName(name)));
	}

	/**
	 * @return the int value of the field.
	 */
	public final int getInt(final String name) {
		return Integer.parseInt(get(intName(name)));
	}

	/**
	 * @return the long value of the field.
	 */
	public final long getLong(final String name) {
		return Long.parseLong(get(longName(name)));
	}

	/**
	 * @return the short value of the field.
	 */
	public final short getShort(final String name) {
		return Short.parseShort(get(shortName(name)));
	}

	/**
	 * @return the String value of the field.
	 */
	public final String getString(final String name) {
		return delegate.get(stringName(name));
	}

	/**
	 * @return A Lucene-compatible object with the same data.
	 */
	public final org.apache.lucene.document.Document toLuceneDocument() {
		return delegate;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
				.append("doc", this.delegate).toString();
	}

	private void addField(final Field field) {
		delegate.add(field);
	}

	private final String get(final String name) {
		return delegate.get(name);
	}

	private Index index(final boolean index) {
		return index ? Index.NOT_ANALYZED_NO_NORMS : Index.NO;
	}

	private Store store(final boolean store) {
		return store ? Store.YES : Store.NO;
	}

}
