package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.SnapshotTest.snapshot;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.INVALID;
import static io.qala.db.TupleTest.inserted;
import static org.junit.Assert.assertEquals;

public class SnapshotIsolationWriterTest {
    @Test public void setsXmaxForUpdateTuple() {
        Tuple t = inserted();
        TransactionId xid = xid(integer());
        TxWriter writer = writer(xid, snapshot(), new Transactions());

        writer.write(t, tdata());
        assertEquals(xid, t.xmax);
        assertEquals(INVALID, t.xmaxStatus);
    }

    private static TxWriter writer(TransactionId xid, Snapshot snapshot, Transactions transactions) {
        return new SnapshotIsolationWriter(xid, snapshot, transactions);
    }
    private static Object[] tdata() {
        return new Object[0];
    }
}
