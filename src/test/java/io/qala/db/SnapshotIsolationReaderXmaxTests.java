package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.*;
import static io.qala.db.TxsStatusTest.*;
import static io.qala.db.TupleTest.deleted;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXmaxTests {
    @Test public void tupleIsInvisible_ifCurrentTxDeletedIt() {
        Tuple t = inserted(xid(integer()));
        t.xmax = xid(integer());
        t.xmaxStatus = TxStatus.random();
        TxReader x = sut(t.xmax, snapshot(), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = COMMITTED;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new TxsStatus());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), committed(t.xmax));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifItWasDeletedAfterCurrentTxStarted_andHasNotBeenCommitted() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        TxReader x = sut(t.xmax.add(-1), snapshot(t.xmax.add(-1), t.xmax), new TxsStatus());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = ABORTED;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new TxsStatus());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), aborted(t.xmax));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXmaxStatusIfTxFinished() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        TxStatus newStatus = TxStatus.random();
        TxReader x = sut(t.xmin.add(1), snapshot(t.xmax, t.xmax.add(1)), transactions(t.xmax, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xmaxStatus);
    }
    @Test public void ifXmaxIsNull_XmaxStatusChangesToAborted() {
        Tuple t = inserted();
        t.xmaxStatus = TxStatus.random();
        TxReader x = sut(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), new TxsStatus());
        assertTrue(x.canRead(t));
        assertEquals(ABORTED, t.xmaxStatus);
    }

    private static TxReader sut(TxId xid, Snapshot snapshot, TxsStatus txsStatus) {
        return new SnapshotIsolationReader(xid, snapshot, txsStatus);
    }
}