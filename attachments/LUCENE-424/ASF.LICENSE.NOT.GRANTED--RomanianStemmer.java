package ro.dazoot.indexserver.analysis;

/**
 * RomanianStemmer for Romanian language, for now it replaces the diacritics with
 * their equivalent.
 * 
 * @author Catalin Constantin
 * @author Tiba Daniela
 * @version $Id: RomanianStemmer.java,v 1.2 2005/08/17 09:38:23 catalin Exp $
 */
public class RomanianStemmer
{
	private StringBuffer sb = new StringBuffer();
	
	protected String stem( String term )
	{
		term = term.toLowerCase();
		if ( !isStemmable( term ) ) return term;
		// Reset the StringBuffer.
		sb.delete( 0, sb.length() );
		sb.insert( 0, term );
		
		// Stemming starts here...
		substitute( sb );
		return sb.toString();
	}

    private boolean isStemmable( String term )
    {
    	for ( int c = 0; c < term.length(); c++ ) 
    	{
    		if ( !Character.isLetter( term.charAt( c ) ) ) return false;
    	}
    	return true;
    }
    
    private void substitute(StringBuffer buffer)
	{
    	int lit;
    	
    	for(int c=0;c<buffer.length();c++)
    	{
    		lit=buffer.charAt(c);
    		// LATIN SMALL LETTER S WITH CEDILLA
    		if (lit == '\u015f') buffer.setCharAt(c, 's');
    		// LATIN SMALL LETTER T WITH CEDILLA
    		if (lit == '\u0163') buffer.setCharAt(c, 't');
    		// LATIN SMALL LETTER A WITH BREVE
    		if (lit == '\u0103') buffer.setCharAt(c, 'a');
    		// LATIN SMALL LETTER A WITH CIRCUMFLEX
    		if (lit == '\u00e2') buffer.setCharAt(c, 'a');
    		// LATIN SMALL LETTER I WITH CIRCUMFLEX
    		if (lit == '\u00ee') buffer.setCharAt(c, 'i');
    	}
    }

}
