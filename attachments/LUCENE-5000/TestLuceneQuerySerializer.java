package org.apache.lucene.search;

import junit.framework.TestCase;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.util.BytesRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author kalle
 * @since 2013-05-13 16:22
 */
public class TestLuceneQuerySerializer extends TestCase {

  public void test() throws Exception {

    queryTest(new SpanTermQuery(new Term("foo", "bar")));

    SpanQuery[] spanNearClauses = new SpanQuery[]{new SpanTermQuery(new Term("foo", "bar1")), new SpanTermQuery(new Term("foo", "bar2"))};
    queryTest(new SpanNearQuery(spanNearClauses, 10, false, true));
    queryTest(new SpanNearQuery(spanNearClauses, 5, true, false));

    queryTest(new SpanMultiTermQueryWrapper<MultiTermQuery>(new FuzzyQuery(new Term("foo", "bar"))));

    queryTest(new SpanNotQuery(new SpanTermQuery(new Term("foo", "include")), new SpanTermQuery(new Term("foo", "exclude"))));

    queryTest(new SpanOrQuery(new SpanTermQuery(new Term("foo", "this")), new SpanTermQuery(new Term("foo", "that"))));

    queryTest(new FieldMaskingSpanQuery(new SpanTermQuery(new Term("foo", "this")), "lie"));

    queryTest(new SpanFirstQuery(new SpanTermQuery(new Term("foo", "this")), 4));
    queryTest(new SpanFirstQuery(new SpanTermQuery(new Term("foo", "this")), 5));

    queryTest(new SpanPositionRangeQuery(new SpanTermQuery(new Term("foo", "bar")), 1, 5));
    queryTest(new SpanPositionRangeQuery(new SpanTermQuery(new Term("foo", "bar")), 5, 9));

    queryTest(new SpanPayloadCheckQuery(new SpanTermQuery(new Term("foo", "bar")), Arrays.asList(new byte[][]{
        new byte[]{1},
        new byte[]{2, 3},
        new byte[]{3, 4, 5},
    })));

    queryTest(new SpanNearPayloadCheckQuery(new SpanNearQuery(spanNearClauses, 10, false, true), Arrays.asList(new byte[][]{
        new byte[]{1},
        new byte[]{2, 3},
        new byte[]{3, 4, 5},
    })));

    List<Query> disjunctions = new ArrayList<>();
    disjunctions.add(new TermQuery(new Term("foo", "bar")));
    disjunctions.add(new TermQuery(new Term("bar", "foo")));
    queryTest(new DisjunctionMaxQuery(disjunctions, 2f));
    queryTest(new DisjunctionMaxQuery(disjunctions, 10f));

    queryTest(new MatchAllDocsQuery());

    queryTest(new TermRangeQuery("foo", new BytesRef("begining"), new BytesRef("end"), true, true));
    queryTest(new TermRangeQuery("foo", new BytesRef("start"), new BytesRef("terminate"), false, false));
    queryTest(new TermRangeQuery("foo", new BytesRef("begining"), new BytesRef("end"), false, true));
    queryTest(new TermRangeQuery("foo", new BytesRef("begining".getBytes(), 1, 4), new BytesRef("end".getBytes(), 0, 3), false, true));

    queryTest(new RegexpQuery(new Term("foo", "bar u.+")));
    queryTest(new RegexpQuery(new Term("foo", "bar")));

    queryTest(new FuzzyQuery(new Term("foo", "bar"), 0, 11, 12, true));
    queryTest(new FuzzyQuery(new Term("foo", "bar"), 1, 12, 13, false));
    queryTest(new FuzzyQuery(new Term("foo", "bar"), 2, 14, 15, false));

    MultiPhraseQuery multiPhraseQuery = new MultiPhraseQuery();
    multiPhraseQuery.add(new Term[]{new Term("foo", "bar"), new Term("foo", "bar2")});
    multiPhraseQuery.add(new Term[]{new Term("foo", "bar3"), new Term("foo", "bar4")});
    queryTest(multiPhraseQuery);
    multiPhraseQuery.setSlop(10);
    queryTest(multiPhraseQuery);

    queryTest(new PrefixQuery(new Term("foo", "bar")));


    PhraseQuery phraseQuery = new PhraseQuery();
    phraseQuery.add(new Term("foo", "bar1"));
    phraseQuery.add(new Term("foo", "bar2"));
    phraseQuery.add(new Term("foo", "bar3"));
    phraseQuery.setSlop(10);
    queryTest(phraseQuery);

    phraseQuery.setSlop(2);
    queryTest(phraseQuery);

    queryTest(new TermQuery(new Term("foo", "bar")));

    queryTest(new TermQuery(new Term("foo", "bar")));

    queryTest(new WildcardQuery(new Term("foo", "bar*")));

    queryTest(NumericRangeQuery.newIntRange("foo", 10, 1, 2, true, true));
    queryTest(NumericRangeQuery.newIntRange("foo", 5, 2, 3, false, false));
    queryTest(NumericRangeQuery.newLongRange("foo", 10, 1l, 2l, true, true));
    queryTest(NumericRangeQuery.newLongRange("foo", 5, 2l, 3l, false, false));
    queryTest(NumericRangeQuery.newFloatRange("foo", 10, 1f, 2f, true, true));
    queryTest(NumericRangeQuery.newFloatRange("foo", 5, 2f, 3f, false, false));
    queryTest(NumericRangeQuery.newDoubleRange("foo", 10, 1d, 2d, true, true));
    queryTest(NumericRangeQuery.newDoubleRange("foo", 5, 2d, 3d, false, false));

    BooleanQuery booleanQuery = new BooleanQuery(true);
    booleanQuery.setMinimumNumberShouldMatch(0);
    booleanQuery.add(new TermQuery(new Term("foo", "a")), BooleanClause.Occur.SHOULD);
    booleanQuery.add(new TermQuery(new Term("foo", "b")), BooleanClause.Occur.MUST);
    booleanQuery.add(new TermQuery(new Term("foo", "c")), BooleanClause.Occur.MUST_NOT);
    queryTest(booleanQuery);

    booleanQuery = new BooleanQuery(false);
    booleanQuery.setMinimumNumberShouldMatch(10);
    booleanQuery.add(new TermQuery(new Term("foo", "a")), BooleanClause.Occur.SHOULD);
    booleanQuery.add(new TermQuery(new Term("foo", "b")), BooleanClause.Occur.MUST);
    booleanQuery.add(new TermQuery(new Term("foo", "c")), BooleanClause.Occur.MUST_NOT);
    queryTest(booleanQuery);

  }

