package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.datagen.RandomShortApi.nullOr;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.INVALID;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.assertEquals;

public class SnapshotIsolationWriterTest {
    @Test public void setsXmaxForUpdatedTuple() {
        Tuple t = inserted();
        TransactionId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new Transactions());

        writer.write(t, tdata());
        assertEquals(xid, t.xmax);
        assertEquals(INVALID, t.xmaxStatus);
    }
    @Test public void setsXminToInsertedTuple() {
        TransactionId xid = xid(integer());
        TxWriter writer = sut(xid, snapshot(), new Transactions());

        Tuple newT = writer.write(nullOr(inserted()), tdata());
        assertEquals(xid, newT.xmin);
        assertEquals(INVALID, newT.xminStatus);
    }

    private static TxWriter sut(TransactionId xid, Snapshot snapshot, Transactions transactions) {
        return new SnapshotIsolationWriter(xid, snapshot, transactions);
    }
    private static Object[] tdata() {
        return new Object[0];
    }
}
