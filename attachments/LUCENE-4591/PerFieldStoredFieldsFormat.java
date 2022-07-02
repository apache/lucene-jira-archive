package org.apache.lucene.codecs.perfield;

import java.io.IOException;

import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.codecs.StoredFieldsWriter;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

public abstract class PerFieldStoredFieldsFormat extends StoredFieldsFormat {

  /** {@link FieldInfo} attribute name used to store the
   *  format name for each field. */
  public static final String PER_FIELD_FORMAT_KEY = PerFieldStoredFieldsFormat.class.getSimpleName() + ".format";

  /** {@link FieldInfo} attribute name used to store the
   *  segment suffix name for each field. */
  public static final String PER_FIELD_SUFFIX_KEY = PerFieldStoredFieldsFormat.class.getSimpleName() + ".suffix";

  @Override
  public StoredFieldsReader fieldsReader(final Directory directory, final SegmentInfo si,
                                         final FieldInfos fn, final IOContext context)
  throws IOException {
    return new PerFieldStoredFieldsReader(directory, si, fn, context, this);
  }

  @Override
  public StoredFieldsWriter fieldsWriter(final Directory directory, final SegmentInfo si,
                                         final IOContext context)
  throws IOException {
    return new PerFieldStoredFieldsWriter(directory, si, context, this);
  }

  /**
   * Returns the {@link StoredFieldsFormat} that should be used for writing
   * new segments of <code>field</code>.
   *
   * <p>
   *
   * The field to format mapping is written to the index, so
   * this method is only invoked when writing, not when reading.
   */
  public abstract StoredFieldsFormat getStoredFieldsFormatForField(String field);

  static String getSuffix(final String formatName, final String suffix) {
    return formatName + "_" + suffix;
  }

}
