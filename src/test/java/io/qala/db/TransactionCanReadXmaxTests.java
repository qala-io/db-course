package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.COMMITTED;
import static io.qala.db.TransactionStatus.INVALID;
import static io.qala.db.TransactionsTest.committed;
import static io.qala.db.TupleTest.deleted;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionCanReadXmaxTests {
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
}