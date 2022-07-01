import java.util.BitSet;

import org.apache.lucene.search.HitCollector;

public class MyHitCollector extends HitCollector
{
	private final BitSet bits = new BitSet();

	@Override
	public void collect( final int doc, final float score )
	{
		bits.set( doc );
	}

	public int hitCount()
	{
		return bits.cardinality();
	}
}
