import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.DataFormatException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.compressing.Compressing40Codec;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.compress.CompressionFormat;
import org.apache.lucene.util.compress.Deflate;
import org.apache.lucene.util.compress.LZ4;


public class CompressionBenchmark {

    private static int CHUNK_SIZE = 1;

    private static final int N_DOCS = 100000;
    private static final int N_GET_DOCS = 10000;

    static class Doc {
        String text;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Doc other = (Doc) obj;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Doc[text=" + text + "]";
        }
    }

    public static void getDocuments(File root, EnumSet<IndexFormat> formats) throws IOException, DataFormatException {
        Random r = new Random();
        int[] docIDs = new int[N_GET_DOCS];
        for (int i = 0; i < docIDs.length; ++i) {
            docIDs[i] = r.nextInt(N_DOCS);
        }
        Doc[] docs = new Doc[docIDs.length];
        for (int i = 0; i < docs.length; ++i) {
            docs[i] = new Doc();
        }
        Map<IndexFormat, Long> times = new LinkedHashMap<IndexFormat, Long>();
        for (int k = 0; k < 20; ++k) {
            for (IndexFormat f : formats) {
                for (int i = 0; i < docs.length; ++i) {
                    docs[i].text = null;
                }
                File dir = new File(root, f.toString());
                if (!dir.exists()) {
                    System.out.println("No index for " + f);
                    continue;
                }
                FSDirectory d = FSDirectory.open(dir);
                final DirectoryReader reader = DirectoryReader.open(d);
                final long start = System.nanoTime();
                for (int i = 0; i < docIDs.length; ++i) {
                    final Document doc = reader.document(docIDs[i]);
                    switch (f) {
                    case COMPRESSED_DOC_DEFLATE1:
                    case COMPRESSED_DOC_DEFLATE9:
                        BytesRef compressedText = doc.getBinaryValue("text");
                        docs[i].text = CompressionTools.decompressString(compressedText.bytes, compressedText.offset, compressedText.length);
                        break;
                    default:
                        docs[i].text = doc.get("text");
                    }
                }
                final long end = System.nanoTime();
                Long previous = times.get(f);
                if (previous == null || previous.longValue() > (end-start)) {
                    times.put(f, end-start);
                }
                System.out.println((end-start) + "\t\t" + f + "\thashCode: " + Arrays.deepHashCode(docs));
            }
            System.out.println();
        }
        for (Map.Entry<IndexFormat, Long> entry : times.entrySet()) {
            System.out.println(entry.getValue() + "\t\t" + entry.getKey());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, DataFormatException {
        File root = new File("/tmp/indexes");
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("Cannot create " + root);
        }
        EnumSet<IndexFormat> formats = EnumSet.allOf(IndexFormat.class);
        for (IndexFormat f : formats) {
            long start = System.nanoTime();
            index(new File("/home/jpountz/lucene-tests/data/enwiki-20120502-lines-1k.txt"), root, f);
            long end = System.nanoTime();
            System.out.println(f + "\t\t" + (end-start)/1000000);
        }
        getDocuments(root, formats);
    }

    enum IndexFormat {
        UNCOMPRESSED,
        COMPRESSED_DOC_DEFLATE1,
        COMPRESSED_DOC_DEFLATE9,
        COMPRESSED_INDEX_DEFLATE1,
        COMPRESSED_INDEX_DEFLATE9,
        COMPRESSED_INDEX_LZ4,
        COMPRESSED_INDEX_LZ4_HC;
    }

    public static void index(File input, File root, final IndexFormat f) throws IOException {
        Analyzer a = new StandardAnalyzer(Version.LUCENE_50);
        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_50, a);
        conf.setOpenMode(OpenMode.CREATE);

        CompressionFormat format;
        switch (f) {
        case COMPRESSED_INDEX_DEFLATE1:
            format = new Deflate.Format(1);
            break;
        case COMPRESSED_INDEX_DEFLATE9:
            format = new Deflate.Format(9);
            break;
        case COMPRESSED_INDEX_LZ4:
            format = new LZ4.FastScanFormat();
            break;
        case COMPRESSED_INDEX_LZ4_HC:
            format = new LZ4.HighCompressionFormat();
            break;
        default:
            format = null;
        }
        if (format != null) {
            conf.setCodec(new Compressing40Codec(format, CHUNK_SIZE));
        }

        conf.setRAMBufferSizeMB(48);

        final Directory d = FSDirectory.open(new File(root, f.toString()));
        final IndexWriter iw = new IndexWriter(d, conf);

        final Document doc = new Document();
        FieldType fieldType = new FieldType(StringField.TYPE_STORED);
        fieldType.setIndexed(false);
        final Field text;
        switch (f) {
        case COMPRESSED_DOC_DEFLATE1:
        case COMPRESSED_DOC_DEFLATE9:
            text = new Field("text", BytesRef.EMPTY_BYTES, fieldType);
            break;
        default:
            text = new Field("text", "", fieldType);
            break;
        }
        doc.add(text);

        BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
        int i = 0;
        rd.readLine(); // FIELDS_HEADER_INDICATOR###
        for (String line = rd.readLine(); line != null; line = rd.readLine()) {
            if (!line.isEmpty()) {
                String[] parts = line.split("\t", 3);
                if (parts.length == 3) {
                    switch (f) {
                    case COMPRESSED_DOC_DEFLATE1:
                        text.setBytesValue(CompressionTools.compressString(parts[2], 1));
                        break;
                    case COMPRESSED_DOC_DEFLATE9:
                        text.setBytesValue(CompressionTools.compressString(parts[2], 9));
                        break;
                    default:
                        text.setStringValue(parts[2]);
                    }

                    iw.addDocument(doc);
                    ++i;
                }
            }
            if (i % N_DOCS == 0) {
                break;
            }
        }
        rd.close();
        iw.commit();
        iw.forceMerge(1);
        iw.close();
    }
}
