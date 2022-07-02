package org.apache.lucene.facet.index;

/*
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValues.Source;
import org.apache.lucene.index.DocValues.Type;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;

/**
 * A {@link FilterAtomicReader} for migrating a facets index which encodes
 * category ordinals in a payload to {@link DocValues}. To migrate the index,
 * you should build a mapping from a field (String) to term ({@link Term}).
 * Every requested {@link #docValues(String) field} will be read from the mapped
 * {@link Term term's} payload. You can follow the code example below to migrate
 * an existing index:
 * 
 * <pre class="prettyprint">
 * // Add the index and migrate payload to DocValues on the go
 * DirectoryReader reader = DirectoryReader.open(oldDir);
 * IndexWriterConfig conf = new IndexWriterConfig(VER, ANALYZER);
 * IndexWriter writer = new IndexWriter(newDir, conf);
 * List&lt;AtomicReaderContext&gt; leaves = reader.leaves();
 * AtomicReader wrappedLeaves[] = new AtomicReader[leaves.size()];
 * for (int i = 0; i &lt; leaves.size(); i++) {
 *   wrappedLeaves[i] = new FacetPayloadMigrationReader(leaves.get(i).reader(), fieldTerms);
 * }
 * writer.addIndexes(new MultiReader(wrappedLeaves));
 * writer.commit();
 * </pre>
 * 
 * @lucene.experimental
 */
public class FacetsPayloadMigrationReader extends FilterAtomicReader {  

  private class PayloadMigratingDocValues extends DocValues {

    private final DocsAndPositionsEnum dpe;
    
    public PayloadMigratingDocValues(DocsAndPositionsEnum dpe) {
      this.dpe = dpe;
    }

    @Override
    protected Source loadDirectSource() throws IOException {
      return new PayloadMigratingSource(getType(), dpe);
    }

    @Override
    protected Source loadSource() throws IOException {
      throw new UnsupportedOperationException("in-memory Source is not supported by this reader");
    }

    @Override
    public Type getType() {
      return Type.BYTES_VAR_STRAIGHT;
    }
    
  }
  
  private class PayloadMigratingSource extends Source {

    private final DocsAndPositionsEnum dpe;
    private int curDocID;
    
    protected PayloadMigratingSource(Type type, DocsAndPositionsEnum dpe) {
      super(type);
      this.dpe = dpe;
      if (dpe == null) {
        curDocID = DocIdSetIterator.NO_MORE_DOCS;
      } else {
        try {
          curDocID = dpe.nextDoc();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
    
    @Override
    public BytesRef getBytes(int docID, BytesRef ref) {
      if (curDocID > docID) {
        // document does not exist
        ref.length = 0;
        return ref;
      }
      
      try {
        if (curDocID < docID) {
          curDocID = dpe.advance(docID);
          if (curDocID != docID) { // requested document does not have a payload
            ref.length = 0;
            return ref;
          }
        }
        
        // we're on the document
        dpe.nextPosition();
        ref.copyBytes(dpe.getPayload());
        return ref;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
  }
  
  private final Map<String,Term> fieldTerms;
  
  /**
   * Wraps an {@link AtomicReader} and migrates the payload to {@link DocValues}
   * fields by using the given mapping.
   */
  public FacetsPayloadMigrationReader(AtomicReader in, Map<String,Term> fieldTerms) {
    super(in);
    this.fieldTerms = fieldTerms;
  }
  
  @Override
  public DocValues docValues(String field) throws IOException {
    Term term = fieldTerms.get(field);
    if (term == null) {
      return super.docValues(field);
    } else {
      DocsAndPositionsEnum dpe = null;
      Fields fields = fields();
      if (fields != null) {
        Terms terms = fields.terms(term.field());
        if (terms != null) {
          TermsEnum te = terms.iterator(null); // no use for reusing
          if (te.seekExact(term.bytes(), true)) {
            // we're not expected to be called for deleted documents
            dpe = te.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_PAYLOADS);
          }
        }
      }
      // we shouldn't return null, even if the term does not exist or has no
      // payloads, since we already marked the field as having DocValues.
      return new PayloadMigratingDocValues(dpe);
    }
  }

  @Override
  public FieldInfos getFieldInfos() {
    FieldInfos innerInfos = super.getFieldInfos();
    ArrayList<FieldInfo> infos = new ArrayList<FieldInfo>(innerInfos.size());
    // if there are partitions, then the source index contains one field for all their terms
    // while with DocValues, we simulate that by multiple fields.
    HashSet<String> leftoverFields = new HashSet<String>(fieldTerms.keySet());
    int number = -1;
    for (FieldInfo info : innerInfos) {
      if (fieldTerms.containsKey(info.name)) {
        // mark this field as having a DocValues
        infos.add(new FieldInfo(info.name, true, info.number,
            info.hasVectors(), info.omitsNorms(), info.hasPayloads(),
            info.getIndexOptions(), Type.BYTES_VAR_STRAIGHT,
            info.getNormType(), info.attributes()));
        leftoverFields.remove(info.name);
      } else {
        infos.add(info);
      }
      number = Math.max(number, info.number);
    }
    for (String field : leftoverFields) {
      infos.add(new FieldInfo(field, false, ++number, false, false, false,
          null, Type.BYTES_VAR_STRAIGHT, null, null));
    }
    return new FieldInfos(infos.toArray(new FieldInfo[infos.size()]));
  }
  
}
