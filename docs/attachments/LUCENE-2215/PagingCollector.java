package org.apache.lucene.search;

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

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * The {@link PagingCollector} allows for paging through lucene hits.
 * @author Aaron McCurry
 */
public class PagingCollector extends TopDocsCollector<ScoreDoc> {

	private ScoreDoc pqTop;
	private int docBase;
	private Scorer scorer;
	private ScoreDoc previousPassLowest;
	private int numHits;

	public PagingCollector(int numHits) {
		// creates an empty score doc so that i don't have to check for null
		// each time.
		this(numHits, new ScoreDoc(-1, Float.MAX_VALUE));
	}

	public PagingCollector(int numHits, ScoreDoc previousPassLowest) {
		super(new HitQueue(numHits, true));
		this.pqTop = pq.top();
		this.numHits = numHits;
		this.previousPassLowest = previousPassLowest;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	@Override
	public void collect(int doc) throws IOException {
		float score = scorer.score();
		totalHits++;
		doc += docBase;
		if (score > previousPassLowest.score) {
			// this hit was gathered on a previous page.
			return;
		} else if (score == previousPassLowest.score && doc <= previousPassLowest.doc) {
			// if the scores are the same and the doc is less than or equal to
			// the
			// previous pass lowest hit doc then skip because this collector
			// favors
			// lower number documents.
			return;
		} else if (score < pqTop.score || (score == pqTop.score && doc > pqTop.doc)) {
			return;
		}
		pqTop.doc = doc;
		pqTop.score = score;
		pqTop = pq.updateTop();
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.docBase = docBase;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		this.scorer = scorer;
	}

	public ScoreDoc getLastScoreDoc(TopDocs topDocs) {
		return topDocs.scoreDocs[(totalHits < numHits ? totalHits : numHits)-1];
	}

	public ScoreDoc getLastScoreDoc(ScoreDoc[] scoreDocs) {
		return scoreDocs[(totalHits < numHits ? totalHits : numHits)-1];
	}

}
