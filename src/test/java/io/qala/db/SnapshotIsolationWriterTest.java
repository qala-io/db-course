package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.*;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TupleTest.deleted;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxOutcome.ABORTED;
import static io.qala.db.TxOutcome.UNKNOWN;
import static io.qala.db.TupleTest.inserted;
import static io.qala.db.TxsOutcomesTest.aborted;
import static io.qala.db.TxsOutcomesTest.committed;
import static org.junit.Assert.*;

public class SnapshotIsolationWriterTest {
    @Test public void setsXmaxForUpdatedTuple() {
        Tuple t = inserted();
        TxId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new TxsOutcomes());

        writer.write(t, tdata());
        assertEquals(xid, t.xmax);
        assertEquals(UNKNOWN, t.xmaxStatus);
    }
    @Test public void setsXminToInsertedTuple() {
        TxId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new TxsOutcomes());

        Tuple newT = writer.write(nullOr(inserted()), tdata());
        assertEquals(xid, newT.xmin);
        assertEquals(UNKNOWN, newT.xminStatus);
    }
    @Test public void errsIfSomeoneCommittedNewVersion_sinceTxStarted() {
        TxId xid = xid(integer());
        TxWriter sut = sut(xid, snapshot(xid, xid), committed(xid.add(1)));
        assertThrows(ConcurrentUpdateException.class, () -> sut.write(deleted(xid.add(1)), tdata()));
    }
    @Test public void updatesIfSomeoneAbortedNewVersion_sinceTxStarted() {
        Tuple deleted = deleted();
        deleted.xmaxStatus = sample(UNKNOWN, ABORTED);
        TxId xid = deleted.xmax.add(-1);//we start before
        TxWriter sut = sut(xid, snapshot(xid, xid), aborted(deleted.xmax));

        Tuple t = sut.write(deleted, tdata());
        assertEquals(xid, deleted.xmax);
        assertEquals(UNKNOWN, deleted.xmaxStatus);
        assertNull(t.xmax);
        assertEquals(ABORTED, t.xmaxStatus);
    }
    @Test public void errsIfSomeoneHasNotCommittedNewVersion_andThenCommits() {
        Tuple deleted = deleted();
        deleted.xmaxStatus = UNKNOWN;
        TxId xid = deleted.xmax.add(-1);//we start before
        TxWriter sut = sut(xid, snapshot(xid, xid), new TxsOutcomes(/*means this tx is still running*/));

        Tuple t = sut.write(deleted, tdata());
        assertEquals(xid, deleted.xmax);
        assertEquals(UNKNOWN, deleted.xmaxStatus);
        assertNull(t.xmax);
        assertEquals(ABORTED, t.xmaxStatus);
    }

    private static TxWriter sut(TxId xid, Snapshot snapshot, TxsOutcomes txsOutcomes) {
        return new SnapshotIsolationWriter(xid, snapshot, txsOutcomes);
    }
    private static Object[] tdata() {
        return new Object[0];
    }
}
