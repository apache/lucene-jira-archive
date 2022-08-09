// Copyright 2018 Amazon.com, Inc. or its affiliates. All rights reserved.

package org.apache.lucene.analysis;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Analyzer that can apply different analysis to sub-fields within a field, as indicated by a Reader implementing
 * SubFieldMarker. Maintains a map from field+subfield to cached TokenStreamComponents. Lazily creates components for
 * each sub-field by delegating to a wrapped Analyzer.  Returns the appropriate components from tokenStream() by relying
 * on setReader() to update state in the MultiTokenStreamComponents that wraps and caches the TokenStreamComponents for
 * each sub-field.
 *
 * Note: this must live in the o.a.l.analysis package so it can access {@link TokenStreamComponents#setReader(Reader)}.
 * TODO: we need to either contribute to Lucene, use reflection, or figure out something else. It's not a great
 * long-term solution to have code living in external packages.
 */
public abstract class SubFieldAnalyzer extends Analyzer {

    /**
     * Create a new instance using the default (global) reuse strategy.
     */
    public SubFieldAnalyzer() {
        super();
    }

    /**
     * Create a new instance using the given reuse strategy.
     * @param reuseStrategy specifies scope of instance sharing/reuse
     */
    public SubFieldAnalyzer(ReuseStrategy reuseStrategy) {
        super(reuseStrategy);
    }

    protected abstract TokenStreamComponents createComponents(String fieldName, String subFieldName);

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new MultiTokenStreamComponents(fieldName, this);
    }

    /**
     * TokenStreamComponents that wraps and caches others per field/sub-field, selecting sub-field based on the Reader
     * that is passed to {@link #setReader(Reader)}.
     */
    private static class MultiTokenStreamComponents extends TokenStreamComponents {

        private final String fieldName;
        private final SubFieldAnalyzer analyzer;
        private final Map<String, TokenStreamComponents> wrapped;

        private TokenStreamComponents curComponents;

        // create multiple internal TSC; hold state of which one is active, switch based on Reader
        MultiTokenStreamComponents(String fieldName, SubFieldAnalyzer analyzer) {
            super(null, null);
            this.analyzer = analyzer;
            this.fieldName = fieldName;
            wrapped = new HashMap<>();
        }

        /**
         * Resets the encapsulated components with the given reader. If the components
         * cannot be reset, an Exception should be thrown.
         *
         * @param reader
         *        a reader to reset the source component
         */
        @Override
        protected void setReader(final Reader reader) {
            String key;
            String subFieldName;
            if (reader instanceof SubFieldMarker) {
                subFieldName = ((SubFieldMarker) reader).getSubFieldName();
                key = createKey(fieldName, subFieldName);
                // TODO multiple analysis chains per field must share the same AttributeSource. Wait really? Hold off
                // We can make this happen by creating an AttributeFactory that just wraps an AttributeSource
                // The only global state we have here is the SourceMap -- when do we reset its pos?
            } else {
                subFieldName = null;
                key = fieldName;
            }
            curComponents = wrapped.computeIfAbsent(key, k -> analyzer.createComponents(fieldName, subFieldName));
            curComponents.setReader(reader);
        }

        /**
         * @return the {@link TokenStream} (sink) for the current sub-field
         */
        public TokenStream getTokenStream() {
            return curComponents.getTokenStream();
        }

        /**
         * @return the {@link Tokenizer} (source) for the current sub-field
         */
        public Tokenizer getTokenizer() {
            return curComponents.getTokenizer();
        }

    }

    static String createKey(String fieldName, String subFieldName) {
        if (subFieldName == null) {
            return fieldName + '.';
        } else {
            return fieldName + '.' + subFieldName;
        }
    }

    /**
     * Interface that Readers may implement in order to provide a subfield name that will be used by SubFieldAnalyzer to
     * choose a subfield-specific Analyzer.
     */
    public interface SubFieldMarker {

        /**
         * @return the name of a sub-field
         */
        String getSubFieldName();

    }

}
