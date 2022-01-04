package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.INVALID;
import static io.qala.db.TransactionsTest.*;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXminTests {
    @Test public void tupleIsVisible_ifXminInSnapshot_andXminCommittedIsSet() {
        Tuple t = new Tuple(xid(integer(-1, 9)), null);
        t.xminStatus = TransactionStatus.COMMITTED;
        Transaction x = sut(10, snapshot(9, 11), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifCurrentTxCreatedIt() {
        Tuple t = new Tuple(xid(integer()), null);
        t.xminStatus = TransactionStatus.random();
        Transaction x = sut(t.xmin, snapshot(), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshot_regardlessOfXminStatus() {
        Tuple t = new Tuple(xid(integer(11, 12)), null);
        t.xminStatus = TransactionStatus.random();
        Transaction x = sut(10, snapshot(9, 10), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshotBecauseItWasActive() {
        Tuple t = new Tuple(xid(10), null);
        t.xminStatus = TransactionStatus.COMMITTED;
        Transaction x = sut(11, snapshot(9, 11, t.xmin), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = TransactionStatus.ABORTED;
        Transaction x = sut(10, snapshot(9, 11), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Transaction x = sut(10, snapshot(9, 11), aborted(t.xmin));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminInSnapshotButXminStatusIsUnknown_andActualTxIsStillActive() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Transaction x = sut(10, snapshot(9, 11), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleVisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxCommitted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Transaction x = sut(10, snapshot(9, 11), committed(t.xmin));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXminStatusIfTxFinished() {
        Tuple t = inserted();
        t.xminStatus = INVALID;
        TransactionStatus newStatus = TransactionStatus.random();
        Transaction x = sut(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), transactions(t.xmin, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xminStatus);
    }

    private static Transaction sut(int xid, Snapshot snapshot, Transactions activeTxs) {
        return sut(xid(xid), snapshot, activeTxs);
    }
    private static Transaction sut(TransactionId xid, Snapshot snapshot, Transactions activeTxs) {
        return new Transaction(xid, snapshot, activeTxs);
    }
}