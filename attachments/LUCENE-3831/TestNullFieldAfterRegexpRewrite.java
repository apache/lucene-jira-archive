package org.apache.lucene.index.memory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

/**
 * Created by IntelliJ IDEA.
 * User: woody
 * Date: 28/02/2012
 * Time: 13:49
 * To change this template use File | Settings | File Templates.
 */
public class TestNullFieldAfterRegexpRewrite {
    
    private final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
    
    @Test
    public void showNullPointerException() throws IOException {
        RegexpQuery regex = new RegexpQuery(new Term("field", "worl."));
        SpanQuery wrappedquery = new SpanMultiTermQueryWrapper<RegexpQuery>(regex);
        
        MemoryIndex mindex = new MemoryIndex();
        mindex.addField("field", analyzer.tokenStream("field", new StringReader("hello there")));

        // This throws an NPE
        assert mindex.search(wrappedquery) == 0;
    }
    
    @Test
    public void showPassesIfWrapped() throws IOException {
        RegexpQuery regex = new RegexpQuery(new Term("field", "worl."));
        SpanQuery wrappedquery = new SpanOrQuery(new SpanMultiTermQueryWrapper<RegexpQuery>(regex));

        MemoryIndex mindex = new MemoryIndex();
        mindex.addField("field", analyzer.tokenStream("field", new StringReader("hello there")));

        // This passes though
        assert mindex.search(wrappedquery) == 0;
    }
}
