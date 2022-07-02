package org.apache.lucene.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.*;


/**
 * Supports serialization of
 * {@link TermQuery}
 * {@link BooleanQuery}
 * {@link WildcardQuery}
 * {@link PhraseQuery}
 * {@link MultiPhraseQuery}
 * {@link FuzzyQuery}
 * {@link RegexpQuery}
 * {@link TermRangeQuery}
 * {@link NumericRangeQuery}
 * {@link DisjunctionMaxQuery}
 * {@link MatchAllDocsQuery}
 * <p/>
 * {@link org.apache.lucene.search.spans.SpanTermQuery}
 * {@link org.apache.lucene.search.spans.SpanMultiTermQueryWrapper}
 * {@link org.apache.lucene.search.spans.SpanNearQuery}
 * {@link org.apache.lucene.search.spans.SpanNotQuery}
 * {@link org.apache.lucene.search.spans.SpanOrQuery}
 * {@link org.apache.lucene.search.spans.FieldMaskingSpanQuery}
 * {@link org.apache.lucene.search.spans.SpanFirstQuery}
 * {@link org.apache.lucene.search.spans.SpanPositionRangeQuery}
 * <p/>
 * {@link SpanPayloadCheckQuery}
 * {@link SpanNearPayloadCheckQuery}
 * <p/>
 * <p/>
 * I.e. it does not support
 * <p/>
 * Due to no serialization strategy for Filter:
 * {@link ConstantScoreQuery}
 * <p/>
 * Due to no serialization strategy for PayloadFunction:
 * {@link org.apache.lucene.search.payloads.PayloadNearQuery}
 * {@link org.apache.lucene.search.payloads.PayloadTermQuery}
 * <p/>
 *
 * @author kalle
 * @since 2013-05-13 16:19
 */
public class LuceneQuerySerializer {


  private Map<String, QueryStrategy> strategiesByClassName = new HashMap<>(20);

  public LuceneQuerySerializer() {
    registerStrategy(new SpanTermQueryStrategy());
    registerStrategy(new SpanMultiTermQueryWrapperStrategy());
    registerStrategy(new SpanNearQueryStrategy());
    registerStrategy(new SpanNotQueryStrategy());
    registerStrategy(new SpanOrQueryStrategy());
    registerStrategy(new FieldMaskingSpanQueryStrategy());

    registerStrategy(new SpanFirstQueryStrategy());
    registerStrategy(new SpanPositionRangeQueryStrategy());

    registerStrategy(new SpanPayloadCheckQueryStrategy());
    registerStrategy(new SpanNearPayloadCheckQueryStrategy());


    registerStrategy(new DisjunctionMaxQueryStrategy());
    registerStrategy(new MatchAllDocsQueryStrategy());
    registerStrategy(new TermRangeQueryStrategy());
    registerStrategy(new RegexpQueryStrategy());
    registerStrategy(new FuzzyQueryStrategy());
    registerStrategy(new MultiPhraseQueryStrategy());
    registerStrategy(new PrefixQueryStrategy());
    registerStrategy(new PhraseQueryStrategy());
    registerStrategy(new WildcardQueryStrategy());
    registerStrategy(new TermQueryStrategy());
    registerStrategy(new BooleanQueryStrategy());
    registerStrategy(new NumericRangeQueryStrategy());
  }

  public void registerStrategy(QueryStrategy strategy) {
    strategiesByClassName.put(strategy.getQueryClass().getName(), strategy);
  }

  public Map<String, QueryStrategy> getStrategiesByClassName() {
    return strategiesByClassName;
  }

  public void setStrategiesByClassName(Map<String, QueryStrategy> strategiesByClassName) {
    this.strategiesByClassName = strategiesByClassName;
  }

  public Query readQuery(ObjectInputStream in) throws IOException, ClassNotFoundException {
    String queryClass = in.readUTF();
    QueryStrategy queryStrategy = strategiesByClassName.get(queryClass);
    if (queryStrategy == null) {
      throw new RuntimeException("Unsupported query class: " + queryClass);
    }
    return queryStrategy.deserialize(this, in);
  }

