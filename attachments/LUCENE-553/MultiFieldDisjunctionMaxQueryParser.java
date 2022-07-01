/*
 * MultiFieldDisjunctionMaxQueryParser.java
 *
 * Created on April 20, 2006, 8:57 PM
 */

package org.apache.lucene.queryParser;

import java.util.Vector;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

/**
 * Version of MultiFieldQueryParser to work with DisjunctionMaxQuery.
 * As with QueryParser, this class is <em>not</em> thread safe.
 * @author chuck
 */
public class MultiFieldDisjunctionMaxQueryParser extends QueryParser {
    
    private float expandedFieldTieBreaker;
    private String[] fields;
    private float[] boosts;
    
    
    /** Create a new MultiFieldDisjunctionMaxQueryParser.  As with QueryParser, this class is <em>not</em> thread safe.
     * @param fields the fields to search for query clauses that do not have a field specified
     * @param boosts the respective boosts for fields
     * @param expandedFieldTieBreaker the tie breaker to use in the generated DisjunctionMaxQuery's
     * @param analyzer the analyzer to use for tokenization
     */
    public MultiFieldDisjunctionMaxQueryParser(String[] fields, float[] boosts, float expandedFieldTieBreaker, Analyzer analyzer) {
        super(null, analyzer);
        this.fields = fields;
        this.boosts = boosts;
        this.expandedFieldTieBreaker = expandedFieldTieBreaker;
    }
    
    protected Query getFieldQuery(String field, String queryText, int slop) throws ParseException {
        if (field == null) {
            DisjunctionMaxQuery query = null;
            for (int i = 0; i < fields.length; i++) {
                Query q = super.getFieldQuery(fields[i], queryText);
                if (q != null) {
                    q.setBoost(boosts[i]);
                    if (q instanceof PhraseQuery) {
                        ((PhraseQuery) q).setSlop(slop);
                    }
                    if (q instanceof MultiPhraseQuery) {
                        ((MultiPhraseQuery) q).setSlop(slop);
                    }
                    if (query==null)
                        query = new DisjunctionMaxQuery(expandedFieldTieBreaker);
                    query.add(q);
                }
            }
            return query;            // Can be null for stop words
        }
        return super.getFieldQuery(field, queryText);
    }
    
    protected Query getFieldQuery(String field, String queryText) throws ParseException {
        return getFieldQuery(field, queryText, 0);
    }
    
    protected Query getFuzzyQuery(String field, String termStr, float minSimilarity) throws ParseException {
        if (field == null) {
            DisjunctionMaxQuery query = new DisjunctionMaxQuery(expandedFieldTieBreaker);
            for (int i = 0; i < fields.length; i++) {
                Query q = super.getFuzzyQuery(fields[i], termStr, minSimilarity);
                q.setBoost(boosts[i]);
                query.add(q);
            }
            return query;
        }
        return super.getFuzzyQuery(field, termStr, minSimilarity);
    }
    
    protected Query getPrefixQuery(String field, String termStr) throws ParseException {
        if (field == null) {
            DisjunctionMaxQuery query = new DisjunctionMaxQuery(expandedFieldTieBreaker);
            for (int i = 0; i < fields.length; i++) {
                Query q = super.getPrefixQuery(fields[i], termStr);
                q.setBoost(boosts[i]);
                query.add(q);
            }
            return query;
        }
        return super.getPrefixQuery(field, termStr);
    }
    
    protected Query getWildcardQuery(String field, String termStr) throws ParseException {
        if (field == null) {
            DisjunctionMaxQuery query = new DisjunctionMaxQuery(expandedFieldTieBreaker);
            for (int i = 0; i < fields.length; i++) {
                Query q = super.getWildcardQuery(fields[i], termStr);
                q.setBoost(boosts[i]);
                query.add(q);
            }
            return query;
        }
        return super.getWildcardQuery(field, termStr);
    }
    
    
    protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
        if (field == null) {
            DisjunctionMaxQuery query = new DisjunctionMaxQuery(expandedFieldTieBreaker);
            for (int i = 0; i < fields.length; i++) {
                Query q = super.getRangeQuery(fields[i], part1, part2, inclusive);
                q.setBoost(boosts[i]);
                query.add(q);
            }
            return query;
        }
        return super.getRangeQuery(field, part1, part2, inclusive);
    }
    
}
