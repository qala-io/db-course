package io.qala.db;

import static io.qala.db.TxStatus.*;
import static io.qala.db.TxStatus.INVALID;

public class SnapshotIsolationReader implements TxReader {
    final TxId id;
    private final Snapshot snapshot;
    private final TxsStatus txsStatus;

    public SnapshotIsolationReader(TxId id, Snapshot snapshot, TxsStatus txsStatus) {
        this.id = id;
        this.snapshot = snapshot;
        this.txsStatus = txsStatus;
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
        TxId endTx = t.xmaxStatus == COMMITTED ? t.xmax : TxId.NULL;
        // the record has been deleted, but maybe it happened in parallel and our snapshot doesn't now yet
        if(t.xmaxStatus == COMMITTED && snapshot.isInSnapshot(endTx))
            return false;
        return true;
    }

    private TxStatus getBeginTxStatus(Tuple t) {
        if(t.xminStatus != INVALID)
            return t.xminStatus;
        return txsStatus.getStatus(t.xmin);
    }
    private TxStatus getEndTxStatus(Tuple t) {
        if(t.xmax == TxId.NULL)
            return ABORTED;
        if(t.xmaxStatus != INVALID)
            return t.xmaxStatus;
        return txsStatus.getStatus(t.xmax);
    }
}
