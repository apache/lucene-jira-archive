package com.jerabi.spellchecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.BreakIterator;
import java.util.Locale;

public class BreakIteratorTest {

	public static void printEachForward(BreakIterator boundary, String source) {
	     int start = boundary.first();
	     for (int end = boundary.next();
	          end != BreakIterator.DONE;
	          start = end, end = boundary.next()) {
	          System.out.println(source.substring(start,end));
	     }
	 }
	
	public static void printEachBackward(BreakIterator boundary, String source) {
	     int end = boundary.last();
	     for (int start = boundary.previous();
	          start != BreakIterator.DONE;
	          end = start, start = boundary.previous()) {
	         System.out.println(source.substring(start,end));
	     }
	 }
	
	public static void printFirst(BreakIterator boundary, String source) {
	     int start = boundary.first();
	     int end = boundary.next();
	     System.out.println(source.substring(start,end));
	 }
	
	public static void printLast(BreakIterator boundary, String source) {
	     int end = boundary.last();
	     int start = boundary.previous();
	     System.out.println(source.substring(start,end));
	 }
	
	public static void printAt(BreakIterator boundary, int pos, String source) {
	     int end = boundary.following(pos);
	     int start = boundary.previous();
	     System.out.println(source.substring(start,end));
	 }
	
	public static int nextWordStartAfter(int pos, String text) {
	     BreakIterator wb = BreakIterator.getWordInstance();
	     wb.setText(text);
	     int last = wb.following(pos);
	     int current = wb.next();
	     while (current != BreakIterator.DONE) {
	         for (int p = last; p < current; p++) {
	             if (Character.isLetter(text.codePointAt(p)))
	                 return last;
	         }
	         last = current;
	         current = wb.next();
	     }
	     return BreakIterator.DONE;
	 }
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/apache/lucene/search/postingshighlight/package.html"));
		
		StringBuilder sb = new StringBuilder();
		String line = null;
		while((line=reader.readLine())!=null){
			sb.append(line).append("\n");
		}
		
		BreakIterator boundary = null;
		
		String stringToExamine = sb.toString();
        
        
		//boundary = BreakIterator.getWordInstance();
		boundary = BreakIterator.getSentenceInstance();
		boundary.setText(stringToExamine);

		//print each word in order
        printEachForward(boundary, stringToExamine);
        /*
        //print each sentence in reverse order
        boundary = BreakIterator.getSentenceInstance(Locale.US);
        boundary.setText(stringToExamine);
        printEachBackward(boundary, stringToExamine);
        printFirst(boundary, stringToExamine);
        printLast(boundary, stringToExamine);
        */

	}
	
	

}