  public void writeQuery(ObjectOutputStream out, Query query) throws IOException {
    QueryStrategy queryStrategy = strategiesByClassName.get(query.getClass().getName());
    if (queryStrategy == null) {
      throw new RuntimeException("Unsupported query class: " + query.getClass().getName());
    }
    queryStrategy.serialize(this, out, query);
  }

  public abstract static class QueryStrategy<Q extends Query> {
    public abstract Class<Q> getQueryClass();

    public abstract void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, Q query) throws IOException;

    public abstract Q deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException;
  }

  public static class DisjunctionMaxQueryStrategy extends QueryStrategy<DisjunctionMaxQuery> {
    @Override
    public Class<DisjunctionMaxQuery> getQueryClass() {
      return DisjunctionMaxQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, DisjunctionMaxQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeInt(query.getDisjuncts().size());
      for (Query disjunction : query.getDisjuncts()) {
        serializer.writeQuery(out, disjunction);
      }
      out.writeFloat(query.getTieBreakerMultiplier());
      out.writeFloat(query.getBoost());
    }

    @Override
    public DisjunctionMaxQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {

      int numberOfDisjunctions = in.readInt();
      List<Query> disjunctions = new ArrayList<>(numberOfDisjunctions);
      for (int i = 0; i < numberOfDisjunctions; i++) {
        disjunctions.add(serializer.readQuery(in));
      }

      DisjunctionMaxQuery query = new DisjunctionMaxQuery(disjunctions, in.readFloat());

      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class TermQueryStrategy extends QueryStrategy<TermQuery> {

    @Override
    public Class<TermQuery> getQueryClass() {
      return TermQuery.class;
    }

    @Override
    public TermQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException {
      TermQuery query = new TermQuery(new Term(in.readUTF(), in.readUTF()));
      query.setBoost(in.readFloat());
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, TermQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getTerm().field());
      out.writeUTF(query.getTerm().text());
      out.writeFloat(query.getBoost());
    }
  }

  public static class MatchAllDocsQueryStrategy extends QueryStrategy<MatchAllDocsQuery> {

    @Override
    public Class<MatchAllDocsQuery> getQueryClass() {
      return MatchAllDocsQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, MatchAllDocsQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeFloat(query.getBoost());
    }

    @Override
    public MatchAllDocsQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      MatchAllDocsQuery query = new MatchAllDocsQuery();
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class TermRangeQueryStrategy extends QueryStrategy<TermRangeQuery> {

    @Override
    public Class<TermRangeQuery> getQueryClass() {
      return TermRangeQuery.class;
    }

    @Override
    public TermRangeQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      TermRangeQuery query = new TermRangeQuery(
          in.readUTF(),
          new BytesRef((byte[]) in.readObject(), in.readInt(), in.readInt()),
          new BytesRef((byte[]) in.readObject(), in.readInt(), in.readInt()),
          in.readBoolean(),
          in.readBoolean());
      query.setBoost(in.readFloat());
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, TermRangeQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getField());

      out.writeObject(query.getLowerTerm().bytes);
      out.writeInt(query.getLowerTerm().offset);
      out.writeInt(query.getLowerTerm().length);

      out.writeObject(query.getUpperTerm().bytes);
      out.writeInt(query.getUpperTerm().offset);
      out.writeInt(query.getUpperTerm().length);

      out.writeBoolean(query.includesLower());
      out.writeBoolean(query.includesUpper());
      out.writeFloat(query.getBoost());
    }
  }

  public static class RegexpQueryStrategy extends QueryStrategy<RegexpQuery> {

    @Override
    public Class<RegexpQuery> getQueryClass() {
      return RegexpQuery.class;
    }

    @Override
    public RegexpQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException {
      String field = in.readUTF();
      String text = in.readUTF();
      float boost = in.readFloat();
      RegexpQuery query = new RegexpQuery(new Term(field, text));
      query.setBoost(boost);
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, RegexpQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.term.field());
      out.writeUTF(query.term.text());
      out.writeFloat(query.getBoost());
    }
  }

  public static class FuzzyQueryStrategy extends QueryStrategy<FuzzyQuery> {

    @Override
    public Class<FuzzyQuery> getQueryClass() {
      return FuzzyQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, FuzzyQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getTerm().field());
      out.writeUTF(query.getTerm().text());
      out.writeInt(query.getMaxEdits());
      out.writeInt(query.getPrefixLength());


      // private fields in FuzzyQuery
      try {
        Field maxExpansionsField = FuzzyQuery.class.getDeclaredField("maxExpansions");
        maxExpansionsField.setAccessible(true);
        int maxExpansions = (Integer) maxExpansionsField.get(query);

        out.writeInt(maxExpansions);

        Field transpositionsField = FuzzyQuery.class.getDeclaredField("transpositions");
        transpositionsField.setAccessible(true);
        boolean transpositions = (Boolean) transpositionsField.get(query);

        out.writeBoolean(transpositions);
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }

      out.writeFloat(query.getBoost());
    }

    @Override
    public FuzzyQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      FuzzyQuery query = new FuzzyQuery(
          new Term(in.readUTF(), in.readUTF()),
          in.readInt(),
          in.readInt(),
          in.readInt(),
          in.readBoolean()
      );
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class WildcardQueryStrategy extends QueryStrategy<WildcardQuery> {

    @Override
    public Class<WildcardQuery> getQueryClass() {
      return WildcardQuery.class;
    }

    @Override
    public WildcardQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException {
      String field = in.readUTF();
      String text = in.readUTF();
      float boost = in.readFloat();

      WildcardQuery query = new WildcardQuery(new Term(field, text));
      query.setBoost(boost);
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, WildcardQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getTerm().field());
      out.writeUTF(query.getTerm().text());
      out.writeFloat(query.getBoost());


    }
  }

  public static class PrefixQueryStrategy extends QueryStrategy<PrefixQuery> {

    @Override
    public Class<PrefixQuery> getQueryClass() {
      return PrefixQuery.class;
    }

    @Override
    public PrefixQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException {
      PrefixQuery query = new PrefixQuery(new Term(in.readUTF(), in.readUTF()));
      query.setBoost(in.readFloat());
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, PrefixQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getPrefix().field());
      out.writeUTF(query.getPrefix().text());
      out.writeFloat(query.getBoost());
    }
  }

  public static class PhraseQueryStrategy extends QueryStrategy<PhraseQuery> {

    @Override
    public Class<PhraseQuery> getQueryClass() {
      return PhraseQuery.class;
    }

    @Override
    public PhraseQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException {

      PhraseQuery query = new PhraseQuery();
      query.setSlop(in.readInt());
      int numberOfTerms = in.readInt();
      for (int i = 0; i < numberOfTerms; i++) {
        query.add(new Term(in.readUTF(), in.readUTF()));
      }
      query.setBoost(in.readFloat());
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, PhraseQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeInt(query.getSlop());
      out.writeInt(query.getTerms().length);
      for (Term term : query.getTerms()) {
        out.writeUTF(term.field());
        out.writeUTF(term.text());
      }
      out.writeFloat(query.getBoost());
    }
  }

  public static class MultiPhraseQueryStrategy extends QueryStrategy<MultiPhraseQuery> {

    @Override
    public Class<MultiPhraseQuery> getQueryClass() {
      return MultiPhraseQuery.class;
    }

    @Override
    public MultiPhraseQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {

      MultiPhraseQuery query = new MultiPhraseQuery();
      query.setSlop(in.readInt());
      int[] positions = (int[]) in.readObject();
      int numberOfTermArrays = in.readInt();
      for (int arrayIndex = 0; arrayIndex < numberOfTermArrays; arrayIndex++) {
        Term[] terms = new Term[in.readInt()];
        for (int termIndex = 0; termIndex < terms.length; termIndex++) {
          terms[termIndex] = new Term(in.readUTF(), in.readUTF());
        }
        query.add(terms, positions[arrayIndex]);
      }
      query.setBoost(in.readFloat());
      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, MultiPhraseQuery query) throws IOException {

      out.writeUTF(getQueryClass().getName());
      out.writeInt(query.getSlop());
      out.writeObject(query.getPositions());
      out.writeInt(query.getTermArrays().size());
      for (Term[] terms : query.getTermArrays()) {
        out.writeInt(terms.length);
        for (Term term : terms) {
          out.writeUTF(term.field());
          out.writeUTF(term.text());
        }
      }
      out.writeFloat(query.getBoost());
    }
  }

  public static class BooleanQueryStrategy extends QueryStrategy<BooleanQuery> {

    @Override
    public Class<BooleanQuery> getQueryClass() {
      return BooleanQuery.class;
    }

    @Override
    public BooleanQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      boolean disableCoords = in.readBoolean();
      BooleanQuery booleanQuery = new BooleanQuery(disableCoords);
      int numberOfClauses = in.readInt();
      for (int i = 0; i < numberOfClauses; i++) {
        String occursString = in.readUTF();
        BooleanClause.Occur occur;
        if (BooleanClause.Occur.MUST.name().equals(occursString)) {
          occur = BooleanClause.Occur.MUST;
        } else if (BooleanClause.Occur.MUST_NOT.name().equals(occursString)) {
          occur = BooleanClause.Occur.MUST_NOT;
        } else if (BooleanClause.Occur.SHOULD.name().equals(occursString)) {
          occur = BooleanClause.Occur.SHOULD;
        } else {
          throw new RuntimeException();
        }
        booleanQuery.add(new BooleanClause(serializer.readQuery(in), occur));
      }
      booleanQuery.setMinimumNumberShouldMatch(in.readInt());
      booleanQuery.setBoost(in.readFloat());
      return booleanQuery;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, BooleanQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeBoolean(query.isCoordDisabled());
      out.writeInt(query.clauses().size());
      for (BooleanClause booleanClause : query.clauses()) {
        out.writeUTF(booleanClause.getOccur().name());
        serializer.writeQuery(out, booleanClause.getQuery());
      }
      out.writeInt(query.getMinimumNumberShouldMatch());
      out.writeFloat(query.getBoost());

    }
  }

  public static class NumericRangeQueryStrategy extends QueryStrategy<NumericRangeQuery> {

    @Override
    public Class<NumericRangeQuery> getQueryClass() {
      return NumericRangeQuery.class;
    }

    @Override
    public NumericRangeQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      String dataType = in.readUTF();
      String field = in.readUTF();
      Number min = (Number) in.readObject();
      Number max = (Number) in.readObject();
      boolean includeMin = in.readBoolean();
      boolean includeMax = in.readBoolean();
      int precisionStep = in.readInt();
      float boost = in.readFloat();

      NumericRangeQuery query;

      if ("INT".equals(dataType)) {
        query = NumericRangeQuery.newIntRange(field, precisionStep, min.intValue(), max.intValue(), includeMin, includeMax);
      } else if ("LONG".equals(dataType)) {
        query = NumericRangeQuery.newLongRange(field, precisionStep, min.longValue(), max.longValue(), includeMin, includeMax);
      } else if ("FLOAT".equals(dataType)) {
        query = NumericRangeQuery.newFloatRange(field, precisionStep, min.floatValue(), max.floatValue(), includeMin, includeMax);
      } else if ("DOUBLE".equals(dataType)) {
        query = NumericRangeQuery.newDoubleRange(field, precisionStep, min.doubleValue(), max.doubleValue(), includeMin, includeMax);
      } else {
        throw new RuntimeException("Unsupported dataType " + dataType);
      }

      query.setBoost(boost);

      return query;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, NumericRangeQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      switch (query.dataType) {
        case INT:
          out.writeUTF("INT");
          break;
        case LONG:
          out.writeUTF("LONG");
          break;
        case FLOAT:
          out.writeUTF("FLOAT");
          break;
        case DOUBLE:
          out.writeUTF("DOUBLE");
          break;
        default:
          throw new RuntimeException("Unsupported dataType " + query.dataType);
      }


      out.writeUTF(query.getField());
      out.writeObject(query.getMin());
      out.writeObject(query.getMax());
      out.writeBoolean(query.includesMax());
      out.writeBoolean(query.includesMin());
      out.writeInt(query.getPrecisionStep());

      out.writeFloat(query.getBoost());

    }
  }

  // spans

  public static class SpanTermQueryStrategy extends QueryStrategy<SpanTermQuery> {
    @Override
    public Class<SpanTermQuery> getQueryClass() {
      return SpanTermQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanTermQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeUTF(query.getTerm().field());
      out.writeUTF(query.getTerm().text());
      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanTermQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanTermQuery query = new SpanTermQuery(new Term(in.readUTF(), in.readUTF()));
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class SpanNotQueryStrategy extends QueryStrategy<SpanNotQuery> {
    @Override
    public Class<SpanNotQuery> getQueryClass() {
      return SpanNotQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanNotQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      serializer.writeQuery(out, query.getInclude());
      serializer.writeQuery(out, query.getExclude());

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanNotQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanNotQuery query = new SpanNotQuery((SpanQuery) serializer.readQuery(in), (SpanQuery) serializer.readQuery(in));
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class SpanFirstQueryStrategy extends QueryStrategy<SpanFirstQuery> {
    @Override
    public Class<SpanFirstQuery> getQueryClass() {
      return SpanFirstQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanFirstQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      serializer.writeQuery(out, query.getMatch());
      out.writeInt(query.getEnd());

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanFirstQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanFirstQuery query = new SpanFirstQuery((SpanQuery) serializer.readQuery(in), in.readInt());
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class SpanPositionRangeQueryStrategy extends QueryStrategy<SpanPositionRangeQuery> {
    @Override
    public Class<SpanPositionRangeQuery> getQueryClass() {
      return SpanPositionRangeQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanPositionRangeQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      serializer.writeQuery(out, query.getMatch());
      out.writeInt(query.getStart());
      out.writeInt(query.getEnd());

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanPositionRangeQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanPositionRangeQuery query = new SpanPositionRangeQuery((SpanQuery) serializer.readQuery(in), in.readInt(), in.readInt());
      query.setBoost(in.readFloat());
      return query;
    }
  }

  public static class SpanOrQueryStrategy extends QueryStrategy<SpanOrQuery> {
    @Override
    public Class<SpanOrQuery> getQueryClass() {
      return SpanOrQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanOrQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      out.writeInt(query.getClauses().length);
      for (SpanQuery clause : query.getClauses()) {
        serializer.writeQuery(out, clause);
      }
      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanOrQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanQuery[] clauses = new SpanQuery[in.readInt()];
      for (int i = 0; i < clauses.length; i++) {
        clauses[i] = (SpanQuery) serializer.readQuery(in);
      }
      SpanOrQuery query = new SpanOrQuery(clauses);
      query.setBoost(in.readFloat());
      return query;
    }
  }


  public static class FieldMaskingSpanQueryStrategy extends QueryStrategy<FieldMaskingSpanQuery> {
    @Override
    public Class<FieldMaskingSpanQuery> getQueryClass() {
      return FieldMaskingSpanQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, FieldMaskingSpanQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());
      serializer.writeQuery(out, query.getMaskedQuery());
      out.writeUTF(query.getField());
      out.writeFloat(query.getBoost());
    }

    @Override
    public FieldMaskingSpanQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      FieldMaskingSpanQuery query = new FieldMaskingSpanQuery((SpanQuery) serializer.readQuery(in), in.readUTF());
      query.setBoost(in.readFloat());
      return query;
    }
  }


  public static class SpanNearQueryStrategy extends QueryStrategy<SpanNearQuery> {
    @Override
    public Class<SpanNearQuery> getQueryClass() {
      return SpanNearQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanNearQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      out.writeInt(query.getClauses().length);
      for (SpanQuery clause : query.getClauses()) {
        serializer.writeQuery(out, clause);
      }

      out.writeInt(query.getSlop());
      out.writeBoolean(query.isInOrder());

      // private field
      try {
        Field collectPayloadsField = SpanNearQuery.class.getDeclaredField("collectPayloads");
        collectPayloadsField.setAccessible(true);
        out.writeBoolean(collectPayloadsField.getBoolean(query));
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }


      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanNearQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {

      SpanQuery[] clauses = new SpanQuery[in.readInt()];
      for (int i = 0; i < clauses.length; i++) {
        clauses[i] = (SpanQuery) serializer.readQuery(in);
      }

      SpanNearQuery query = new SpanNearQuery(clauses, in.readInt(), in.readBoolean(), in.readBoolean());
      query.setBoost(in.readFloat());
      return query;
    }
  }


  public static class SpanNearPayloadCheckQueryStrategy extends QueryStrategy<SpanNearPayloadCheckQuery> {
    @Override
    public Class<SpanNearPayloadCheckQuery> getQueryClass() {
      return SpanNearPayloadCheckQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanNearPayloadCheckQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      serializer.writeQuery(out, query.getMatch());


      // private field
      try {
        Field payloadToMatchField = SpanNearPayloadCheckQuery.class.getDeclaredField("payloadToMatch");
        payloadToMatchField.setAccessible(true);
        out.writeObject(payloadToMatchField.get(query));
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanNearPayloadCheckQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {

      SpanNearPayloadCheckQuery query = new SpanNearPayloadCheckQuery((SpanNearQuery) serializer.readQuery(in), (Collection<byte[]>) in.readObject());
      query.setBoost(in.readFloat());
      return query;
    }
  }


  public static class SpanPayloadCheckQueryStrategy extends QueryStrategy<SpanPayloadCheckQuery> {
    @Override
    public Class<SpanPayloadCheckQuery> getQueryClass() {
      return SpanPayloadCheckQuery.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanPayloadCheckQuery query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      serializer.writeQuery(out, query.getMatch());


      // private field
      try {
        Field payloadToMatchField = SpanPayloadCheckQuery.class.getDeclaredField("payloadToMatch");
        payloadToMatchField.setAccessible(true);
        out.writeObject(payloadToMatchField.get(query));
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanPayloadCheckQuery deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {

      SpanPayloadCheckQuery query = new SpanPayloadCheckQuery((SpanQuery) serializer.readQuery(in), (Collection<byte[]>) in.readObject());
      query.setBoost(in.readFloat());
      return query;
    }
  }


  public static class SpanMultiTermQueryWrapperStrategy extends QueryStrategy<SpanMultiTermQueryWrapper> {
    @Override
    public Class<SpanMultiTermQueryWrapper> getQueryClass() {
      return SpanMultiTermQueryWrapper.class;
    }

    @Override
    public void serialize(LuceneQuerySerializer serializer, ObjectOutputStream out, SpanMultiTermQueryWrapper query) throws IOException {
      out.writeUTF(getQueryClass().getName());

      // private field
      try {
        Field queryField = SpanMultiTermQueryWrapper.class.getDeclaredField("query");
        queryField.setAccessible(true);
        serializer.writeQuery(out, (MultiTermQuery) queryField.get(query));
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (NoSuchFieldException nsfe) {
        throw new RuntimeException(nsfe);
      }

      out.writeFloat(query.getBoost());
    }

    @Override
    public SpanMultiTermQueryWrapper deserialize(LuceneQuerySerializer serializer, ObjectInputStream in) throws IOException, ClassNotFoundException {
      SpanMultiTermQueryWrapper query = new SpanMultiTermQueryWrapper((MultiTermQuery) serializer.readQuery(in));
      query.setBoost(in.readFloat());
      return query;
    }
  }


}
