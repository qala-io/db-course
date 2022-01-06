package io.qala.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.qala.db.TxId.assertNotNull;
import static io.qala.db.TxOutcome.ABORTED;
import static io.qala.db.TxOutcome.UNKNOWN;

public class TxsOutcomes {
    private final ConcurrentMap<TxId, TxOutcome> status = new ConcurrentHashMap<>();

    public void commit(TxId xid) {
        setStatus(xid, TxOutcome.COMMITTED);
    }
    public void abort(TxId xid) {
        setStatus(xid, TxOutcome.ABORTED);
    }

    /**
     * https://github.com/postgres/postgres/blob/fd0625c7a9c679c0c1e896014b8f49a489c3a245/src/backend/access/heap/heapam_visibility.c#L457
     */
    public void updateXmaxStatus(Tuple t, boolean waitIfUnknown) {
        TxOutcome status;
        if (t.xmax == null)
            status = ABORTED;
        else if (t.xmaxStatus != UNKNOWN)
            status = t.xmaxStatus;
        else if(waitIfUnknown)
            status = getOrWaitForStatus(t.xmax);
        else
            status = getOutcome(t.xmax);
        t.xmaxStatus = status;
    }
    public void updateXminStatus(Tuple t) {
        TxOutcome status;
        if(t.xminStatus != UNKNOWN)
            status = t.xminStatus;
        else
            status = getOutcome(t.xmin);
        t.xminStatus = status;
    }

    /**
     * https://github.com/postgres/postgres/blob/bcf60585e6e0e95f0b9e5d64c7a6edca99ec6e86/src/backend/access/heap/heapam.c#L3326
     */
    TxOutcome getOrWaitForStatus(TxId xid) {
        TxOutcome outcome;
        //noinspection StatementWithEmptyBody spin until TX finishes
        while((outcome = getOutcome(xid)) == UNKNOWN);
        return outcome;
    }
    TxOutcome getOutcome(TxId xid) {
        return status.getOrDefault(assertNotNull(xid), TxOutcome.UNKNOWN);
    }
    void setStatus(TxId xid, TxOutcome status) {
        this.status.put(assertNotNull(xid), status);
    }
}
