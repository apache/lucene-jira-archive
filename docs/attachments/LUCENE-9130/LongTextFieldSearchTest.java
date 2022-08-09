package test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LongTextFieldSearchTest {
	IndexWriter indexWriter;
	IndexReader indexReader;
	IndexSearcher indexSearcher;

	String fieldName = "address";
	String addressValue = "申长路988弄虹桥万科中心地下停车场LG2层2179-2184车位(锡虹路入,LG1层开到底下LG2)";	

	Analyzer analyzer = new SmartChineseAnalyzer();
	int doc;

	@Before
	public void setUp() throws Exception {
		Directory indexDataDir = FSDirectory.open(Paths.get("d:/tmp"));
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		indexWriter = new IndexWriter(indexDataDir, iwc);
		org.apache.lucene.document.Document document = new org.apache.lucene.document.Document();
		document.add(new TextField(fieldName, addressValue, Store.NO)); 
		indexWriter.addDocument(document);
		indexWriter.commit();
		indexReader = DirectoryReader.open(indexWriter);
		indexSearcher = new IndexSearcher(this.indexReader);

		Set<String> terms = loadTerms(indexReader, fieldName);
		log.info("indexed terms: "+ StringUtils.join((Collection<String>)terms, ", "));
		
		Query q = new MatchAllDocsQuery();
		TopDocs results = indexSearcher.search(q, 1);
		assert results.totalHits.value==1;
		this.doc = results.scoreDocs[0].doc;
	}

	@After
	public void tearDown() throws Exception {
		indexSearcher = null;
		indexReader.close();
		indexWriter.close();
	}

	@Test
	public void testPhraseQuery() throws IOException {
		String queryText = addressValue;
		List<String> L = analysis(analyzer, fieldName, queryText);
		//
		StringTokenizer st = new StringTokenizer(queryText, " ");
		BooleanQuery.Builder builderBooleanMUST = new BooleanQuery.Builder();
		while(st.hasMoreTokens()){
			String subQueryText = st.nextToken();
			{
				Query q = buildPhraseQuery(indexReader, analyzer, fieldName, subQueryText, 2);
				builderBooleanMUST.add(q, BooleanClause.Occur.MUST);
			}
		}
		Query q = builderBooleanMUST.build();
		log.info("query: "+q);
		//
		TopDocs results = this.indexSearcher.search(q, 1);
		//assert results.totalHits.value==1;
		log.info("results.totalHits.value="+results.totalHits.value);
		if(results.totalHits.value==0) {
			Explanation explanation = this.indexSearcher.explain(q, this.doc);
			logExplanation(explanation);
		}
	}
	
	@Test
	public void testBooleanAndQuery() throws IOException {
		String queryText = addressValue;
		List<String> L = analysis(analyzer, fieldName, queryText);
		//
		StringTokenizer st = new StringTokenizer(queryText, " ");
		BooleanQuery.Builder builderBooleanMUST = new BooleanQuery.Builder();
		while(st.hasMoreTokens()){
			String subQueryText = st.nextToken();
			{
				Query q = buildBooleanANDQuery(analyzer, fieldName, subQueryText);
				builderBooleanMUST.add(q, BooleanClause.Occur.MUST);
			}
		}
		Query q = builderBooleanMUST.build();
		log.info("query: "+q);
		//
		TopDocs results = this.indexSearcher.search(q, 1);
		log.info("results.totalHits.value="+results.totalHits.value);
		if(results.totalHits.value==0) {
			Explanation explanation = this.indexSearcher.explain(q, this.doc);
			logExplanation(explanation);
			fail("!");
		}
	}

	private void logExplanation(Explanation explanation) {
		logExplanation(explanation, "");
	}

	private void logExplanation(Explanation explanation, String indent) {
		log.info(indent + explanation.getDescription());
		for(Explanation d : explanation.getDetails())
			logExplanation(d, indent+"  ");
	}
	
	private static List<String> analysis(org.apache.lucene.analysis.Analyzer analyzer, String fieldName, String text) {
		List<String> terms = new ArrayList<String>();
		TokenStream stream = analyzer.tokenStream(fieldName, text);
		try {
			stream.reset();
			while(stream.incrementToken()) {
				CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);
				String term = termAtt.toString();
				terms.add(term);
			}
			stream.end();
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}finally {
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
		return terms;
	}
	
	private static Query buildPhraseQuery(IndexReader indexReader, Analyzer analyzer, String fieldName, String queryText, int slop) {
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		builder.setSlop(2); //? max is 2;
		List<String> terms = analysis(analyzer, fieldName, queryText);
		log.info("terms: "+StringUtils.join(terms, ", "));
		for(String termKeyword : terms) {
			Term term = new Term(fieldName, termKeyword);
			builder.add(term);
		}
		Query query = builder.build();
		return query;
	}

	private static Query buildBooleanANDQuery(Analyzer analyzer, String fieldName, String queryText) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		List<String> terms = analysis(analyzer, fieldName, queryText);
		log.info("terms: "+StringUtils.join(terms, ", "));
		for(String termKeyword : terms) {
			Term term = new Term(fieldName, termKeyword);
			builder.add(new TermQuery(term), BooleanClause.Occur.MUST);
		}
		return builder.build();
	}
	
	private static Set<String> loadTerms(IndexReader indexReader, String fieldName) {
		Set<String> termSet = new HashSet<String>();
		try {
			Terms terms = MultiTerms.getTerms(indexReader, fieldName);
			if(terms==null)
				return termSet; //fieldName没有索引到有效的terms;
			TermsEnum it = terms.iterator();
			BytesRef byteRef = null;
		    while((byteRef = it.next()) != null){
		        String term = byteRef.utf8ToString();
		        termSet.add(term);
		    }
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage(), e);
		}
	    return termSet;
	}
}
