package io.qala.db;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SnapshotIsolationTest {
    @Test public void uncommittedReadAreNotPossible() {
        Db db = new Db();
        Tx tx1 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        Tuple t = tx1.write(null, null);
        Tx tx2 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertFalse(tx2.canRead(t));
    }
    @Test public void repeatableRead_doesNotAllowReadingValueCommittedAfterTxStarted() {
        Db db = new Db();
        Tx tx1 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        Tuple t = tx1.write(null, null);

        Tx tx2 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        db.commit(tx1.id);
        assertFalse(tx2.canRead(t));
    }
    @Test public void canReadTuples_committedBeforeTxStarts() {
        Db db = new Db();
        Tx tx1 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        Tuple t = tx1.write(null, null);
        db.commit(tx1.id);

        Tx tx2 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertTrue(tx2.canRead(t));
    }
}
