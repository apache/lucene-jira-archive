package org.apache.lucene.queryParser;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.Query;

public class BugTest {

	public static void main(String[] args) throws ParseException {
		QueryParser qp = new QueryParser("", new SimpleAnalyzer());
		Query q = qp.parse("aaa");
		System.out.println("toString():\n "+q.toString());
		System.out.println("toString(\"xx\"):\n "+q.toString("xx"));
	}
}
