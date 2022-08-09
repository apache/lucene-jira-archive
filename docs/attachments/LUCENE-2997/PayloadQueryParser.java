import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.nodes.FieldQueryNode;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.standard.StandardQueryParser;
import org.apache.lucene.queryParser.standard.builders.StandardQueryBuilder;
import org.apache.lucene.queryParser.standard.builders.StandardQueryTreeBuilder;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.payloads.AveragePayloadFunction;
import org.apache.lucene.search.payloads.PayloadFunction;
import org.apache.lucene.search.payloads.PayloadTermQuery;


/**
 * Query parser suitable for use with Payloads, using the new modular Query Parser structure.
 * 
 * 
 * @author edwardd@wolfram.com
 * @see StandardQueryParser
 */
public class PayloadQueryParser extends StandardQueryParser {
	
	/**
	 * Creates a new PayloadQueryParser.
	 * <br/><br/>
	 * This will use a {@link PayloadQueryTreeBuilder} instance with the default values.
	 */
	public PayloadQueryParser() {
		this(new PayloadQueryNodeBuilder());
	}
	
	/**
	 * Creates a new PayloadQueryParser with the supplied Analyzer and a default {@link PayloadQueryTreeBuilder}.
	 * @param analyzer The analyzer to be used by this query parser.
	 */
	public PayloadQueryParser(Analyzer analyzer) {
		this();
		this.setAnalyzer(analyzer);
	}
	
	/**
	 * Creates a new PayloadQueryParser with the supplied {@link PayloadQueryTreeBuilder}.
	 * @param builder The builder to be used by this query parser.
	 */
	public PayloadQueryParser(PayloadQueryNodeBuilder builder) {
		super();
		this.setQueryBuilder(new PayloadQueryTreeBuilder(builder));
	}
	
	/**
	 * Creates a new PayloadQueryParser with the supplied {@link PayloadQueryTreeBuilder}.
	 * @param builder  The builder to be used by this query parser.
	 * @param analyzer The analyzer to be used by t his query parser.
	 */
	public PayloadQueryParser(PayloadQueryNodeBuilder builder, Analyzer analyzer) {
		this(builder);
		this.setAnalyzer(analyzer);
	}
	
	/**
	 * Class for creating QueryParser nodes using {@link PayloadTermQuery}, suitable for payload usage.
	 * @author edwardd
	 * @see {@link StandardQueryBuilder}
	 * @see {@link PayloadTermQuery}
	 */
	public static class PayloadQueryNodeBuilder implements StandardQueryBuilder {
		private PayloadFunction function;
		private boolean includeSpanScore;
		
		/**
		 * Creates a PayloadQueryNodeBuilder with the default options.
		 * <br/><br/>
		 * The PayloadFunction defaults to {@link AveragePayloadFunction}. <br/>
		 * Including span scores is disabled by default.
		 */
		PayloadQueryNodeBuilder() {
			this(new AveragePayloadFunction());
		}
		
		/**
		 * Creates a new PayloadQueryNodeBuilder with the supplied 
		 * {@link PayloadFunction} and the default value for 
		 * whether to include span scores (false).
		 * @param function  The {@link PayloadFunction} to use to construct {@link PayloadTermQuery}.
		 */
		PayloadQueryNodeBuilder(PayloadFunction function) {
			this(function, false);
		}
		
		/**
		 * Creates a new PayloadQueryNodeBuilder.
		 * 
		 * @param function  The {@link PayloadFunction} to use to construct {@link PayloadTermQuery}.
		 * @param includeSpanScore Whether to include span scores in constructed {@link PayloadTermQuery} instances.
		 * @see {@link PayloadTermQuery}
		 */
		PayloadQueryNodeBuilder(PayloadFunction function, boolean includeSpanScore) {
			this.includeSpanScore = includeSpanScore;
			this.function = function;
		}
		
		/* inherit javadoc */
		@Override
		public Query build(QueryNode queryNode) throws QueryNodeException {
			FieldQueryNode node = (FieldQueryNode) queryNode;
			return new PayloadTermQuery(new Term(node.getFieldAsString(), node.getTextAsString()), function, includeSpanScore);
		}
	}
	
	private static class PayloadQueryTreeBuilder extends StandardQueryTreeBuilder {
		public PayloadQueryTreeBuilder(PayloadQueryNodeBuilder builder) {
			super();
			setBuilder(FieldQueryNode.class, builder);
		}
	}
}
