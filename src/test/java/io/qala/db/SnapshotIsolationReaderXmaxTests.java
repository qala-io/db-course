package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.NULL;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.*;
import static io.qala.db.TransactionsTest.*;
import static io.qala.db.TupleTest.deleted;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.*;

public class SnapshotIsolationReaderXmaxTests {
    @Test public void tupleIsInvisible_ifCurrentTxDeletedIt() {
        Tuple t = inserted(xid(integer()));
        t.xmax = xid(integer());
        t.xmaxStatus = TransactionStatus.random();
        Transaction x = new Transaction(t.xmax, snapshot(), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = COMMITTED;
        Transaction x = new Transaction(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new Transactions());
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsInvisible_ifItWasDeletedBeforeCurrentTxStarted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        Transaction x = new Transaction(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), committed(t.xmax));
        assertFalse(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifItWasDeletedAfterCurrentTxStarted_andHasNotBeenCommitted() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        Transaction x = new Transaction(t.xmax.add(-1), snapshot(t.xmax.add(-1), t.xmax), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInTuple() {
        Tuple t = deleted();
        t.xmaxStatus = ABORTED;
        Transaction x = new Transaction(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), new Transactions());
        assertTrue(x.canRead(t));
    }
    @Test public void tupleIsVisible_ifDeletingTxAborted_xmaxStatusInHistory() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        Transaction x = new Transaction(t.xmax.add(1), snapshot(t.xmax, t.xmax.add(1)), aborted(t.xmax));
        assertTrue(x.canRead(t));
    }
    @Test public void updatesTupleXmaxStatusIfTxFinished() {
        Tuple t = deleted();
        t.xmaxStatus = INVALID;
        TransactionStatus newStatus = TransactionStatus.random();
        Transaction x = new Transaction(t.xmin.add(1), snapshot(t.xmax, t.xmax.add(1)), transactions(t.xmax, newStatus));
        x.canRead(t);
        assertEquals(newStatus, t.xmaxStatus);
    }
    @Test public void ifXmaxIsNull_XmaxStatusChangesToAborted() {
        Tuple t = inserted();
        t.xmax = NULL;
        t.xmaxStatus = TransactionStatus.random();
        Transaction x = new Transaction(t.xmin.add(1), snapshot(t.xmin, t.xmin.add(1)), new Transactions());
        assertTrue(x.canRead(t));
        assertEquals(ABORTED, t.xmaxStatus);
    }
}