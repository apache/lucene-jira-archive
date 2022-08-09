package org.apache.lucene.spatial.tier;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.search.function.FieldScoreQuery;
import org.apache.lucene.search.function.FieldScoreQuery.Type;
import org.apache.lucene.spatial.geohash.GeoHashUtils;
import org.apache.lucene.spatial.geometry.FloatLatLng;
import org.apache.lucene.spatial.geometry.LatLng;
import org.apache.lucene.spatial.geometry.shape.LLRect;
import org.apache.lucene.spatial.tier.DistanceFieldComparatorSource;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.NumericUtils;

public class BoundingBoxCalucationIssueTest extends TestCase {
    
	private Directory directory;
	private IndexSearcher searcher;
	private List<CartesianTierPlotter> ctps = new LinkedList<CartesianTierPlotter>();
	private String geoHashPrefix = "geohash";
	private IProjector project = new SinusoidalProjector();
	
	
	private static final double testRadius = 31;
	private double searchLat = 52.304567;
	private double searchLng = 4.647118;
	
	
	protected void setUp() throws IOException {
		
		directory = new RAMDirectory();
	    
	    IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
	    setUpPlotter( 2, 15);
	    addData(writer);
	    searcher = new IndexSearcher(directory,true);
	    System.out.println("setup");
	}
	 
	private void setUpPlotter(int base, int top) {
	    for (; base <= top; base ++){
	      ctps.add(new CartesianTierPlotter(base,project,
	          CartesianTierPlotter.DEFALT_FIELD_PREFIX));
	    }
	  }
	
	private void addPoint(IndexWriter writer, String name, double lat, double lng) throws IOException {
	    
	    Document doc = new Document();
	    
	    doc.add(new Field("name", name,Field.Store.YES, Field.Index.ANALYZED));
	    // add a default meta field to make searching all documents easy 
	    doc.add(new Field("metafile", "doc",Field.Store.YES, Field.Index.ANALYZED));
	    
	    int ctpsize = ctps.size();
	    for (int i =0; i < ctpsize; i++){
	      CartesianTierPlotter ctp = ctps.get(i);
	      
	      doc.add(new Field(ctp.getTierFieldName(), 
	          NumericUtils.doubleToPrefixCoded(ctp.getTierBoxId(lat,lng)),
	          Field.Store.YES, 
	          Field.Index.NOT_ANALYZED_NO_NORMS));
	      
	      doc.add(new Field(geoHashPrefix, GeoHashUtils.encode(lat,lng), 
	    		  Field.Store.YES, 
	    		  Field.Index.NOT_ANALYZED_NO_NORMS));
	    }
	    writer.addDocument(doc);
	    
	  }
	 
	private void addData(IndexWriter writer) throws IOException {
		    addPoint(writer,"A GREAT LOCATION",52.0952131,5.1287664);
		    writer.commit();
		    writer.close();
		  }
	
