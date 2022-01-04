package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.INVALID;
import static io.qala.db.TxsStatusTest.*;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXminTests {
    @Test public void tupleIsVisible_ifXminInSnapshot_andXminCommittedIsSet() {
        Tuple t = new Tuple(xid(integer(-1, 9)), null);
        t.xminStatus = TxStatus.COMMITTED;
        Tx x = sut(10, snapshot(9, 11), new TxsStatus());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifCurrentTxCreatedIt() {
        Tuple t = new Tuple(xid(integer()), null);
        t.xminStatus = TxStatus.random();
        Tx x = sut(t.xmin, snapshot(), new TxsStatus());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshot_regardlessOfXminStatus() {
        Tuple t = new Tuple(xid(integer(11, 12)), null);
        t.xminStatus = TxStatus.random();
        Tx x = sut(10, snapshot(9, 10), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshotBecauseItWasActive() {
        Tuple t = new Tuple(xid(10), null);
        t.xminStatus = TxStatus.COMMITTED;
        Tx x = sut(11, snapshot(9, 11, t.xmin), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = TxStatus.ABORTED;
        Tx x = sut(10, snapshot(9, 11), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Tx x = sut(10, snapshot(9, 11), aborted(t.xmin));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminInSnapshotButXminStatusIsUnknown_andActualTxIsStillActive() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Tx x = sut(10, snapshot(9, 11), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleVisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxCommitted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = INVALID;
        Tx x = sut(10, snapshot(9, 11), committed(t.xmin));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXminStatusIfTxFinished() {
        Tuple t = inserted();
        t.xminStatus = INVALID;
        TxStatus newStatus = TxStatus.random();
        Tx x = sut(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), transactions(t.xmin, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xminStatus);
    }

    private static Tx sut(int xid, Snapshot snapshot, TxsStatus activeTxs) {
        return sut(xid(xid), snapshot, activeTxs);
    }
    private static Tx sut(TxId xid, Snapshot snapshot, TxsStatus activeTxs) {
        return new Tx(xid, snapshot, activeTxs);
    }
}