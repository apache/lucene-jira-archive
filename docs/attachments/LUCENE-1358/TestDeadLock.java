/**
 *
 */
package Test;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

/**
 *
 */
public class TestDeadLock {

    public static class DeadLock extends Thread {
        Query q1;
        Query q2;
        public DeadLock( Query q1, Query q2 ) {
            this.q1 = q1;
            this.q2 = q2;
        }

        @Override
        public void run() {
            int count = 0;
            while( true ) {
                System.out.println( new StringBuilder( "Pre equals "  ).append( this ).append( " count: " ).append( ++count ) );
                q1.equals( q2 );
                System.out.println( new StringBuilder( "Post equals " ).append( this ).append( " count: " ).append(   count ) );
            }
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) throws InterruptedException {

        PhraseQuery q1 = new PhraseQuery();
        q1.add( new Term( "field", "txt" ) );
        PhraseQuery q2 = new PhraseQuery();
        q2.add( new Term( "field", "txt" ) );

        System.out.println( "Starting 1" );
        Thread t1 = new DeadLock( q1, q2 );
        t1.start();

        Thread.sleep( 1 );

        System.out.println( "Starting 2" );
        Thread t2 = new DeadLock( q2, q1 );
        t2.start();

        t1.join();
        t2.join();

        System.out.println( "Huh?" );
    }

}
