package io.qala.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.qala.db.TransactionId.assertNotNull;

public class Transactions {
    private final ConcurrentMap<TransactionId, TransactionStatus> status = new ConcurrentHashMap<>();
    public void commit(TransactionId xid) {
        setStatus(xid, TransactionStatus.COMMITTED);
    }
    public void abort(TransactionId xid) {
        setStatus(xid, TransactionStatus.ABORTED);
    }
    public void setStatus(TransactionId xid, TransactionStatus status) {
        this.status.put(assertNotNull(xid), status);
    }

    public boolean isCommitted(TransactionId xid) {
        return getStatus(xid) == TransactionStatus.COMMITTED;
    }
    public boolean isAborted(TransactionId xid) {
        return getStatus(xid) == TransactionStatus.ABORTED;
    }
    public TransactionStatus getStatus(TransactionId xid) {
        return status.getOrDefault(assertNotNull(xid), TransactionStatus.INVALID);
    }
}
