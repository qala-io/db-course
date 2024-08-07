package io.qala.db.pg;

import static io.qala.db.pg.TxOutcome.COMMITTED;

public class SnapshotIsolationReader implements TxReader {
    final TxId id;
    private final Snapshot snapshot;
    private final TxsOutcomes txsOutcomes;

    public SnapshotIsolationReader(TxId id, Snapshot snapshot, TxsOutcomes txsOutcomes) {
        this.id = id;
        this.snapshot = snapshot;
        this.txsOutcomes = txsOutcomes;
    }

    public boolean canRead(Tuple t) {
        if(id.equals(t.xmax))
            return false;// we deleted this tuple
        if(t.xmin.equals(id))
            return true;// we created this tuple
        txsOutcomes.updateXminStatus(t);
        if(t.xminStatus != COMMITTED || !snapshot.isInSnapshot(t.xmin))
            return false;// inserting transaction hasn't been committed
        txsOutcomes.updateXmaxStatus(t, false);
        TxId endTx = t.xmaxStatus == COMMITTED ? t.xmax : null;
        // the record has been deleted, but maybe it happened in parallel and our snapshot doesn't now yet
        return t.xmaxStatus != COMMITTED || !snapshot.isInSnapshot(endTx);
    }

}
