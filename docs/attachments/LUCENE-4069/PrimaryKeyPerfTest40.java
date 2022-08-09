package test.bloom;
/*
 * Copyright Michael McCandless
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of
 * the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See
 * the License for the specific language governing
 * permissions and limitations under the License.
*/

// echo 1 > /proc/sys/vm/drop_caches; javac -Xlint:deprecation -cp build/lucene-core-4.0-dev.jar:../modules/analysis/build/common/lucene-analyzers-common-4.0-dev.jar PrimaryKeyPerfTest40.java ; java -Xmx2g -Xms2g -cp .:build/lucene-core-4.0-dev.jar:../modules/analysis/build/common/lucene-analyzers-common-4.0-dev.jar PrimaryKeyPerfTest40 /x/lucene/testidx 1000

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.bloom.BloomFilteringPostingsFormat;
import org.apache.lucene.codecs.lucene40.Lucene40Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.hash.MurmurHash2;

// Adapted from example here:  http://blog.mikemccandless.com/2010/06/lucenes-pulsingcodec-on-primary-key.html
public class PrimaryKeyPerfTest40 {

  // How many unique terms to create in the index
  final private static int INDEX_TERM_COUNT = 10000000;

  // Best of N iterations
  final private static int TEST_ITER = 1;

  // Length (in chars) of each unique term
  final private static int TERM_LENGTH = 9;

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("\nUsage: java PrimaryKeyPerfTest40 /path/to/index testTermCount\n");
      System.exit(1);
    }

    final File standardPath = new File(args[0] + ".standard");
    final Directory standardDir = FSDirectory.open(standardPath);

    final int testTermCount = Integer.parseInt(args[1]);

    final File bloomedPath = new File(args[0] + ".bloomed");
    final Directory bloomedDir = FSDirectory.open(bloomedPath);

    final File pulsingPath = new File(args[0] + ".pulsing");
    final Directory pulsingDir = FSDirectory.open(pulsingPath);

    final File bloomedPulsedPath = new File(args[0] + ".bloompulsing");
    final Directory bloomedPulsedDir = FSDirectory.open(bloomedPulsedPath);
    
    
    
    // raw byte block hold all bytes for all random terms
    final byte[] termData = createTermData();

    //Standard Codec Index
    final Codec standardCodec= Codec.forName("Lucene40");
    if (!standardPath.exists()) {
      System.out.println("\nCreate 'standard' index with " + INDEX_TERM_COUNT + " terms...");
      createIndex(standardDir, termData, standardCodec);
    }

    //Pulsed Codec Index
    final PostingsFormat pulsing = PostingsFormat.forName("Pulsing40");
    final PostingsFormat standardPostings= PostingsFormat.forName("Lucene40");
    
    if (!pulsingPath.exists()) {
      System.out.println("\nCreate 'Pulsing' index with " + INDEX_TERM_COUNT + " terms...");
      createIndex(pulsingDir, termData, new Lucene40Codec(){
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {
          return pulsing;
        }
        
      });
    }
    
    
    //BloomFiltered Standard Codec Index
    if (!bloomedPath.exists()) {
      System.out.println("\nCreate 'BloomFiltered' index with " + INDEX_TERM_COUNT + " terms...");
      createIndex(bloomedDir, termData, new Lucene40Codec(){
        PostingsFormat bloomFormat=new BloomFilteringPostingsFormat(standardPostings);
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {
          return bloomFormat;
        }
        
      });
    }
    
    //BloomFiltered Pulsed  Codec Index
    if (!bloomedPulsedPath.exists()) {
      System.out.println("\nCreate 'BloomFiltered' and pulsed index with " + INDEX_TERM_COUNT + " terms...");
      createIndex(bloomedPulsedDir, termData, new Lucene40Codec(){
        PostingsFormat bloomedPulsedFormat=new BloomFilteringPostingsFormat(pulsing);
        @Override
        public PostingsFormat getPostingsFormatForField(String field) {
          return bloomedPulsedFormat;
        }
        
      });
    }
    

    //Doing a single loop of the tests was not sufficient (presumably because of Hotspotting differences)
    for (int i = 0; i < 100; i++) {
    {
      System.out.print("Standard test...");
      DirectoryReader reader = DirectoryReader.open(standardDir);
      try {
        System.out.println("  " + test(reader, termData, testTermCount) + " msec");
      } finally {
        reader.close();
      }
    }
    
    {
      System.out.print("Pulsing test...");
      DirectoryReader reader = DirectoryReader.open(pulsingDir);
      try {
        System.out.println("  " + test(reader, termData, testTermCount) + " msec");
      } finally {
        reader.close();
      }
    }    
    
    {
      System.out.print("Bloomed test...");
      DirectoryReader reader = DirectoryReader.open(bloomedDir);

      try {
        System.out.println("  " + test(reader, termData, testTermCount) + " msec");
      } finally {
        reader.close();
      }
    }
    
    
    {
      System.out.print("Bloomed Pulsed test...");
      DirectoryReader reader = DirectoryReader.open(bloomedPulsedDir);
      try {
        System.out.println("  " + test(reader, termData, testTermCount) + " msec");
      } finally {
        reader.close();
      }
    }    
    System.out.println();
    
    }    
    

    standardDir.close();
    pulsingDir.close();
    bloomedDir.close();
    bloomedPulsedDir.close();
  }

  private static byte[] createTermData() {
    System.out.println("\nCreate term data...");
    final Random r = new Random(42);
    final int numBytes = INDEX_TERM_COUNT*TERM_LENGTH;
    final byte[] termData = new byte[numBytes];
    for(int i=0;i<numBytes;i++) {
      termData[i] = (byte) r.nextInt(128);
    }
    return termData;
  }

  // returns time in msec
  private static double test(final DirectoryReader reader, final byte[] termData, int testTermCount) throws IOException {
    List<? extends AtomicReader> subreaders = reader.getSequentialSubReaders();
    TermsEnum termsEnums[]=new TermsEnum[subreaders.size()];
    DocsEnum docsEnums[] = new DocsEnum[subreaders.size()];
    for (int i = 0; i < termsEnums.length; i++) {
      termsEnums[i]=subreaders.get(i).fields().terms("field").iterator(null);
      termsEnums[i].next();//Otherwise unpositioned
      docsEnums[i]=termsEnums[i].docs(null, null, false);
    }
    // warmup
    final BytesRef br = new BytesRef();
    br.bytes = termData;
    br.length = TERM_LENGTH;
        for(int i=0;i<10;i++) {
          for(int idx=0;idx<100;idx++) {
            br.offset = idx * TERM_LENGTH;
            boolean termFound=false;
            //search in all segments until found
            for (int j = 0; j < termsEnums.length; j++) {
              TermsEnum termsEnum=termsEnums[j];
              DocsEnum docs = docsEnums[j];
              if(termsEnum.seekExact(br,true))
              {
                termFound=true;
                break;
              }
            }
            assert termFound;
        }
    }

    //Now run the test
    final Random r = new Random(611953);
    final long t0 = System.nanoTime();
    for(int t=0;t<testTermCount;t++) {
      final int idx = r.nextInt(INDEX_TERM_COUNT);
      br.offset = idx * TERM_LENGTH;
      boolean termFound=false;
      for (int j = 0; j < termsEnums.length; j++) {
        TermsEnum termsEnum=termsEnums[j];
        DocsEnum docs = docsEnums[j];
        if(termsEnum.seekExact(br,true))
        {
          termFound=true;
//          docs = termsEnum.docs(null, docs, false);
//          final int doc = docs.nextDoc();
          //MH not sure if this assert is valid? Was failing in my tests
//        assert doc == idx;
          break;
        }
      }
      assert termFound;
    }
    return (System.nanoTime() - t0)/1000000.;
  }

  private static void createIndex(final Directory dir,
                                         final byte[] termData,
                                         final Codec codecs) throws IOException {

    long start=System.currentTimeMillis();
    final IndexWriter w = new IndexWriter(dir,
                                          new IndexWriterConfig(Version.LUCENE_40,
                                                                new WhitespaceAnalyzer(Version.LUCENE_40))
                                          .setOpenMode(IndexWriterConfig.OpenMode.CREATE)
                                          .setCodec(codecs));
    ((TieredMergePolicy) w.getConfig().getMergePolicy()).setUseCompoundFile(false);
    
    final Document doc = new Document();
    final StringField field = new StringField("field", "",Store.YES);
    doc.add(field);

    final Random r = new Random(611953);

    final BytesRef br = new BytesRef();
    br.length = TERM_LENGTH;
    br.bytes = termData;
    for(int i=0;i<INDEX_TERM_COUNT;i++) {
      br.offset = i*TERM_LENGTH;
      field.setStringValue(br.utf8ToString());
      w.addDocument(doc);
    }
    w.close();
    long diff=System.currentTimeMillis()-start;
    System.out.println("Index creation using "+codecs.getName()+" took "+diff+" ms");
  }
}