package no.intermedium.LuceneSearcher.fragmenter;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.search.highlight.Fragmenter;

/**
 * 
 * This implements a fragmenter for Lucene that split into fragments as close to sentences as possible. 
 * We want hitlines in OverView to be 1 or more whole sentences, as well as within a paragraph. 
 * 
 * 
 * @author Terje Eggestad, InterMedium AS <terje.eggestad@intermedium.no>
 *
 */
public class IMSentenceFragmenter implements Fragmenter {

	private static final int DEFAULT_FRAGMENT_SIZE = 100;
	Logger log ;

	private int currentFragment;
	private int fragmentSize;
	private String text;
	private OffsetAttribute offsetAtt;

	
	public IMSentenceFragmenter() {
		this(DEFAULT_FRAGMENT_SIZE);
	}

	public IMSentenceFragmenter(int fragmentSize) {
		this.fragmentSize = fragmentSize;
		log = Logger.getLogger(this.getClass());

	}

	public void start(String originalText, TokenStream stream) {
		currentFragment = 0;
		lastfragendoffset = 0;
		startOfFragment = new ArrayList<Integer>();

		offsetAtt = stream.addAttribute(OffsetAttribute.class);
	    
		setText(originalText);
		currentFragment = 0; 
		findFragments();
	}

	private ArrayList<Integer> startOfFragment = new ArrayList<Integer>();
	private final String[] endOfSentenceChars = new String[] { ".", "!", "?"};

	
	/**
	 * given a textstring return a list of offsets to all sentence boundaries
	 * @param text
	 * @return list of offsets to sentence boundaries
	 */
	private ArrayList<Integer> findEndOfSentences(String text) {
		String t = text;
		
		
		ArrayList<Integer> endOfSentencePositions = new ArrayList<Integer>();
		
		if (text.trim().length() == 0) return endOfSentencePositions;
		
		int idx;
		int absidx = 0;
		while((idx = StringUtils.indexOfAny(t, endOfSentenceChars)) > 0) {
			// iff last sentence
			if (t.length() == idx+1) {
				break;
			}
			if ( Character.isWhitespace(t.charAt(idx +1))) {
				//System.out.println("EOS: " + (absidx + idx+2));
				endOfSentencePositions.add(absidx+idx+2);				
			}
			absidx += idx +2;
			t = t.substring(idx+2);
		}
		int eot = text.length()-1;
		// add the end of text as well iff not already there
		if (endOfSentencePositions.size() == 0) {
			endOfSentencePositions.add(eot);	
		} else {
			int lasteos = endOfSentencePositions.get(endOfSentencePositions.size()-1);
			if (Math.abs(eot - lasteos) > 5) {
				endOfSentencePositions.add(text.length()-1);
			}
		}
		//System.out.println("EOS's: " + Arrays.deepToString(endOfSentencePositions.toArray()));
		return endOfSentencePositions;
	}
	
	
	/** 
	 *  splits the this.text into fragments and store the offsets where we shall return true in isnewfragement()
	 *  
	 *  We split on paragraphs, then call fragementParagraph to split the paragraph into fragments on sentences. 
	 */
	private void findFragments() {
			
		
		int sop = 0;
		// First we find the next end of "this" paragraph and call fragmentParagraph()
		int eop = -1;
		do {
			eop = text.indexOf("\n\n", sop); 
			//System.out.println("sop = " + sop + " eop " + eop);
			
//			if (eop == -1)
//				System.out.println(text.substring(sop));
//			else
//				System.out.println(text.substring(sop, eop));
			
			fragmentParagraph(sop, eop);
			if (eop > 0)
				sop = eop+2;
		} while (eop > 0);
		
		
		//System.out.println("SOF's: " + Arrays.deepToString(startOfFragment.toArray()));
		
		
		//
		//  fragmentParagraph split only on sentenecs and may leavefragments longer than fragmentSize
		// Therefore we now loop thru the fragments and split anyone longer into roughly equal length pieces. 
		// We try first to split on punctuation, then on white space.
		//
		ArrayList<Integer> newlist = new ArrayList<Integer>();
		//System.out.println("fragement size "  + fragmentSize);

		startOfFragment.add(text.length());
		Integer lastoffset = 0;
		for (Integer offset : startOfFragment) {
			if (offset == 0) continue;
			
			int len = offset - lastoffset;
			//System.out.println("fragement "  + len + " @ " + offset);
			
			
			if (len > fragmentSize) {
				// if  a fragment is longer than fragmentSize. This happens if a sentence is longer than fragementSize.
				// in this case we try to split the fragment in evenly sized pieces. 
				// we try to find a periode or a comma to split on first, if that fails we split on white space
				
				//System.out.println("fragement too long "  + len + " @ " + offset);
				int pieces = len / fragmentSize;
				int piecelen = len / (pieces+1);
				
				for (int i = 0; i < pieces; i++) {
					
					int breakat =  lastoffset + (piecelen * (i+1) - (piecelen  / 4));
					int maxbreakat = breakat +  piecelen * 1 / 3; 
						
					//System.out.println("looking for break at " + breakat);
					int nextperiod = text.indexOf(".",  breakat);
					int nextcomma = text.indexOf(",",  breakat);
					
					if (nextperiod > 0 && nextperiod < maxbreakat) {
						breakat = nextperiod;
						//System.out.println("adding break at period " + breakat);
					} else if (nextcomma> 0 && nextcomma< maxbreakat) {
						breakat = nextcomma;
						//System.out.println("adding break at comma " + breakat);
					} else {
						try {
							breakat =  lastoffset + (piecelen * (i+1) - (piecelen  / 10));
							while(!Character.isWhitespace(text.charAt(++breakat)));
							//System.out.println("adding break at white space " + breakat);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					//System.out.println("Adding extra fragment for long sentence at " + breakat);
					newlist.add(breakat);					
				}
			}
			newlist.add(offset);
			lastoffset = offset;
		}
		startOfFragment = newlist;

		// now startOfFragements correctly hold all fragment boundaries
		//log.debug("fragment startoffsets: " + Arrays.deepToString(startOfFragment.toArray()));
	}
	
	/**
	 * Splits a given paragraph on sentances as close to fragmentSize as possible.
	 * 
	 * @param startofpara must the a start of paragraph in this.text
	 * @param endofpara must the end of the paragraph starting at startofpare in this.text
	 */
	void fragmentParagraph(int startofpara, int endofpara) {
		
		
		// check for last para
		if (endofpara < startofpara) {
			if (startofpara >= text.length()) return;
			endofpara = text.length()-1;			
		}
		
		if (endofpara - startofpara > 0) {
			
			//System.out.println("Adding start of fragment on start of para" + startofpara);
			startOfFragment.add(startofpara);
		}

		String para = null;
		try {
			para = text.substring(startofpara, endofpara);
		} catch (Exception e) {
			log.error("while fetching paragraph to process during fragmenting text", e);
			return ;
		}
		ArrayList<Integer> endOfSentencePositions = findEndOfSentences(para);

		int startofThisFragment = 0;
		for (int i = 0; i < endOfSentencePositions.size(); i++) {
			int endofsentence = endOfSentencePositions.get(i);
			boolean lastSentence = (i +1 == endOfSentencePositions.size());
			
			if (endofsentence - startofThisFragment > fragmentSize ) {
				
				if (i > 0) {
					int startOfNextFragment = endOfSentencePositions.get(i-1) ;
					
					//System.out.println("Start of fragment: " + (startofpara +  startOfNextFragment));
					startOfFragment.add(startofpara + startOfNextFragment);
					
					startofThisFragment = endOfSentencePositions.get(i)+2;
				}

			}
		}
		
		//System.out.println("SOF's: " + Arrays.deepToString(startOfFragment.toArray()));
		//System.out.println("done para");
		
	}
	
	

	private int lastfragendoffset = 0;
	
	public boolean isNewFragment() {
		int startOffset = offsetAtt.startOffset(); 
		int endOffset = offsetAtt.endOffset();  
		
		// needed to handle the last fragment
		if (startOfFragment.size() <= currentFragment) return false;
		
		boolean isNewFrag = (startOffset >= startOfFragment.get(currentFragment));
		
		if (isNewFrag) {
		
			//System.out.println("endofset > = (fragmentSize* (currentNumFrags - 1) + (fragmentSize / 2)) -> " + endOffset + " >= ("  + (fragmentSize* (currentNumFrags - 1)) +  " + " + (fragmentSize / 2) +")" );
			//String frag = getText().substring(lastfragendoffset, startOffset);
			//System.out.println("   FRAG(" + frag.length() + "): " + frag);
			//log.debug("FRAG(" + frag.length() + "): " + frag);
			lastfragendoffset = startOffset;
			
			currentFragment++;
			
		}
		//System.out.println("new gragment for "+startOffset + "  " + endOffset + " = " + isNewFrag);
		return isNewFrag;
	}

	public int getFragmentSize() {
		return fragmentSize;
	}

	public void setFragmentSize(int size) {
		fragmentSize = size;
	}

	public String getText() {
		return text;
	}

	public void setText(String newText) {
		text = newText;
	}

}