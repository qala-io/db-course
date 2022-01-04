package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.datagen.RandomShortApi.nullOr;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.INVALID;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.assertEquals;

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

    private static TxWriter sut(TxId xid, Snapshot snapshot, TxsStatus txsStatus) {
        return new SnapshotIsolationWriter(xid, snapshot, txsStatus);
    }
    private static Object[] tdata() {
        return new Object[0];
    }
}
