package io.qala.db;

public class SnapshotIsolationWriter implements TxWriter {
    final TxId id;
    private final Snapshot snapshot;
    private final TxsStatus txsStatus;

    public SnapshotIsolationWriter(TxId id, Snapshot snapshot, TxsStatus txsStatus) {
        this.id = id;
        this.snapshot = snapshot;
        this.txsStatus = txsStatus;
    }

    public Tuple write(Tuple oldVersion, Object[] data) throws ConcurrentUpdateException {
        Tuple prev = oldVersion == null// when it's INSERT, not UPDATE
                ? new Tuple(id, null)//just to eliminate all the null checks, it will be GCed quickly
                : oldVersion;
        txsStatus.updateXmaxStatus(prev);
        if(prev.xmaxStatus == TxStatus.COMMITTED && !snapshot.isInSnapshot(prev.xmax))
            throw new ConcurrentUpdateException();
        Tuple newVersion = new Tuple(id, data);
        newVersion.currentWriter = id;
        prev.nextVersion = newVersion;
        prev.xmax = id;
        prev.xmaxStatus = TxStatus.INVALID;
        return newVersion;
    }
}
