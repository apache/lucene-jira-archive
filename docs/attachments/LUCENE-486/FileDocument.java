package org.apache.lucene.document;
/**
 * Created by IntelliJ IDEA.
 * User: Grant Ingersoll
 * Date: Jan 10, 2006
 * Time: 4:28:59 PM
 * $Id:$
 * Copyright 2005.  Center For Natural Language Processing
 */


import java.io.File;
import java.io.FileReader;

/**
 *
 *
 **/
public class FileDocument {
  /**
   * Makes a document for a File.
   * <p/>
   * The document has three fields:
   * <ul>
   * <li><code>path</code>--containing the pathname of the file, as a stored,
   * untokenized field;
   * <li><code>modified</code>--containing the last modified date of the file as
   * a field as created by <a
   * href="lucene.document.DateTools.html">DateTools</a>; and
   * <li><code>contents</code>--containing the full contents of the file, as a
   * Reader field;
   */
  public static Document Document(File f)
          throws java.io.FileNotFoundException {

    // make a new, empty document
    Document doc = new Document();

    // Add the path of the file as a field named "path".  Use a field that is 
    // indexed (i.e. searchable), but don't tokenize the field into words.
    doc.add(new Field("path", f.getPath(), Field.Store.YES, Field.Index.UN_TOKENIZED));

    // Add the last modified date of the file a field named "modified".  Use 
    // a field that is indexed (i.e. searchable), but don't tokenize the field
    // into words.
    doc.add(new Field("modified",
            DateTools.timeToString(f.lastModified(), DateTools.Resolution.MINUTE),
            Field.Store.YES, Field.Index.UN_TOKENIZED));

    // Add the contents of the file to a field named "contents".  Specify a Reader,
    // so that the text of the file is tokenized and indexed, but not stored.
    // Note that FileReader expects the file to be in the system's default encoding.
    // If that's not the case searching for special characters will fail.
    doc.add(new Field("contents", new FileReader(f)));

    // return the document
    return doc;
  }

  private FileDocument() {
  }
}

