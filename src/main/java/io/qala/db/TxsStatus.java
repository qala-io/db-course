package io.qala.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.qala.db.TxId.assertNotNull;

public class TxsStatus {
    private final ConcurrentMap<TxId, TxStatus> status = new ConcurrentHashMap<>();

    public void commit(TxId xid) {
        setStatus(xid, TxStatus.COMMITTED);
    }
    public void abort(TxId xid) {
        setStatus(xid, TxStatus.ABORTED);
    }
    public void setStatus(TxId xid, TxStatus status) {
        this.status.put(assertNotNull(xid), status);
    }

    public TxStatus getStatus(TxId xid) {
        return status.getOrDefault(assertNotNull(xid), TxStatus.INVALID);
    }
}
