package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionsTest.aborted;
import static io.qala.db.TransactionsTest.committed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionCanReadXminTest {
    @Test public void tupleIsVisible_ifXminInSnapshot_andXminCommittedIsSet() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(-1, 9));
        t.xminStatus = TransactionStatus.COMMITTED;
        Transaction x = new Transaction(xid(10), snapshot(9, 11), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifXminIsCurrent() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer());
        t.xminStatus = TransactionStatus.random();
        Transaction x = new Transaction(t.xmin, snapshot(), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshot_regardlessOfXminStatus() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(11, 12));
        t.xminStatus = TransactionStatus.random();
        Transaction x = new Transaction(xid(10), snapshot(9, 10), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshotBecauseItWasActive() {
        Tuple t = new Tuple(null);
        t.xmin = xid(10);
        t.xminStatus = TransactionStatus.COMMITTED;
        Transaction x = new Transaction(xid(11), snapshot(9, 11, t.xmin), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsAborted() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(8, 9));
        t.xminStatus = TransactionStatus.ABORTED;
        Transaction x = new Transaction(xid(10), snapshot(9, 11), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxStatusIsAborted() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(8, 9));
        t.xminStatus = TransactionStatus.INVALID;
        Transaction x = new Transaction(xid(10), snapshot(9, 11), aborted(t.xmin));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxIsStillActive() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(8, 9));
        t.xminStatus = TransactionStatus.INVALID;
        Transaction x = new Transaction(xid(10), snapshot(9, 11), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleVisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxCommitted() {
        Tuple t = new Tuple(null);
        t.xmin = xid(integer(8, 9));
        t.xminStatus = TransactionStatus.INVALID;
        Transaction x = new Transaction(xid(10), snapshot(9, 11), committed(t.xmin));
        assertTrue(x.canRead(t));
    }
}