package org.apache.lucene.test;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.FSDirectory;

public class BadSegmentsFileTest extends TestCase
{

	private FSDirectory dir;
//	File badSegmentFile=new File("badSegmentsFile");
	File badSegmentsDir=new File("corruptSegmentFiles");
	private File indexDir;

	@Override
	protected void setUp() throws Exception
	{
		if(!badSegmentsDir.exists())
		{
			throw new RuntimeException("missing bad segments directory");
		}
		if(!badSegmentsDir.isDirectory())
		{
			throw new RuntimeException("missing bad segments directory");
		}

		indexDir=new File(System.getProperty("java.io.tmpdir"),"badSegment");
		if(indexDir.exists())
		{
			File[] oldFiles = indexDir.listFiles();
			for (File file : oldFiles)
			{
				file.delete();
			}
		}
		dir=FSDirectory.open(indexDir);
		IndexWriter w=new IndexWriter(dir,new WhitespaceAnalyzer(),true,MaxFieldLength.UNLIMITED);
		Document doc=new Document();
		w.addDocument(doc);
		w.commit();
		w.close();

	}

	public void testBadSegment() throws Exception
	{
		IndexReader r=IndexReader.open(dir,true);
		assertEquals(r.maxDoc(), 1);
		IndexReader[] sr = r.getSequentialSubReaders();
		assertEquals(1,sr.length);
		long nextGen=SegmentInfos.getCurrentSegmentGeneration(dir)+1;
		r.close();

		SegmentInfos.setInfoStream(System.out);
		File[] badSegs = badSegmentsDir.listFiles();
		for (File badSeg : badSegs)
		{
			System.out.println("\nTrying to read with bad segment file "+badSeg);
			copyFile(badSeg,new File(indexDir,"segments_"+nextGen));
			try
			{
				r=IndexReader.open(dir,true);
				assertEquals("The corrupt segments file "+
						badSeg
						+"should be safely ignored and existing content remain intact",1,r.maxDoc());
				r.close();
			}
			catch(Exception expected)
			{
				//ignore
			}

		}

	}

	 static void copyFile(File inputFile, File outputFile) throws Exception
	 {
		    FileReader in = new FileReader(inputFile);
		    FileWriter out = new FileWriter(outputFile);
		    int c;
		    while ((c = in.read()) != -1)
		      out.write(c);

		    in.close();
		    out.close();

	}

}
