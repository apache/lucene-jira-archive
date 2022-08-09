import java.io.File;
import java.io.IOException;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IOUtils;

public class TestCore_46 {
	private String taxonomyPath = null;

	private DirectoryTaxonomyWriter fsTaxoWriter = null;
	private DirectoryTaxonomyWriter memTaxoWriter = null;
	private RAMDirectory taxoRamDir = null;
		
	public TestCore_46(String path) {
		this.taxonomyPath = path + File.separator + "taxo";
	}

	public void open() throws IOException {
		System.out.println ("opening engine");
		
		if (fsTaxoWriter == null) {
			File taxonomy = new File(taxonomyPath);
			fsTaxoWriter = new DirectoryTaxonomyWriter(FSDirectory.open(taxonomy), OpenMode.CREATE_OR_APPEND) 
			{
				@Override
				protected IndexWriterConfig createIndexWriterConfig(OpenMode openMode) {
					IndexWriterConfig conf = super.createIndexWriterConfig(openMode);
					LogMergePolicy lmp = (LogMergePolicy) conf.getMergePolicy();
					lmp.setNoCFSRatio(1.0);
					conf.setUseCompoundFile(true);
					return conf;
				}
			};
			fsTaxoWriter.commit();
			taxoRamDir = new RAMDirectory();
			memTaxoWriter = new DirectoryTaxonomyWriter(taxoRamDir, OpenMode.CREATE_OR_APPEND) {
				@Override
				protected IndexWriterConfig createIndexWriterConfig(OpenMode openMode) {
					IndexWriterConfig conf = super.createIndexWriterConfig(openMode);
					LogMergePolicy lmp = (LogMergePolicy) conf.getMergePolicy();
					lmp.setNoCFSRatio(1.0);
					conf.setUseCompoundFile(true);
					return conf;
				}
			};
			memTaxoWriter.replaceTaxonomy(fsTaxoWriter.getDirectory());
			memTaxoWriter.commit();
		}
	}
	
	public void doInsert() throws IOException {
		fsTaxoWriter.replaceTaxonomy(memTaxoWriter.getDirectory());
		fsTaxoWriter.commit();
	}

	public void stop() throws IOException {
		System.out.println("stopping engine");
		IOUtils.close(memTaxoWriter, fsTaxoWriter, taxoRamDir);
		memTaxoWriter = null;
		fsTaxoWriter = null;
		taxoRamDir = null;
	}

	public static void main(String[] args) throws IOException {
		TestCore_46 engine = new TestCore_46("data");
		
		engine.open();
		engine.doInsert();
		engine.stop();
		
		engine.open();
		engine.stop();
	}
}		
