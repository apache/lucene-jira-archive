package org.apache.lucene.codecs.perfield;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerFieldStoredFieldsWriter extends StoredFieldsWriter {

  /**
   * Buffer of stored fields for the current document
   */
  private FieldRefs fieldsBuffer;

  /**
   * References to the instantiated {@link StoredFieldsWriter}s per
   * {@link StoredFieldsFormat}.
   */
  private final Map<StoredFieldsFormat, StoredFieldsWriter> writers;

  /**
   * Reference to the parent {@link PerFieldStoredFieldsFormat}.
   */
  private final PerFieldStoredFieldsFormat perFieldFormat;

  private final Directory directory;
  private final SegmentInfo si;
  private final IOContext context;

  protected static final
  Logger logger = LoggerFactory.getLogger(PerFieldStoredFieldsWriter.class);

  public PerFieldStoredFieldsWriter(final Directory directory,
                                    final SegmentInfo si,
                                    final IOContext context,
                                    final PerFieldStoredFieldsFormat perFieldFormat) {
    this.perFieldFormat = perFieldFormat;
    this.directory = directory;
    this.si = si;
    this.context = context;
    writers = new HashMap<StoredFieldsFormat, StoredFieldsWriter>();
  }

  @Override
  public void startDocument(final int numStoredFields) throws IOException {
    fieldsBuffer = new FieldRefs(numStoredFields);
  }

  @Override
  public void writeField(final FieldInfo info, final IndexableField field)
  throws IOException {
    final StoredFieldsFormat format = perFieldFormat.getStoredFieldsFormatForField(info.name);
    if (format == null) {
      throw new IllegalStateException("invalid null StoredFieldsFormat for field=\"" + info.name + "\"");
    }

    // First time we are seeing this format; create a new instance
    if (!writers.containsKey(format)) {
      writers.put(format, format.fieldsWriter(directory, si, context));
    }

    // Buffer the field
    fieldsBuffer.add(format, info, field);

    // If we received all the stored fields for this document, we can write
    // them
    if (fieldsBuffer.isFull()) {
      this.callStartDocument();
      this.callWriteField();
    }
  }

  /**
   * Call {@link StoredFieldsWriter#startDocument(int)} for each of the
   * {@link StoredFieldsFormat} used in the current document. The
   * <code>numStoredFields</code> parameter is derived from the number of stored
   * fields that is buffered for a given {@link StoredFieldsFormat}.
   */
  private void callStartDocument() throws IOException {
    for (final StoredFieldsFormat format : fieldsBuffer.getStoredFieldsFormat()) {
      writers.get(format).startDocument(fieldsBuffer.getNumStoredFields(format));
    }
  }

  /**
   * Call {@link StoredFieldsWriter#writeField(FieldInfo, IndexableField)} for
   * each of the {@link StoredFieldsFormat} used in the current document.
   */
  private void callWriteField() throws IOException {
    for (final StoredFieldsFormat format : fieldsBuffer.getStoredFieldsFormat()) {
      for (final FieldRef ref : fieldsBuffer.getFieldRefs(format)) {
        writers.get(format).writeField(ref.fieldInfo, ref.indexableField);
      }
    }
  }

  @Override
  public void abort() {
    for (final StoredFieldsWriter writer : writers.values()) {
      writer.abort();
    }
  }

  @Override
  public void finish(final FieldInfos fis, final int numDocs)
  throws IOException {
    for (final StoredFieldsWriter writer : writers.values()) {
      writer.finish(fis, numDocs);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      IOUtils.close(writers.values());
    }
    finally {
      writers.clear();
    }
  }

  /**
   * A finite set of {@link FieldRef} ordered by {@link StoredFieldsFormat}.
   */
  private static class FieldRefs {

    /** The expected number of stored field references */
    public int numStoredFields;
    /** The {@link FieldRef}s per {@link StoredFieldsFormat} */
    private final Map<StoredFieldsFormat, List<FieldRef>> fieldRefs;

    public FieldRefs(final int numStoredFields) {
      this.numStoredFields = numStoredFields;
      fieldRefs = new HashMap<StoredFieldsFormat, List<FieldRef>>();
    }

    public void add(final StoredFieldsFormat format, final FieldInfo info,
                    final IndexableField field) {
      if (!fieldRefs.containsKey(format)) {
        fieldRefs.put(format, new ArrayList<FieldRef>());
      }
      fieldRefs.get(format).add(new FieldRef(info, field));
      this.numStoredFields--;
    }

    public boolean isFull() {
      return numStoredFields == 0;
    }

    public Set<StoredFieldsFormat> getStoredFieldsFormat() {
      return fieldRefs.keySet();
    }

    public int getNumStoredFields(final StoredFieldsFormat format) {
      return fieldRefs.get(format).size();
    }

    public List<FieldRef> getFieldRefs(final StoredFieldsFormat format) {
      return fieldRefs.get(format);
    }

  }

  /**
   * A reference to a stored field composed of a {@link FieldInfo} and a
   * {@link IndexableField}.
   */
  private static class FieldRef {

    /** The {@link FieldInfo} of the FieldRef. Should never be {@code null}. */
    public FieldInfo fieldInfo;
    /** The {@link IndexableField} of the FieldRef. Should never be {@code null}. */
    public IndexableField indexableField;

    public FieldRef(final FieldInfo fieldInfo, final IndexableField indexableField) {
      this.fieldInfo = fieldInfo;
      this.indexableField = indexableField;
    }

  }

}
