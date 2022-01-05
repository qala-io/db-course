package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TxsStatusTest {
    @Test public void statusIsWhateverHappenedToTransaction() {
        TxsStatus statuses = new TxsStatus();
        statuses.commit(xid(-100));
        statuses.abort(xid(100));
        assertEquals(COMMITTED, statuses.getStatus(xid(-100)));
        assertEquals(ABORTED, statuses.getStatus(xid(100)));
    }
    @Test public void invalidStatus_ifTxIsNotPresentInHistoryYet() {
        assertEquals(INVALID, new TxsStatus().getStatus(xid(integer())));
    }
    @Test public void throws_IfTxIsNull() {
        assertThrows(IllegalArgumentException.class, ()-> committed((TxId) null));
        assertThrows(IllegalArgumentException.class, ()-> aborted((TxId) null));
    }

    public static TxsStatus aborted(TxId... xids) {
        TxsStatus result = new TxsStatus();
        for (TxId xid : xids)
            result.abort(xid);
        return result;
    }
    public static TxsStatus committed(TxId... xids) {
        TxsStatus result = new TxsStatus();
        for (TxId xid : xids)
            result.commit(xid);
        return result;
    }
    public static TxsStatus transactions(TxId xid, TxStatus status) {
        TxsStatus history = new TxsStatus();
        history.setStatus(xid, status);
        return history;
    }
}