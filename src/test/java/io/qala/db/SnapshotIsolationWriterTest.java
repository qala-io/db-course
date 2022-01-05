package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.*;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TupleTest.deleted;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.ABORTED;
import static io.qala.db.TxStatus.INVALID;
import static io.qala.db.TupleTest.inserted;
import static io.qala.db.TxsStatusTest.aborted;
import static io.qala.db.TxsStatusTest.committed;
import static org.junit.Assert.*;

public class SnapshotIsolationWriterTest {
    @Test public void setsXmaxForUpdatedTuple() {
        Tuple t = inserted();
        TxId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new TxsStatus());

        writer.write(t, tdata());
        assertEquals(xid, t.xmax);
        assertEquals(INVALID, t.xmaxStatus);
    }
    @Test public void setsXminToInsertedTuple() {
        TxId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new TxsStatus());

        Tuple newT = writer.write(nullOr(inserted()), tdata());
        assertEquals(xid, newT.xmin);
        assertEquals(INVALID, newT.xminStatus);
    }
    @Test public void errsIfSomeoneCommittedNewVersion_sinceTxStarted() {
        TxId xid = xid(integer());
        TxWriter sut = sut(xid, snapshot(xid, xid), committed(xid.add(1)));
        assertThrows(ConcurrentUpdateException.class, () -> sut.write(deleted(xid.add(1)), tdata()));
    }
    @Test public void updatesIfSomeoneAbortedNewVersion_sinceTxStarted() {
        Tuple deleted = deleted();
        deleted.xmaxStatus = sample(INVALID, ABORTED);
        TxId xid = deleted.xmax.add(-1);//we start before
        TxWriter sut = sut(xid, snapshot(xid, xid), aborted(deleted.xmax));

        Tuple t = sut.write(deleted, tdata());
        assertEquals(xid, deleted.xmax);
        assertEquals(INVALID, deleted.xmaxStatus);
        assertNull(t.xmax);
        assertEquals(ABORTED, t.xmaxStatus);
    }

    private static TxWriter sut(TxId xid, Snapshot snapshot, TxsStatus txsStatus) {
        return new SnapshotIsolationWriter(xid, snapshot, txsStatus);
    }
    private static Object[] tdata() {
        return new Object[0];
    }
}
