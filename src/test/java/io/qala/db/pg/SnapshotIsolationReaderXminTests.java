package io.qala.db.pg;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.pg.SnapshotTest.snapshot;
import static io.qala.db.pg.TxId.xid;
import static io.qala.db.pg.TxOutcome.UNKNOWN;
import static io.qala.db.pg.TxsOutcomesTest.*;
import static io.qala.db.pg.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXminTests {
    @Test public void tupleIsVisible_ifXminInSnapshot_andXminCommittedIsSet() {
        Tuple t = new Tuple(xid(integer(-1, 9)), null);
        t.xminStatus = TxOutcome.COMMITTED;
        TxReader x =  sut(10, snapshot(9, 11), new TxsOutcomes());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifCurrentTxCreatedIt() {
        Tuple t = new Tuple(xid(integer()), null);
        t.xminStatus = TxOutcome.random();
        TxReader x =  sut(t.xmin, snapshot(), new TxsOutcomes());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshot_regardlessOfXminStatus() {
        Tuple t = new Tuple(xid(integer(11, 12)), null);
        t.xminStatus = TxOutcome.random();
        TxReader x =  sut(10, snapshot(9, 10), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminNotInSnapshotBecauseItWasActive() {
        Tuple t = new Tuple(xid(10), null);
        t.xminStatus = TxOutcome.COMMITTED;
        TxReader x =  sut(11, snapshot(9, 11, t.xmin), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = TxOutcome.ABORTED;
        TxReader x =  sut(10, snapshot(9, 11), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxStatusIsAborted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = UNKNOWN;
        TxReader x =  sut(10, snapshot(9, 11), aborted(t.xmin));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifXminInSnapshotButXminStatusIsUnknown_andActualTxIsStillActive() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = UNKNOWN;
        TxReader x =  sut(10, snapshot(9, 11), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleVisible_ifInXminInSnapshotButXminStatusIsUnknown_andActualTxCommitted() {
        Tuple t = new Tuple(xid(integer(8, 9)), null);
        t.xminStatus = UNKNOWN;
        TxReader x =  sut(10, snapshot(9, 11), committed(t.xmin));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXminStatusIfTxFinished() {
        Tuple t = inserted();
        t.xminStatus = UNKNOWN;
        TxOutcome newStatus = TxOutcome.random();
        TxReader x =  sut(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), transactions(t.xmin, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xminStatus);
    }

    private static TxReader sut(int xid, Snapshot snapshot, TxsOutcomes activeTxs) {
        return sut(xid(xid), snapshot, activeTxs);
    }
    private static TxReader sut(TxId xid, Snapshot snapshot, TxsOutcomes activeTxs) {
        return new SnapshotIsolationReader(xid, snapshot, activeTxs);
    }
}