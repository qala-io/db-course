package io.qala.db;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Transactions {
    private final ConcurrentMap<TransactionId, TransactionStatus> status = new ConcurrentHashMap<>();

    public void commit(TransactionId txId) {
        this.status.put(txId, TransactionStatus.COMMITTED);
    }
    public void abort(TransactionId txId) {
        this.status.put(txId, TransactionStatus.ABORTED);
    }

    public boolean isCommitted(TransactionId txId) {
        return status.get(txId) == TransactionStatus.COMMITTED;
    }
    public boolean isAborted(TransactionId txId) {
        return status.get(txId) == TransactionStatus.ABORTED;
    }
    public TransactionStatus getStatus(TransactionId txId) {
        return status.get(txId);
    }
}
