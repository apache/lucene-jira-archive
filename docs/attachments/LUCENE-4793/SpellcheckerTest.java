/**
 * 
 */
package es.colbenson.opensearch.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.Version;

/**
 * @author Samuel García Martínez <samuelgmartinez@gmail.com>
 * 
 */
public class SpellcheckerTest extends LuceneTestCase {

	private static final String[] SIX_LEN_WORDS = { "george", "anthem", "fluent", "argued" };

	public void testCurrentSpellChecker() throws IOException {
		SpellChecker checker = new SpellChecker(new RAMDirectory());

		checker.clearIndex();
		checker.indexDictionary(new InMemoryDictionary(Arrays.asList(SIX_LEN_WORDS)), new IndexWriterConfig(
				Version.LUCENE_36, null), false);

		// just checking the words are indexed correctly
		for (String word : SIX_LEN_WORDS) {
			assertTrue(checker.exist(word));
		}

		// as specified in the issue the problem shows
		// up when you swap 3rd and 4th letter
		// using 6 letter words

		// should return "george"
		assertTrue(checker.suggestSimilar("geroge", 10).length > 0);

		// should return "fluent"
		assertTrue(checker.suggestSimilar("fleunt", 10).length > 0);

		// should return "argued"
		assertTrue(checker.suggestSimilar("aruged", 10).length > 0);

		IOUtils.closeQuietly(checker);
	}


	public static class InMemoryDictionary implements Dictionary {
		final private Collection<String> words;

		public InMemoryDictionary(Collection<String> words) {
			this.words = words;
		}

		public BytesRefIterator getWordsIterator() throws IOException {
			return new BytesRefIterator() {

				private Iterator<String> it = words.iterator();
				private BytesRef spare = new BytesRef();

				public BytesRef next() throws IOException {
					if (it.hasNext()) {
						spare.copyChars(it.next());
						return spare;
					}

					return null;
				}

				public Comparator<BytesRef> getComparator() {
					return null;
				}
			};
		}

	}
}