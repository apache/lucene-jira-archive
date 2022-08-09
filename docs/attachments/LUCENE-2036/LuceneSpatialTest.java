package test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.NumericUtils;
import org.junit.Before;
import org.junit.Test;

public class LuceneSpatialTest {
	String latField = "lat";
	String lngField = "lon";
	String tierPrefix = "_localTier";

	private Directory directory;
	private IndexWriter writer;

	@Before
	public void setUp() throws IOException {
		directory = new RAMDirectory();
		writer = new IndexWriter(directory, new WhitespaceAnalyzer(), MaxFieldLength.UNLIMITED);
		addData();
	}

	@Test
	public void testLocalRemoteSpatialSearch() throws Exception {		
		// There should be 3 entries
		
		// This mimics local
		assertEquals(3, findNear("Restaurant", 38.8725000, -77.3829000, 8, false).size());
		
		// This mimics remote
		// Currently it throws a NPE!
		assertEquals(3, findNear("Restaurant", 38.8725000, -77.3829000, 8, true).size());
	}

	/**
	 * Returns a list of results
	 * 
	 * @param what
	 * @param latitude
	 * @param longitude
	 * @param radius
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	public List<String> findNear(String what, double latitude, double longitude, double radius, boolean serializeObjects) throws Exception {
		IndexSearcher searcher = new IndexSearcher(directory, true);

		DistanceQueryBuilder dq;
		dq = new DistanceQueryBuilder(latitude, longitude, radius, latField, lngField, tierPrefix, true);

		Query tq;
		if (what == null)
			tq = new TermQuery(new Term("metafile", "doc"));
		else
			tq = new TermQuery(new Term("name", what));

		DistanceFieldComparatorSource dsort;
		dsort = new DistanceFieldComparatorSource(dq.getDistanceFilter());
		Sort sort = new Sort(new SortField("foo", dsort));
		Filter filter = dq.getFilter();
		
		
		// Here I am mimicking the effect Java RMI has if I were to do a remote Searcher 
		// by Serialzing the objects and reading them back from memory
		// the search SHOULD WORK given that these classes are serializable
		// Unfortunately I've passed a reference from the Filter to the Sort so when
		// I serialize I end up breaking the underlying logic...
		if (serializeObjects) {
			tq = (Query) readObjectFromSerialization(tq);
			filter = (Filter) readObjectFromSerialization(filter);
			sort = (Sort) readObjectFromSerialization(sort);
		}

		
		TopDocs hits = searcher.search(tq, filter, 10, sort);

		int numResults = hits.totalHits;

		Map<Integer, Double> distances = dq.getDistanceFilter().getDistances();

		ArrayList<String> results = new ArrayList<String>();
		System.out.println("Number of results: " + numResults);
		System.out.println("Found:");
		for (int i = 0; i < numResults; i++) {
			int docID = hits.scoreDocs[i].doc;
			Document d = searcher.doc(docID);

			String name = d.get("name");
			double rsLat = NumericUtils.prefixCodedToDouble(d.get(latField));
			double rsLng = NumericUtils.prefixCodedToDouble(d.get(lngField));
			Double geo_distance = distances.get(docID);

			System.out.printf(name + ": %.2f Miles\n", geo_distance);
			System.out.println("\t\t(" + rsLat + "," + rsLng + ")");

			// Add results
			results.add(name);
		}

		return results;
	}
	
	private Object readObjectFromSerialization(Serializable o) throws Exception{
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			
			ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bis);
			
			return ois.readObject();
		}catch(NotSerializableException e){
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Adds a location
	 * 
	 * @param writer
	 * @param name
	 * @param lat
	 * @param lng
	 * @throws IOException
	 */
	private void addLocation(IndexWriter writer, String name, double lat, double lng) throws IOException {

		Document doc = new Document();
		doc.add(new Field("name", name, Field.Store.YES, Field.Index.ANALYZED));

		doc.add(new Field(latField, NumericUtils.doubleToPrefixCoded(lat), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field(lngField, NumericUtils.doubleToPrefixCoded(lng), Field.Store.YES, Field.Index.NOT_ANALYZED));

		doc.add(new Field("metafile", "doc", Field.Store.YES, Field.Index.ANALYZED));

		IProjector projector = new SinusoidalProjector();

		int startTier = 5;
		int endTier = 15;

		for (; startTier <= endTier; startTier++) {
			CartesianTierPlotter ctp;
			ctp = new CartesianTierPlotter(startTier, projector, tierPrefix);

			double boxId = ctp.getTierBoxId(lat, lng);
			System.out.println("Adding field " + ctp.getTierFieldName() + ":" + boxId);
			doc.add(new Field(ctp.getTierFieldName(), NumericUtils.doubleToPrefixCoded(boxId), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));

		}

		writer.addDocument(doc);
		System.out.println("===== Added Doc to index ====");
	}

	/**
	 * Add dummy data
	 * 
	 * @throws IOException
	 */
	private void addData() throws IOException {
		addLocation(writer, "McCormick & Schmick's Seafood Restaurant", 38.9579000, -77.3572000);
		addLocation(writer, "Jimmy's Old Town Tavern", 38.9690000, -77.3862000);
		addLocation(writer, "Ned Devine's", 38.9510000, -77.4107000);
		addLocation(writer, "Old Brogue Irish Pub", 38.9955000, -77.2884000);
		addLocation(writer, "Alf Laylah Wa Laylah", 38.8956000, -77.4258000);
		addLocation(writer, "Sully's Restaurant & Supper", 38.9003000, -77.4467000);
		addLocation(writer, "TGIFriday", 38.8725000, -77.3829000);
		addLocation(writer, "Potomac Swing Dance Club", 38.9027000, -77.2639000);
		addLocation(writer, "White Tiger Restaurant", 38.9027000, -77.2638000);
		addLocation(writer, "Jammin' Java", 38.9039000, -77.2622000);
		addLocation(writer, "Potomac Swing Dance Club", 38.9027000, -77.2639000);
		addLocation(writer, "WiseAcres Comedy Club", 38.9248000, -77.2344000);
		addLocation(writer, "Glen Echo Spanish Ballroom", 38.9691000, -77.1400000);
		addLocation(writer, "Whitlow's on Wilson", 38.8889000, -77.0926000);
		addLocation(writer, "Iota Club and Cafe", 38.8890000, -77.0923000);
		addLocation(writer, "Hilton Washington Embassy Row", 38.9103000, -77.0451000);
		addLocation(writer, "HorseFeathers, Bar & Grill", 39.01220000000001, -77.3942);
		writer.close();
	}
}
