package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxOutcome.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TxsOutcomesTest {
    @Test public void statusIsWhateverHappenedToTransaction() {
        TxsOutcomes statuses = new TxsOutcomes();
        statuses.commit(xid(-100));
        statuses.abort(xid(100));
        assertEquals(COMMITTED, statuses.getStatus(xid(-100)));
        assertEquals(ABORTED, statuses.getStatus(xid(100)));
    }
    @Test public void invalidStatus_ifTxIsNotPresentInHistoryYet() {
        assertEquals(UNKNOWN, new TxsOutcomes().getStatus(xid(integer())));
    }
    @Test public void throws_IfTxIsNull() {
        assertThrows(IllegalArgumentException.class, ()-> committed((TxId) null));
        assertThrows(IllegalArgumentException.class, ()-> aborted((TxId) null));
    }

    public static TxsOutcomes aborted(TxId... xids) {
        TxsOutcomes result = new TxsOutcomes();
        for (TxId xid : xids)
            result.abort(xid);
        return result;
    }
    public static TxsOutcomes committed(TxId... xids) {
        TxsOutcomes result = new TxsOutcomes();
        for (TxId xid : xids)
            result.commit(xid);
        return result;
    }
    public static TxsOutcomes transactions(TxId xid, TxOutcome status) {
        TxsOutcomes history = new TxsOutcomes();
        history.setStatus(xid, status);
        return history;
    }
}