package org.apache.lucene.codecs.perfield;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.StoredFieldsReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.index.StoredFieldVisitor.Status;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.util.IOUtils;

public class PerFieldStoredFieldsReader extends StoredFieldsReader {

  private final Directory directory;
  private final SegmentInfo si;
  private final FieldInfos fn;
  private final IOContext context;

  /**
   * References to the instantiated {@link StoredFieldsReader}s per
   * {@link StoredFieldsFormat}.
   */
  private final Map<StoredFieldsFormat, StoredFieldsReader> readers;

  /**
   * Reference to the parent {@link PerFieldStoredFieldsFormat}.
   */
  private final PerFieldStoredFieldsFormat perFieldFormat;

  public PerFieldStoredFieldsReader(final Directory directory,
                                    final SegmentInfo si,
                                    final FieldInfos fn,
                                    final IOContext context,
                                    final PerFieldStoredFieldsFormat perFieldFormat) {
    this.directory = directory;
    this.si = si;
    this.fn = fn;
    this.context = context;
    this.perFieldFormat = perFieldFormat;
    readers = new HashMap<StoredFieldsFormat, StoredFieldsReader>();
  }

  // used by clone
  private PerFieldStoredFieldsReader(final PerFieldStoredFieldsReader reader) {
    this.directory = reader.directory;
    this.si = reader.si;
    this.fn = reader.fn;
    this.context = reader.context;
    this.perFieldFormat = reader.perFieldFormat;
    readers = new HashMap<StoredFieldsFormat, StoredFieldsReader>();
    for (final Entry<StoredFieldsFormat, StoredFieldsReader> entry : reader.readers.entrySet()) {
      readers.put(entry.getKey(), entry.getValue().clone());
    }
  }

  @Override
  public void visitDocument(final int n, final StoredFieldVisitor visitor)
  throws IOException {
    final Iterator<FieldInfo> it = fn.iterator();
    while (it.hasNext()) {
      final FieldInfo field = it.next();
      if (visitor.needsField(field) == Status.YES) {
        final StoredFieldsFormat format = perFieldFormat.getStoredFieldsFormatForField(field.name);
        // First time we are seeing this format; create a new instance
        if (!readers.containsKey(format)) {
          readers.put(format, format.fieldsReader(directory, si, fn, context));
        }
        readers.get(format).visitDocument(n, visitor);
      }
    }
  }

  @Override
  public StoredFieldsReader clone() {
    return new PerFieldStoredFieldsReader(this);
  }

  @Override
  public void close() throws IOException {
    try {
      IOUtils.close(readers.values());
    }
    finally {
      readers.clear();
    }
  }

}
