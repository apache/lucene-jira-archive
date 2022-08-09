/*
 * WikipediaSimilarity.java
 *
 * Created on January 28, 2005, 12:57 PM
 */

package com;

import org.apache.lucene.search.DefaultSimilarity;

/**
 * Similarity for use in Wikipedia relevance benchmark test.
 * Assumes two fields called "title" and "body"
 * @author Williams
 */
public class WikipediaSimilarity extends DefaultSimilarity {
    
    // lengthNorm uses logs to the base 10
    private static final double LOG10 = Math.log(10.0);
    
    /** Base of logarithm used to flatten tf's */
    public double tfLogBase = Math.log(10.0);
    
    /** Base of logarithm used to flatten idf's */
    public double idfLogBase = Math.log(10.0);
    
    /** Creates a new instance of WikipediaSimilarity */
    public WikipediaSimilarity() {}
    
    /** Ignore lengths of titles, minimize differences in short bodies, and generally
     * flatten significance of this factor a little.
     * @param fieldName the name of a search field
     * @param numTerms the number of words in that field
     * @return 3/log10(1000+numTerms) for "body" and 1.0f for "title"
     */
    public float lengthNorm(String fieldName, int numTerms) {
        if (fieldName.equals("body"))
            return (float)(3.0/(Math.log(1000+numTerms)/LOG10));
        return 1.0f;
    }
  
    /** Scale down the significance of tf weights considerably, especially to keep them from unduly influencing title scores
     * ***** TO DO:  It would be nice if there was a way to tune this for description different than for title
     * @param freq the number of occurrences of a term in a field
     * @return 1+log(freq) where the log is base tfLogBase
     */
    public float tf(float freq) {
        return (float)(1.0 + Math.log(freq)/tfLogBase);
    }
    
    /** Similar to tf scaled down in sifnificance greatly.
     * Note that idf is multiple into Term scores twice! (queryNorm and fieldNorm)
     * This is compensated for by taking the square root of the desired idf.
     * @param docFreq the number of documents in which a Term occurs
     * @param numDocs the total number of documents
     * @return sqrt(1+log(numDocs / (docFreq + 1))) where the log is base idfLogBase
     */
    public float idf(int docFreq, int numDocs) {
      return (float)Math.sqrt(1.0 + Math.log(numDocs/(double)(docFreq+1))/idfLogBase);
    }
    
}
