package org.apache.lucene.store;

/**
 * Factory class to instantiate custom lock handler if one
 * is provided.
 *<P>
 * To use a custom lock mechanism instead of the provided FS
 * lock mechanism, create a new class that extends
 * org.apache.lucene.store.Lock.  For this example, let's call
 * the new lock class <code>foo.MyLocker</code>
 *<P>
 * skeleton of an overridden lock class:
 * <pre>
 *    package foo;
 *
 *    public class MyLocker extends org.apache.lucene.store.Lock {
 *
 *        public boolean obtain() {
 *            // obtain custom lock here and return
 *            // true if obtained, false otherwise
 *            // use the this.getLockName() method to
 *            // make sure you're dealing with the correct lock
 *            return DECISION;
 *        }
 *
 *        public boolean isLocked() {
 *            // check to see if custom lock is set here
 *            // use the this.getLockName() method to
 *            // make sure you're dealing with the correct lock
 *            return DECISION;
 *        }
 *
 *        public void release() {
 *            // include release logic here
 *            // use the this.getLockName() method to
 *            // make sure you're dealing with the correct lock
 *        }
 *
 *    }
 * </pre>
 * <P>
 * When executing program, include a new -D switch specifying the
 * overridden class like this:
 * <P>
 *    <code> java -Dorg.apache.lucene.lockClass=foo.MySQLLocker </code> ...
 *
 *@author     Jeff Patterson (jeffATwebdoyen.com)
 *@created    November 8, 2004
 */
public class LockFactory {

    private final static String OVERRIDE_CLASS = System.getProperty("org.apache.lucene.lockClass");
    private final static boolean OVERRIDDEN = OVERRIDE_CLASS != null;

    /**
     *  Generates a Lock for the given lockName
     *
     *@param  lockName                    Unique name for locking an index
     *@return                             The custom Lock mechanism object
     *@exception  IllegalAccessException  Standard forName.getInstance exception
     *@exception  ClassNotFoundException  Standard forName.getInstance exception
     *@exception  InstantiationException  Standard forName.getInstance exception
     *
     * @see org.apache.lucene.store.Lock
     */
    protected static Lock getLock(String lockName)
        throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        org.apache.lucene.store.Lock lock = (org.apache.lucene.store.Lock)
                                            Class.forName(OVERRIDE_CLASS).newInstance();
        lock.setLockName(lockName);
        return lock;
    }


    /**
     * Determine if the default lock mechanism has been
     * overridden by a custom lock mechanism
     *
     *@return    true if custom class should be used,
     *           false if using default lock mechanism
     */
    protected static boolean isOverridden() {
        return OVERRIDDEN;
    }

}