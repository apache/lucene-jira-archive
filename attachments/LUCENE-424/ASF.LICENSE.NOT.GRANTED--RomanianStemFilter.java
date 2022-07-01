package ro.dazoot.indexserver.analysis;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;

/**
 * A filter that stems Romanian words. It supports a table of words that should
 * not be stemmed at all. The stemmer used can be changed at runtime after the
 * filter object is created (as long as it is a RomanianStemmer).
 *
 * @author Catalin Constantin
 * @author Tiba Daniela
 * @version   $Id: RomanianStemFilter.java,v 1.2 2005/08/17 09:38:23 catalin Exp $
 */
public final class RomanianStemFilter extends TokenFilter
{
    /**
     * The actual token in the input stream.
     */
    private Token token = null;
    private RomanianStemmer stemmer = null;
    private Set exclusionSet = null;

    public RomanianStemFilter( TokenStream in )
    {
      super(in);
      stemmer = new RomanianStemmer();
    }

    /**
     * Builds a GermanStemFilter that uses an exclusiontable.
     * @deprecated Use {@link #GermanStemFilter(org.apache.lucene.analysis.TokenStream, java.util.Set)} instead.
     */
    public RomanianStemFilter( TokenStream in, Hashtable exclusiontable )
    {
      this( in );
      exclusionSet = new HashSet(exclusiontable.keySet());
    }

    /**
     * Builds a GermanStemFilter that uses an exclusiontable.
     */
    public RomanianStemFilter( TokenStream in, Set exclusionSet )
    {
      this( in );
      this.exclusionSet = exclusionSet;
    }

    /**
     * @return  Returns the next token in the stream, or null at EOS
     */
    public final Token next()
      throws IOException
    {
      if ( ( token = input.next() ) == null ) {
        return null;
      }
      // Check the exclusiontable
      else if ( exclusionSet != null && exclusionSet.contains( token.termText() ) ) {
        return token;
      }
      else {
        String s = stemmer.stem( token.termText() );
        // If not stemmed, dont waste the time creating a new token
        if ( !s.equals( token.termText() ) ) {
          return new Token( s, token.startOffset(),
            token.endOffset(), token.type() );
        }
        return token;
      }
    }

    /**
     * Set a alternative/custom RomanianStemmer for this filter.
     */
    public void setStemmer( RomanianStemmer stemmer )
    {
      if ( stemmer != null ) {
        this.stemmer = stemmer;
      }
    }

    /**
     * Set an alternative exclusion list for this filter.
     * @deprecated Use {@link #setExclusionSet(java.util.Set)} instead.
     */
    public void setExclusionTable( Hashtable exclusiontable )
    {
      exclusionSet = new HashSet(exclusiontable.keySet());
    }

    /**
     * Set an alternative exclusion list for this filter.
     */
    public void setExclusionSet( Set exclusionSet )
    {
      this.exclusionSet = exclusionSet;
    }
}
