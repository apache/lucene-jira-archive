/*
 * ContentDependentFailure.java
 *
 * Created on 30 October 2003, 14:13
 */

package net.motile.pittwater.index.test;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

/**
 *
 * @author  esmond
 */
public class ContentDependentFailure
{
	
	/** Creates a new instance of ContentDependentFailure */
	public ContentDependentFailure(Reader reader) throws Exception
	{
		Directory	directory = FSDirectory.getDirectory("index",true);
		IndexWriter	writer = new IndexWriter(directory, new StandardAnalyzer(), true);
		// make a new, empty document
		Document doc = new Document();

		// Add the contents as a field named "content".
		// Use a Field.Text created with a Reader so that the text is tokenized and indexed
		// but not stored verbatim.
		doc.add(Field.Text("content", reader));

		writer.addDocument(doc);
		writer.close();
		directory.close();
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws Exception
	{
		new ContentDependentFailure(new InputStreamReader(new URL(args[0]).openStream()));
	}
}
