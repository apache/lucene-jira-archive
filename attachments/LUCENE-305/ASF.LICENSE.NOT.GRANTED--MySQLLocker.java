package foo;

import java.io.IOException;
import java.sql.*;

/**
 *  Demonstration of an external lock class for Lucene
 * code consideration (to fit the Pluggable Lock Framework
 * suggestion on Bugzilla)
 *
 * Requierd for usage:
 *
 *   - MySQL must be installed (or another compliant SQL engine)
 *   - Build the following table in schema TEST:
 *      create table lucene_lock (lock_name VARCHAR(255) UNIQUE,touch_time TIMESTAMP);
 *   - Modify the DB constants at top of class for your database
 *   - classpath that contains MySQL driver
 *   - start java with a -Dorg.apache.lucene.lockClass=foo.MySQLLocker switch
 *
 * Obviously, this class is just a demo.  A more robust
 * implementation would be required for a production system,
 * but this demonstrates the basic usage of a pluggable
 * locker.  For example, the DB connection is set up via a
 * static block that does not handle exceptions well at all...
 *
 *@author     Jeff Patterson (jeffATwebdoyen.com)
 *@created    November 8, 2004
 */
public class MySQLLocker extends org.apache.lucene.store.Lock {

    // STATIC MEMBERS
    private final static String DB_DRIVER = "com.mysql.jdbc.Driver";
    private final static String DB_URL = "jdbc:mysql://localhost:3306/test";
    private final static String DB_USERNAME = "";
    private final static String DB_PASSWORD = "";
    private static Connection con = null;
    private static PreparedStatement checkLockStatement = null;
    private static PreparedStatement obtainLockStatement = null;
    private static PreparedStatement releaseLockStatement = null;

    // OBJECT MEMBERS
    private String lockName = null;

    /**
     * Static block should be replaced with something
     * more robust - just used due to demo
     */
     static {
        try {
            Class.forName(DB_DRIVER);
            con = DriverManager.getConnection(DB_URL,DB_USERNAME,DB_PASSWORD);
            checkLockStatement = con.prepareStatement("select lock_name from lucene_lock where lock_name = ?");
            obtainLockStatement = con.prepareStatement("insert into lucene_lock values (?,NOW())");
            releaseLockStatement = con.prepareStatement("delete from lucene_lock where lock_name = ?");
        }catch(Exception e) {
            // VERY UGLY, but good for testing...
            e.printStackTrace();
            System.exit(-1);
        }
     }

    /**
     *  Constructor for the MySQLLocker object
     * Doesn't do anything...
     */
    public MySQLLocker() {
        super();
    }

    /**
     *  Required implementation of the obtain method from abstract Lock
     *
     * This method will fail if it is unable to insert the lock again and
     * throw an SQL exception that we return as a failure to obtain a lock.
     * This is dependant on the LOCK_NAME column in the lucene_lock table
     * to have the UNIQUE attribute.  See the main comments at top of class.
     *
     * NOTE: Logically, this no longer should throw IOException, but instead
     * something like LockAttemptException that could encapsulate a
     * throwable of an appropriate type. This would allow a SQLLocker
     * to throw a nested SQLException, or a FS Locker to throw an
     * IOException, etc...but for now, the interface requires IOException
     *
     *@return                  true if lock obtained, else false
     *@exception  IOException  Problems establishing attempt on Lock
     */
    public boolean obtain() throws IOException {
        try {
            obtainLockStatement.execute();
            return true;
        } catch(SQLException e) {
            return false;
        }
    }

    /**
     *  Required implementation of the release method from abstract Lock
     */
    public void release() {
        try {
            releaseLockStatement.execute();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     *  Required implementation of the isLocked method from abstract Lock
     *
     *@return    true if this lock is locked, else false
     */
    public boolean isLocked() {
        try {
            return checkLockStatement.executeQuery().next();
        } catch(Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Should be a little better on the error handling here,
     * but again, just a demo
     */
    public void setLockName(String value) {
        super.setLockName( value );
        try {
            checkLockStatement.setString(1,value);
            obtainLockStatement.setString(1,value);
            releaseLockStatement.setString(1,value);
        }catch(Exception e) {
            // we should more elegantly handle this exception
            // just dump due to demo
            e.printStackTrace();
        }
    }

    /**
     *  Overload of toString() (of course not required)
     *
     *@return    Description of the Return Value
     */
    public String toString() {
        return super.toString() + "|lockName=" + this.getLockName();
    }

    /**
     * Just a simple tester main
     */
    public static void main(String[] args) {
        try {
            MySQLLocker locker = new MySQLLocker();
            locker.setLockName("test");
            System.out.println( "locked?   " + locker.isLocked() );
            System.out.println( "obtained? " + locker.obtain() );
            System.out.println( "locked?   " + locker.isLocked() );
            System.out.println( "releasing...");
            locker.release();
            System.out.println( "locked?   " + locker.isLocked() );
            // clean up our connection
            con.close();
            System.exit(0);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

