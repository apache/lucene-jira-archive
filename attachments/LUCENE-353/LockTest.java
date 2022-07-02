/*
 * 
 * 
 * Copyright (C) 2005 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2005 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.apache.lucene.store;

import java.io.IOException;

import junit.framework.TestCase;

public class LockTest extends TestCase {

    public void testObtain() {
        LockMock l = new LockMock();
        Lock.LOCK_POLL_INTERVAL = 10;

        try {
            l.obtain(Lock.LOCK_POLL_INTERVAL);
            fail("Should have failed to obtain lock");
        } catch (IOException e) {
            assertEquals("should attempt to lock more than once", l.lockAttempts, 2);
        }
    }

    private class LockMock extends Lock {
        public int lockAttempts;

        public boolean obtain() throws IOException {
            lockAttempts++;
            return false;
        }

        public void release() {
            // do nothing
        }

        public boolean isLocked() {
            return false;
        }
    }
}
