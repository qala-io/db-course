package io.qala.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.qala.db.TxId.assertNotNull;
import static io.qala.db.TxStatus.ABORTED;
import static io.qala.db.TxStatus.INVALID;

public class TxsStatus {
    private final ConcurrentMap<TxId, TxStatus> status = new ConcurrentHashMap<>();

    public void commit(TxId xid) {
        setStatus(xid, TxStatus.COMMITTED);
    }
    public void abort(TxId xid) {
        setStatus(xid, TxStatus.ABORTED);
    }

    public void updateXmaxStatus(Tuple t) {
        TxStatus status;
        if (t.xmax == null)
            status = ABORTED;
        else if (t.xmaxStatus != INVALID)
            status = t.xmaxStatus;
        else
            status = getStatus(t.xmax);
        t.xmaxStatus = status;
    }
    public void updateXminStatus(Tuple t) {
        TxStatus status;
        if(t.xminStatus != INVALID)
            status = t.xminStatus;
        else
            status = getStatus(t.xmin);
        t.xminStatus = status;
    }

    TxStatus getStatus(TxId xid) {
        return status.getOrDefault(assertNotNull(xid), TxStatus.INVALID);
    }
    void setStatus(TxId xid, TxStatus status) {
        this.status.put(assertNotNull(xid), status);
    }
}
