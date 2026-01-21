package io.qala.db.pg;

import org.junit.Test;

import static io.qala.db.pg.SnapshotIsolationWriterTest.inThread;
import static io.qala.db.pg.SnapshotIsolationWriterTest.tdata;
import static io.qala.db.pg.TupleTest.deleted;
import static io.qala.db.TxIsolationLevel.SNAPSHOT;
import static org.junit.Assert.*;

public class SnapshotIsolationTest {
    @Test public void uncommittedReadAreNotPossible() {
        Db db = new Db();
        Tx tx1 = db.beginTx(SNAPSHOT);
        Tuple t = tx1.write(null, null);
        Tx tx2 = db.beginTx(SNAPSHOT);
        assertFalse(tx2.canRead(t));
    }
    @Test public void repeatableRead_doesNotAllowReadingValueCommittedAfterTxStarted() {
        Db db = new Db();
        Tx tx1 = db.beginTx(SNAPSHOT);
        Tuple t = tx1.write(null, null);

        Tx tx2 = db.beginTx(SNAPSHOT);
        db.commit(tx1.id);
        assertFalse(tx2.canRead(t));
    }
    @Test public void canReadTuples_committedBeforeTxStarts() {
        Db db = new Db();
        Tx tx1 = db.beginTx(SNAPSHOT);
        Tuple t = tx1.write(null, null);
        db.commit(tx1.id);

        Tx tx2 = db.beginTx(SNAPSHOT);
        assertTrue(tx2.canRead(t));
    }
    @Test public void errsIfTupleIsUpdatedInParallel() {
        Db db = new Db();
        Tx tx1 = db.beginTx(SNAPSHOT);
        Tx tx2 = db.beginTx(SNAPSHOT);

        Tuple t = deleted(tx1.id);
        inThread(() -> db.commit(tx1.id));
        assertThrows(ConcurrentUpdateException.class, () -> tx2.write(t, tdata()));
    }
    @Test public void continuesIfParallelTupleUpdatedAborted() {
        Db db = new Db();
        Tuple t = insert(db);

        Tx tx1 = db.beginTx(SNAPSHOT);
        tx1.write(t, tdata());
        Tx tx2 = db.beginTx(SNAPSHOT);

        inThread(() -> db.abort(tx1.id));
        tx2.write(t, tdata());
        assertEquals(tx2.id, t.xmax);
        assertEquals(TxOutcome.UNKNOWN, t.xmaxStatus);
    }

    private Tuple insert(Db db) {
        Tx beforeAll = db.beginTx(SNAPSHOT);
        Tuple t = beforeAll.write(null, tdata());
        db.commit(beforeAll.id);
        return t;
    }
}
