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
package ru.arptek.arpsite.search.lucene;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

/**
 * Extension of {@link IndexSearcher} with assumption that {@link Scorer} is
 * much faster than {@link BatchFilter}
 */
public class IndexSearcherImpl extends IndexSearcher {

    public IndexSearcherImpl(Directory path) throws CorruptIndexException,
            IOException {
        super(path);
    }

    public IndexSearcherImpl(Directory path, boolean readOnly)
            throws CorruptIndexException, IOException {
        super(path, readOnly);
    }

    public IndexSearcherImpl(IndexReader r) {
        super(r);
    }

    public IndexSearcherImpl(IndexReader reader, IndexReader[] subReaders,
            int[] docStarts) {
        super(reader, subReaders, docStarts);
    }

    @Override
    public void search(Weight weight, Filter filter, Collector collector)
            throws IOException {
        if (filter == null || !(filter instanceof BatchFilter)) {
            super.search(weight, filter, collector);
            return;
        }

        for (int i = 0; i < subReaders.length; i++) { // search each
            // subreader
            collector.setNextReader(subReaders[i], docStarts[i]);
            searchWithBatchFilter(subReaders[i], weight, (BatchFilter) filter,
                    collector);
        }
    }

    private void searchWithBatchFilter(IndexReader reader, Weight weight,
            final BatchFilter filter, final Collector collector)
            throws IOException {

        assert filter != null;

        Scorer scorer = weight.scorer(reader, true, false);
        if (scorer == null) {
            return;
        }

        assert scorer.docID() == -1
                || scorer.docID() == DocIdSetIterator.NO_MORE_DOCS;

        BitSet bitSet = new BitSet();
        int scorerDoc = DocIdSetIterator.NO_MORE_DOCS;
        while ((scorerDoc = scorer.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
            bitSet.set(scorerDoc);
        }
        bitSet = filter.filter(reader, bitSet);

        // reset scorer
        scorer = weight.scorer(reader, true, false);
        assert scorer != null;

        collector.setScorer(scorer);
        scorerDoc = -1;
        while ((scorerDoc = bitSet.nextSetBit(scorerDoc + 1)) != -1) {
            scorerDoc = scorer.advance(scorerDoc);
            if (scorerDoc == DocIdSetIterator.NO_MORE_DOCS)
                break;

            collector.collect(scorerDoc);
        }
    }
}
