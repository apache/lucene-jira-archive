package sample;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

public class Test {
  
  static Directory dir = new RAMDirectory();
  static Analyzer analyzer = new WhitespaceAnalyzer( Version.LUCENE_40 );
  //static Analyzer analyzer = new StopAnalyzer( Version.LUCENE_40 );
  static final String F = "f";

  public static void main(String[] args) throws IOException {
    makeIndex();
    printTermVectors();
  }

  static void makeIndex() throws IOException {
    IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_40, analyzer );
    IndexWriter writer = new IndexWriter( dir, config );
    Document doc = new Document();
    doc.add( new Field( F, "Mike", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    doc.add( new Field( F, "will", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    //doc.add( new Field( F, "", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    doc.add( new Field( F, "use", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    //doc.add( new Field( F, "will use", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    doc.add( new Field( F, "Lucene", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    writer.addDocument( doc );
    writer.close();
  }
  
  static void printTermVectors() throws IOException {
    IndexReader reader = IndexReader.open( dir );
    TermFreqVector tfv = reader.getTermFreqVector( 0, F );
    TermPositionVector tpv = (TermPositionVector)tfv;
    BytesRef[] terms = tpv.getTerms();
    for( int i = 0; i < terms.length; i++ ){
      System.out.print( terms[i].utf8ToString() );
      TermVectorOffsetInfo[] ois = tpv.getOffsets( i );
      for( int j = 0; j < ois.length; j++ ){
        System.out.println( "(" + ois[j].getStartOffset() +
            "," + ois[j].getEndOffset() + ")" );
      }
    }
    reader.close();
  }
}
