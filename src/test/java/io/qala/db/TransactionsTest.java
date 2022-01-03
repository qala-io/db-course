package io.qala.db;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.datagen.RandomShortApi.nullOr;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TransactionsTest {
    @Test public void statusIsWhateverHappenedToTransaction() {
        Transactions statuses = new Transactions();
        statuses.commit(xid(-100));
        statuses.abort(xid(100));
        assertEquals(COMMITTED, statuses.getStatus(xid(-100)));
        assertEquals(ABORTED, statuses.getStatus(xid(100)));
    }
    @Test public void invalidStatus_ifTxIsNotPresentInHistoryYet() {
        assertEquals(INVALID, new Transactions().getStatus(xid(integer())));
    }
    @Test public void throws_IfTxIsNull() {
        assertThrows(IllegalArgumentException.class, ()-> committed(nullOr(TransactionId.NULL)));
        assertThrows(IllegalArgumentException.class, ()-> aborted(nullOr(TransactionId.NULL)));
    }

    public static Transactions aborted(TransactionId ... xids) {
        Transactions result = new Transactions();
        for (TransactionId xid : xids)
            result.abort(xid);
        return result;
    }
    public static Transactions committed(TransactionId ... xids) {
        Transactions result = new Transactions();
        for (TransactionId xid : xids)
            result.commit(xid);
        return result;
    }
    public static Transactions transactions(TransactionId xid, TransactionStatus status) {
        Transactions history = new Transactions();
        history.setStatus(xid, status);
        return history;
    }
}