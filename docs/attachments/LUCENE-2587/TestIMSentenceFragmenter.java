package no.intermedium.LuceneSearcher.fragmenter;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.util.Version;
/**
 * 
 * Test for Sentence Fragmenter
 * 
 * 
 * @author Terje Eggestad, InterMedium AS <terje.eggestad@intermedium.no>
 *
 */
public class TestIMSentenceFragmenter extends TestCase {
	Fragmenter fragmenter;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fragmenter = new IMSentenceFragmenter(30);	
		
	}
	public void testFragmenter() throws IOException {
		
		
		System.out.println("\n\nTEST2\n");
		String originalText =
			/*
			         1         2         3         4         5
			123456789012345678901234567890123456789012345678901234567890
			*/
			" hei hei alle sammen. " + // 23
			"XXX Og sånn skal det vaære. "+ // 29
			"XXX dsfhgk dddd kdh kj gh,  XXX 777 8888 fff jjj."  + // 50
			"\n\n" +
			"XXX hei hei alle sammen. " + // 26
			"XXX Og sånn skal det vaære. "+ // 29
			"XXX eoirut teowry ewoiy,  XXX 957 9058 6777 9875 59687. " + // 50
			"XXX this is the end of all." // 28
			+"\n\nXXX 7878 gh ldsøf.";
	
		runTestOnText(fragmenter, originalText);
	}

	public void testFragmenter2() throws IOException {
		
		System.out.println("\n\nTEST2\n");
		String originalText =
			/*
			         1         2         3         4         5
			123456789012345678901234567890123456789012345678901234567890
			*/
			" hei hei alle sammen. " + // 23
			"XXX Og sånn skal det vaære. "+ // 29
			"XXX dsfhgk dddd kdh kj gh,  XXX 777 8888 fff jjj."  + // 50
			"\n\n" +
			"XXX hei hei alle sammen. " + // 26
			"XXX Og sånn skal det vaære. "+ // 29
			"XXX eoirut teowry ewoiy,  XXX 957 9058 6777 9875 59687. " + // 50
			"XXX this is the end of all." +// 28
			//"\n\nXXX 7878." +
	//		"\n\n " +
			""
			;
	
		runTestOnText(fragmenter, originalText);		
	}
	
	public void testFragmenter3() throws IOException {
		
		System.out.println("\n\nTEST3\n");
		String originalText =
			/*
			         1         2         3         4         5
			123456789012345678901234567890123456789012345678901234567890
			*/
			" hei hei alle sammen. " + // 23			
			"\n\n" +
			"\n\nXXX 7878." +
			""
			;
	
		runTestOnText(fragmenter, originalText);		
	}
	
	public void testFragmenter4() throws IOException {
		Fragmenter fragmenter = new IMSentenceFragmenter(200);
		
		System.out.println("\n\nTEST4\n");
		String originalText =
			/*
			         1         2         3         4         5
			123456789012345678901234567890123456789012345678901234567890
			*/
			"Ny skolering om distriktene \n"+
			"\n"+
			" Dag Jørund Lønning \n"+
			"\n"+
			" Alder: 43. \n"+
			"\n"+
			" Bosted: Hå Aktuell: Kunnskapsdepartementet har nylig at Lønnings egen høgskole, Høgskolen for landbruk og bygdenæringer (HLB) skal kunne tildele en bachelorgrad innenfor fagfeltet bygdeutvikling. \n"+
			"\n"+
			" - Hva blir det viktigste for HLB med tanke på å kunne tilby en bachelorutdanning innen bygdeutvikling? \n"+
			"\n"+
			" - HLB har som hovedmålsetting å produsere kunnskap for livskraftige bygdesamfunn, og vi tar nå et langt steg videre i forhold til dette målet. \n"+
			"\n"+
			" - Hvem er denne utdannelsen spesielt rettet mot? \n"+
			"\n"+
			" - Vi retter oss inn mot alle som vil være med og utvikle Distrikts-Norge videre. \n"+
			"\n"+
			" - Trenger man virkelig en bachelorgrad for å utvikle bygdene. Holder det ikke lenger med sunt bondevett? \n"+
			"\n"+
			" - Kunnskap og kompetanse er sentrale elementer i all nyskaping. Samtidig legger vi ved HLB sterk vekt på at kunnskapen også skal være handlingsorientert. \n"+
			"\n"+
			" - Hva er best med sommeren? \n"+
			"\n"+
			" - Jeg har nettopp flyttet til Jæren og stortrives med nykokte reker fra Sirevåg og landets lengste og fineste strender. Sommer handler vel mest om å nyte - samtidig som refleksjonen over vår privilegerte status her i Norge alltid bør være med. \n"+
			"\n"+
			" - Vi er inne i agurktiden. Hva er din beste oppskrift som inneholder agurk? \n"+
			"\n"+
			" - Helt klart Tzatziki. Finhakk agurk og hvitløk, tilsett yoghurt naturell og bruk et klede til å presse ut en del av væsken. Glimrende til grillmat! \n"+
			"\n"+
			" - Hvor drar du på ferie i sommer? \n"+
			"\n"+
			" - Mesteparten av sommeren har gått med til innflytting i nytt hus, men jeg har også vært på en lengre tur i England sammen med familien. \n"+
			"\n"+
			" - Hvorfor drar du akkurat dit? \n"+
			"\n"+
			" - England er spennende, og lett tilgjengelig fra Rogaland. \n"+
			"\n"+
			" - Hva er ditt beste sommerminne? \n"+
			"\n"+
			" - Jeg har mange gode minner fra somre på hjemmegården på Stord, ikke minst fra tiden da våre egne barn var små og mine foreldre Selma og Per ennå levde. \n"+
			"\n"+
			" - Hva leser du i sommer? \n"+
			"\n"+
			" - Nå leser jeg en av Englands aller fremste nålevende romanforfattere Iain Pears. Det er stor litteratur med sterke spenningsinnslag. \n"+
			"\n"+
			" - Hvem ville du ha spist en bedre sommermiddag sammen med om du kunne velge fritt? \n"+
			"\n"+
			" - Da ville jeg valgt den amerikanske filosofen Robert Pirsig. Han skrev i sin tid verdens beste bok - «Zen og kunsten å reparere en motorsykkel». Ingen har kommet nærmere en forståelse av hva «kvalitet» egentlig er for noe, og boka burde vært obligatorisk lesning for alle som jobber med nye «kvalitetsprodukter» og -satsinger både i bygd og by.\n"+
			"\n"+
			"WERNER WILH DALLAWARA\n"
			;
	
		runTestOnPlainText(fragmenter, originalText);		
	}
	public void testFragmenter5() throws IOException {
		Fragmenter fragmenter = new IMSentenceFragmenter(200);
		
		System.out.println("\n\nTEST4\n");
		String originalText ="De rødgrønne trodde kanskje at det var over. I april forsvant det blåblå flertallet fra meningsmålingene etter at det hadde vært der i et helt år. Gjennomsnittet for målingene i juni viser at de blåblå er på vei opp igjen. \n"+
		"\n"+
		"Et valg i juni ville gitt Høyre og Fremskrittspartiet tilsammen 84 mandater på Stortinget. \n"+
		"\n"+
		"&#x2013; Plutselig ligger de ett mandat fra flertall igjen, sier valgforsker Bernt Aardal, som har laget gjennomsnittet av de ni stortingsvalgmålingene i juni og satt sammen mandatberegningen for Dagens Næringsliv. \n"+
		"\n"+
		"Det er særlig Høyre som går frem. Partiet får 29 prosent i oppslutning. Det er rekordhøyt. Aardal har gjennomsnittstall for målingene helt fra 1997. I løpet av de 14 årene har Høyre bare en gang vært over 29 prosent i oppslutning, og det var i juni 2001. \n"+
		"\n"+
		"&#x2013; For Høyre er dette meget hyggelige tall å ta med seg inn i sommeren, sier Aardal. \n"+
		"\n"+
		"Nedturen for Fremskrittspartiet ser ut til å være stanset, og kanskje har den snudd. Det er store sprik i resultatene for partiet. Forskjellen på den laveste og høyeste målingen for Frp er på nesten åtte prosentpoeng. Målingene som kom mot slutten av måneden, er høyest. \n"+
		"\n"+
		"&#x2013; Det er litt usikkerhet knyttet til om de senere målingene gir uttrykk for en ny og litt annen tendens for Frp, sier Aardal. \n"+
		"\n"+
		"Den eventuelle bedringen mot slutten av måneden kommer etter at partileder Siv Jensens ryggbrudd utløste uro rundt Carl I. Hagens rolle i partiets valgkamp. \n"+
		"\n"+
		"Tilsammen har de to blå partiene 48,5 prosent av velgerne. De rødgrønne partiene &#x2013; Arbeiderpartiet, SV og Senterpartiet &#x2013; får støtte fra 39,2 prosent. Junitallene gir alle de rødgrønne partiene grunn til bekymring. \n"+
		"\n"+
		"n Ap faller tilbake igjen etter at partiet i mai var nær ved å krabbe over 30 prosent. Ap har nå opplevd et helt år med oppslutning lavere enn 30 prosent. I juni blir Ap til og med hårfint passert av Høyre. \n"+
		"\n"+
		"n SV får 5,4 prosent. Juni er partiets dårligste siden oktober ifjor. Det er ikke snakk om noe stort fall fra april. Mest av alt er det enda en svak måned i en lang rekke med elendighet. Siden mars, da bombingen av Libya begynte, har SV falt nesten ett prosentpoeng. En fattig trøst for partiet er at ikke en eneste krigstrett SV-er ser ut til å ha vandret til Rødt. \n"+
		"\n"+
		"n Sp går bare svakt tilbake i en måned med murring etter landbruksoppgjøret. Men også for Sp var juni bare nok en måned i en sørgelig rekke. \n"+
		"\n"+
		"Venstre faller ned mot sperregrensen igjen og får 4,3 prosent. Kristelig Folkeparti opplever derimot en svak trend oppover. 5,3 prosent i oppslutning er det beste resultatet for KrF siden desember 2009. \n"+
		"\n"+
		"Et valgresultat i 2013 på linje med gjennomsnittstallene for junimålingene vil bety at Høyre-leder Erna Solberg tar over som statsminister. Hun vil invitere både Frp, Venstre og KrF til regjeringssamtaler. Venstre og KrF vil trolig høflig takke nei til å sitte i regjering med Frp. Frp vil trolig benytte sjansen til for første gang å få regjeringsmakt. \n"+
		"\n"+
		"Det vil bli den blåeste regjering noengang. \n"+
		"\n"+
		"SV-leder Kristin Halvorsen sa i forrige uke at hun ikke tror det egentlig er noen høyrebølge blant velgerne for hun «hører ikke noe skrik etter skattelette og privatisering». \n"+
		"\n"+
		"Halvorsens problem kan være at hun som kunnskapsminister i stor grad treffer lærere. De skriker sjelden om noe som helst, særlig ikke om privatisering. Dessuten er det ikke privatisering og skattelette Høyre og Frp snakker høyest om, selv om de fortsatt vil ha det. I stedet snakker de for eksempel om kreftbehandling i tide. \n"+
		"\n"+
		"Halvorsen ser kanskje ikke at det er blått overalt. Men målingene viser at den kryper opp leggen hennes.Kjetil B. Alstadheim er kommentator i Dagens Næringsliv.kjetil.alstadheim@dn.no Hovefetivalen, 30.06.11. Felicia Battrawden (20) og Marlene Schønning (22) bruker ca 5000 hver pluss festivalpass (2500) på fire festivaldager. Felicia fikk lønning i dag og er redd den ryker med. Alt er veldig dyrt her sier de. Lade mobilen koster 30 kr timen. Håndklær 200 stk. etcFoto Tomm W. Christiansen\n"+
		"\n"+
		"\n"+
		"\n";
		
		originalText =originalText.replace("&#x2013;", "-");
		//originalText = StringEscapeUtils.unescapeXml(originalText);
		
		runTestOnPlainText(fragmenter, originalText);		
	}
	private void runTestOnPlainText(Fragmenter fragmenter, String originalText)
	throws IOException {
		StandardTokenizer stream = new StandardTokenizer(Version.LUCENE_30, new StringReader(originalText));
		fragmenter.start(originalText, stream);
		int startoffragment = 0;
		
		while(stream.incrementToken()) {
			//System.out.println(stream);
			TermAttribute termAttr = stream.getAttribute(TermAttribute.class);

			OffsetAttribute offsetAttr = stream.getAttribute(OffsetAttribute.class);


			String term = termAttr.term();
			boolean isNewFragment = fragmenter.isNewFragment();
			
			if (isNewFragment) {
				int startOff = offsetAttr.startOffset();
				int endOff = offsetAttr.endOffset();
				
				String fragment = originalText.substring(startoffragment, startOff-1); 
				fragment = fragment.replaceAll("\\-", "");
				fragment = fragment.trim();
				
				System.out.println(String.format("[%d:%d](%d) %s", startoffragment, startOff, (startOff - startoffragment) , fragment));
				
				
				startoffragment = startOff;
				assertFalse("checking for paragraphbreak in fragment", fragment.indexOf("\n\n") > 0);
				IMSentenceFragmenter imfragmenter = (IMSentenceFragmenter) fragmenter;
				
				assertTrue("checking to see if a fragment is too long", fragment.length() < imfragmenter.getFragmentSize());
			}
		}
	}



	
	private void runTestOnText(Fragmenter fragmenter, String originalText)
			throws IOException {
		StandardTokenizer stream = new StandardTokenizer(Version.LUCENE_30, new StringReader(originalText));
		fragmenter.start(originalText, stream);
		
		while(stream.incrementToken()) {
			//System.out.println(stream);
			TermAttribute termAttr = stream.getAttribute(TermAttribute.class);
						
			OffsetAttribute offsetAttr = stream.getAttribute(OffsetAttribute.class);
			
			
			String term = termAttr.term();
			boolean isNewFragment = fragmenter.isNewFragment();
			
			String broken = "";
			
			boolean termIsXXX = term.equals("XXX");
			// System.out.println(isNewFragment + " " + termIsXXX + " " +term + " "  + offsetAttr.startOffset() + ":" + offsetAttr.endOffset());
			
			if (termIsXXX ) {
				if (isNewFragment) {
				// we OK
				} else {
					assertTrue("XXX on non new fragment", false);
				}
			} else {
				if (isNewFragment) {
					assertTrue("non XXX on new fragment", false);
				} else {
					// we OK					
				}
			}
		}
	}
	
}