	public void testBasicSearchHitsWithLucene() throws Exception {
		//Search point Coordinates
		
		
		//The distance between my search point and target is calculated as  25.03... miles
		//So both searches below - radius 31 and 32 should work.
		//32 does, 31 does not
		
		//the various radius to test with               
		final double[] milesToTest = new double[] {32,31};
		//and corresponding expected results
		final int[] expectedHitCount = new int[]  {1,1};
		
		for(int x=0;x<expectedHitCount.length;x++) {
		    System.out.println("testing for distance : "+milesToTest[x]);
		    
			final double miles = milesToTest[x];
			final DistanceQueryBuilder dq = new DistanceQueryBuilder(searchLat, searchLng, miles, 
			        "geohash", CartesianTierPlotter.DEFALT_FIELD_PREFIX, true);
			
			Query query = new TermQuery(new Term("metafile","doc"));
	
			FieldScoreQuery fsQuery = new FieldScoreQuery("geo_distance", Type.FLOAT);
		    CustomScoreQuery customScore = new CustomScoreQuery(query,fsQuery) {
			      
		        @Override
		          public float customScore(int doc, float subQueryScore, float valSrcScore){
		         // System.out.println(doc);
		          if (dq.getDistanceFilter().getDistance(doc) == null)
		            return 0;
			        
		          double distance = dq.getDistanceFilter().getDistance(doc);
		          // boost score shouldn't exceed 1
		          if (distance < 1.0d)
		            distance = 1.0d;
		          //boost by distance is invertly proportional to
		          // to distance from center point to location
		          float score = new Float((miles - distance) / miles ).floatValue();
		          return score * subQueryScore;
		        }
		      };
		    
		    // Create a distance sort
		    // As the radius filter has performed the distance calculations
		    // already, pass in the filter to reuse the results.
		    // 
		    DistanceFieldComparatorSource dsort = new DistanceFieldComparatorSource(dq.getDistanceFilter());
		    //Sort sort = new Sort(new SortField("geo_distance", dsort));
			    
		    // Perform the search, using the term query, the serial chain filter, and the
		    // distance sort
		    
		    TopDocs tops = searcher.search(customScore,dq.getFilter(),1);
		    
		    
		    
		    System.out.println(tops.scoreDocs.length);
		    if (tops.totalHits > 0){
		    	System.out.println(tops.scoreDocs[0].doc);
		    	System.out.println(dq.getDistanceFilter().getDistance(tops.scoreDocs[0].doc));
		    }
		    
		    
		    assertEquals(expectedHitCount[x], tops.totalHits);
		   
		}
		
		
	}
	
	/*
	
	The test below prints out the dimensions of the calculated bounding box in LLRect.java for a search radius of 31 miles
	
	Without the fix :
	
	The diameter of search circle is 62.  The diagonal of the box is calcualted to be 62.
	So the box height and width are smaller indicating the calculated bounding box is contained within the circle.  In this case we are potentially 
	excluding data from our search results
	
	Diameter62.00320716628395
	H43.84154153329628
	W44.15872656967801

	With the fix:
	The box height and width are now the same as the diameter of the search circle. The diagaonal of the box is bigger than the diameter of the circle
	The calculated bounding box completely contains the circle.

	Diameter87.68577648515755
	H61.99939800104835
	W62.6365561535134

	Of course this means we will have data points in our box that are not interesting for our search (around the corners) but I'd rather have more data than miss data.
	And by using a more precise filter (boolean flag on DistanceQueryBuilder) we will exclude those points anyway.
	
	The fix is a one liner in LLRect.java
	
		public static LLRect createBox(LatLng center, double widthMi, double heightMi) {
		  
		    double d = widthMi;
		    
		    //Calculate the parameter value required to obtain a bounding box that CONTAINS the search circle
		    //For that we want the diagonal of the box to be equal to radius of the search circle.
		    //Knowing that we can calculate the value to be passed thru.
		    //Note the param names are misleading - they are not widths or heights but half those values. Ie our search radius.
		    >>>>>>>>> d = widthMi*Math.sqrt(2.0);
		    
		    LatLng ur = boxCorners(center, d, 45.0); // assume right angles
		    LatLng ll = boxCorners(center, d, 225.0);
		
		    return new LLRect(ll, ur);
	  	}
	
	
	 */
	
	public void testBoxDimensions(){
		
		LLRect box1 = LLRect.createBox( new FloatLatLng(searchLat,searchLng),testRadius,testRadius);
	    LatLng ll = box1.getLowerLeft();
	    LatLng ur = box1.getUpperRight();
	 
	    //Diagonal of the box should be more than the diameter of the search circle  
        double dis = DistanceUtils.getInstance().getDistanceMi(ll.getLat(), ll.getLng(), ur.getLat(), ur.getLng());
        System.out.println("Diameter"+dis);
        
        //height of box - should be equal to the 
	    dis = DistanceUtils.getInstance().getDistanceMi(ll.getLat(),  ll.getLng(), ur.getLat(), ll.getLng());
    	System.out.println("H"+dis);
    	
    	//width of box
    	dis = DistanceUtils.getInstance().getDistanceMi(ll.getLat(),  ll.getLng(), ll.getLat(), ur.getLng());
    	System.out.println("W"+dis);

	}
	
	
	
	}
