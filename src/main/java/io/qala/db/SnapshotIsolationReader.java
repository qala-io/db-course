package io.qala.db;

import static io.qala.db.TransactionStatus.*;
import static io.qala.db.TransactionStatus.INVALID;

public class SnapshotIsolationReader {
    final TransactionId id;
    private final Snapshot snapshot;
    private final Transactions transactions;

    public SnapshotIsolationReader(TransactionId id, Snapshot snapshot, Transactions transactions) {
        this.id = id;
        this.snapshot = snapshot;
        this.transactions = transactions;
    }

    public boolean canRead(Tuple t) {
        if(t.xmax.equals(id))
            return false;// we deleted this tuple
        if(t.xmin.equals(id))
            return true;// we created this tuple
        t.xminStatus = getBeginTxStatus(t);
        if(t.xminStatus != COMMITTED || !snapshot.isInSnapshot(t.xmin))
            return false;// inserting transaction hasn't been committed
        t.xmaxStatus = getEndTxStatus(t);
        TransactionId endTx = t.xmaxStatus == COMMITTED ? t.xmax : TransactionId.NULL;
        // the record has been deleted, but maybe it happened in parallel and our snapshot doesn't now yet
        if(t.xmaxStatus == COMMITTED && snapshot.isInSnapshot(endTx))
            return false;
        return true;
    }

    private TransactionStatus getBeginTxStatus(Tuple t) {
        if(t.xminStatus != INVALID)
            return t.xminStatus;
        return transactions.getStatus(t.xmin);
    }
    private TransactionStatus getEndTxStatus(Tuple t) {
        if(t.xmax == TransactionId.NULL)
            return ABORTED;
        if(t.xmaxStatus != INVALID)
            return t.xmaxStatus;
        return transactions.getStatus(t.xmax);
    }
}
