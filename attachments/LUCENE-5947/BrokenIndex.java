package brokenIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.lucene410.Lucene410Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class BrokenIndex {
    public static void main(String[] args) throws Exception {
        Path path = Files.createTempDirectory("lucene_");
        Runtime.getRuntime().addShutdownHook(new Thread(new DirectoryCleaner(path)));

        FSDirectory dir = FSDirectory.open(path.toFile());
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_0, new StandardAnalyzer())
            .setMergePolicy(NoMergePolicy.INSTANCE)
            .setCodec(new Lucene410Codec());
        try (IndexWriter iw = new IndexWriter(dir, iwc)) {
                for (int i = 0; i < 10; i++) {
                    Document doc = new Document();
                    doc.add(new IntField("id", i, Store.NO));
                    iw.addDocument(doc);
                    iw.commit();
                }
            }

        for (Path f : Files.newDirectoryStream(path)) {
            String fname = f.getFileName().toString();
            if (fname.endsWith(".si")) {
                Files.delete(f);
                break;
            }
        }
        CheckIndex.main(new String[] {"-fix", path.toString()});
    }

    private static class DirectoryCleaner implements Runnable {
        private final Path path;
        DirectoryCleaner(Path path) {
            this.path = path;
        }

        public void run() {
            try {
                for (Path f : Files.newDirectoryStream(path)) {
                    Files.delete(f);
                }
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