  private void queryTest(Query query) throws Exception {
    query.setBoost(1f);
    doQueryTest(query);
    query.setBoost(10f);
    doQueryTest(query);
  }

  public void doQueryTest(Query query) throws Exception {

    LuceneQuerySerializer serializer = new LuceneQuerySerializer();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    serializer.writeQuery(oos, query);
    oos.close();

    ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bis);
    Query deserializedQuery = serializer.readQuery(ois);
    ois.close();

    // perhaps implement a more complex equals in SpanPayloadCheckQuery and SpanNearPayloadCheckQuery that use Arrays.equals?
    if (query instanceof SpanNearPayloadCheckQuery) {
      SpanNearPayloadCheckQuery a = (SpanNearPayloadCheckQuery) query;
      SpanNearPayloadCheckQuery b = (SpanNearPayloadCheckQuery) deserializedQuery;
      assertEquals(a.getBoost(), b.getBoost());
      assertEquals(a.getMatch(), b.getMatch());

      // private field
      try {
        Field payloadToMatchField = SpanNearPayloadCheckQuery.class.getDeclaredField("payloadToMatch");
        payloadToMatchField.setAccessible(true);

        Collection<byte[]> ac = (Collection<byte[]>) payloadToMatchField.get(a);
        Collection<byte[]> bc = (Collection<byte[]>) payloadToMatchField.get(b);

        assertEquals(ac.size(), bc.size());
        Iterator<byte[]> ai = ac.iterator();
        Iterator<byte[]> bi = bc.iterator();
        while (ai.hasNext()) {
          byte[] aa = ai.next();
          byte[] ba = bi.next();
          assertTrue(Arrays.equals(aa, ba));
        }

      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }


    } else if (query instanceof SpanPayloadCheckQuery) {

      SpanPayloadCheckQuery a = (SpanPayloadCheckQuery) query;
      SpanPayloadCheckQuery b = (SpanPayloadCheckQuery) deserializedQuery;
      assertEquals(a.getBoost(), b.getBoost());
      assertEquals(a.getMatch(), b.getMatch());

      // private field
      try {
        Field payloadToMatchField = SpanPayloadCheckQuery.class.getDeclaredField("payloadToMatch");
        payloadToMatchField.setAccessible(true);

        Collection<byte[]> ac = (Collection<byte[]>) payloadToMatchField.get(a);
        Collection<byte[]> bc = (Collection<byte[]>) payloadToMatchField.get(b);

        assertEquals(ac.size(), bc.size());
        Iterator<byte[]> ai = ac.iterator();
        Iterator<byte[]> bi = bc.iterator();
        while (ai.hasNext()) {
          byte[] aa = ai.next();
          byte[] ba = bi.next();
          assertTrue(Arrays.equals(aa, ba));
        }

      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }


    } else {
      assertEquals(query, deserializedQuery);
    }

  }


}
