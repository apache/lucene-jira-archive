
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterCodecReader;
import org.apache.lucene.index.FilteredTermsEnum;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;

public class Lucene6826
{
    public static void main(String[] args) throws Exception
    {
        try (Directory inputDirectory = makeInputIndex();
             DirectoryReader reader = DirectoryReader.open(inputDirectory);
             Directory outputDirectory = new RAMDirectory();
             IndexWriter writer = new IndexWriter(outputDirectory, new IndexWriterConfig(new KeywordAnalyzer())))
        {
            List<LeafReaderContext> leaves = reader.leaves();
            CodecReader[] codecReaders = new CodecReader[leaves.size()];
            for (int i = 0; i < leaves.size(); i++)
            {
                codecReaders[i] = new MigratingCodecReader((CodecReader) leaves.get(i).reader());
            }

            writer.addIndexes(codecReaders); // <- bang
        }
    }

    private static Directory makeInputIndex() throws Exception
    {
        Directory directory = new RAMDirectory();
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new KeywordAnalyzer())))
        {
            Document document = new Document();
            document.add(new StringField("deleted", "0", Field.Store.YES));
            writer.addDocument(document);
            writer.commit();
        }
        return directory;
    }

    private static class MigratingCodecReader extends FilterCodecReader
    {
        MigratingCodecReader(CodecReader in)
        {
            super(in);
        }

        @Override
        public FieldsProducer getPostingsReader()
        {
            return new MigratingFieldsProducer(super.getPostingsReader(), getFieldInfos());
        }

        private static class MigratingFieldsProducer extends BaseMigratingFieldsProducer
        {
            MigratingFieldsProducer(FieldsProducer delegate, FieldInfos newFieldInfo)
            {
                super(delegate, newFieldInfo);
            }

            @Override
            public Terms terms(String field) throws IOException
            {
                if ("deleted".equals(field))
                {
                    Terms deletedTerms = super.terms("deleted");
                    if (deletedTerms != null)
                    {
                        return new ValueFilteredTerms(deletedTerms, new BytesRef("1"));
                    }

                    return null;
                }
                else
                {
                    return super.terms(field);
                }
            }

            @Override
            protected FieldsProducer create(FieldsProducer delegate, FieldInfos newFieldInfo)
            {
                return new MigratingFieldsProducer(delegate, newFieldInfo);
            }

            private static class ValueFilteredTerms extends Terms
            {
                private final Terms delegate;
                private final BytesRef value;

                public ValueFilteredTerms(Terms delegate, BytesRef value)
                {
                    this.delegate = delegate;
                    this.value = value;
                }

                @Override
                public TermsEnum iterator() throws IOException
                {
                    return new FilteredTermsEnum(delegate.iterator())
                    {
                        @Override
                        protected AcceptStatus accept(BytesRef term)
                        {
                            int comparison = term.compareTo(value);
                            if (comparison < 0)
                            {
                                // I don't think it will actually get here because they are supposed to call nextSeekTerm
                                // to get the initial term to seek to.
                                return AcceptStatus.NO_AND_SEEK;
                            }
                            else if (comparison > 0)
                            {
                                return AcceptStatus.END;
                            }
                            else // comparison == 0
                            {
                                return AcceptStatus.YES;
                            }
                        }

                        @Override
                        protected BytesRef nextSeekTerm(BytesRef currentTerm)
                        {
                            if (currentTerm == null || currentTerm.compareTo(value) < 0)
                            {
                                return value;
                            }

                            return null;
                        }
                    };
                }

                @Override
                public long size() throws IOException
                {
                    // Docs say we can return -1 if we don't know.
                    return -1;
                }

                @Override
                public long getSumTotalTermFreq() throws IOException
                {
                    // Docs say we can return -1 if we don't know.
                    return -1;
                }

                @Override
                public long getSumDocFreq() throws IOException
                {
                    // Docs say we can return -1 if we don't know.
                    return -1;
                }

                @Override
                public int getDocCount() throws IOException
                {
                    // Docs say we can return -1 if we don't know.
                    return -1;
                }

                @Override
                public boolean hasFreqs()
                {
                    return delegate.hasFreqs();
                }

                @Override
                public boolean hasOffsets()
                {
                    return delegate.hasOffsets();
                }

                @Override
                public boolean hasPositions()
                {
                    return delegate.hasPositions();
                }

                @Override
                public boolean hasPayloads()
                {
                    return delegate.hasPayloads();
                }
            }

        }

        private static class BaseMigratingFieldsProducer extends FieldsProducer
        {
            private final FieldsProducer delegate;
            private final FieldInfos newFieldInfo;

            public BaseMigratingFieldsProducer(FieldsProducer delegate, FieldInfos newFieldInfo)
            {
                this.delegate = delegate;
                this.newFieldInfo = newFieldInfo;
            }

            @Override
            public Iterator<String> iterator()
            {
                Iterator<FieldInfo> fieldInfoIterator = newFieldInfo.iterator();
                return new Iterator<String>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return fieldInfoIterator.hasNext();
                    }

                    @Override
                    public String next()
                    {
                        return fieldInfoIterator.next().name;
                    }
                };
            }

            @Override
            public int size()
            {
                return newFieldInfo.size();
            }

            @Override
            public Terms terms(String field) throws IOException
            {
                return delegate.terms(field);
            }

            @Override
            public FieldsProducer getMergeInstance() throws IOException
            {
                return create(delegate.getMergeInstance(), newFieldInfo);
            }

            protected FieldsProducer create(FieldsProducer delegate, FieldInfos newFieldInfo)
            {
                return new BaseMigratingFieldsProducer(delegate, newFieldInfo);
            }

            @Override
            public void checkIntegrity() throws IOException
            {
                delegate.checkIntegrity();
            }

            @Override
            public long ramBytesUsed()
            {
                return delegate.ramBytesUsed();
            }

            @Override
            public Collection<Accountable> getChildResources()
            {
                return delegate.getChildResources();
            }

            @Override
            public void close() throws IOException
            {
                delegate.close();
            }
        }
    }
}
