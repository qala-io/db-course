package io.qala.db.pg;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.pg.SnapshotTest.snapshot;
import static io.qala.db.pg.TxId.xid;
import static io.qala.db.pg.TxOutcome.*;
import static io.qala.db.pg.TxsOutcomesTest.*;
import static io.qala.db.pg.TupleTest.deleted;
import static io.qala.db.pg.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXmaxTests {
    @Test public void tupleIsInvisible_ifCurrentTxDeletedIt() {
        Tuple t = inserted(xid(integer()));
        t.xmax = xid(integer());
        t.xmaxStatus = TxOutcome.random();
        TxReader x = sut(t.xmax, snapshot(), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = COMMITTED;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new TxsOutcomes());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = UNKNOWN;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), committed(t.xmax));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifItWasDeletedAfterCurrentTxStarted_andHasNotBeenCommitted() {
        Tuple t = deleted();
        t.xmaxStatus = UNKNOWN;
        TxReader x = sut(t.xmax.add(-1), snapshot(t.xmax.add(-1), t.xmax), new TxsOutcomes());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = ABORTED;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new TxsOutcomes());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = UNKNOWN;
        TxReader x = sut(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), aborted(t.xmax));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXmaxStatusIfTxFinished() {
        Tuple t = deleted();
        t.xmaxStatus = UNKNOWN;
        TxOutcome newStatus = TxOutcome.random();
        TxReader x = sut(t.xmin.add(1), snapshot(t.xmax, t.xmax.add(1)), transactions(t.xmax, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xmaxStatus);
    }
    @Test public void ifXmaxIsNull_XmaxStatusChangesToAborted() {
        Tuple t = inserted();
        t.xmaxStatus = TxOutcome.random();
        TxReader x = sut(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), new TxsOutcomes());
        assertTrue(x.canRead(t));
        assertEquals(ABORTED, t.xmaxStatus);
    }

    private static TxReader sut(TxId xid, Snapshot snapshot, TxsOutcomes txsOutcomes) {
        return new SnapshotIsolationReader(xid, snapshot, txsOutcomes);
    }
}