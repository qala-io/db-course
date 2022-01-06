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

    public void updateXmaxStatus(Tuple t) {
        TxOutcome status;
        if (t.xmax == null)
            status = ABORTED;
        else if (t.xmaxStatus != UNKNOWN)
            status = t.xmaxStatus;
        else
            status = getStatus(t.xmax);
        t.xmaxStatus = status;
    }
    public void updateXminStatus(Tuple t) {
        TxOutcome status;
        if(t.xminStatus != UNKNOWN)
            status = t.xminStatus;
        else
            status = getStatus(t.xmin);
        t.xminStatus = status;
    }

    TxOutcome getStatus(TxId xid) {
        return status.getOrDefault(assertNotNull(xid), TxOutcome.UNKNOWN);
    }
    void setStatus(TxId xid, TxOutcome status) {
        this.status.put(assertNotNull(xid), status);
    }
}
