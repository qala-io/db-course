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

    public Tuple write(Tuple oldVersion, Object[] data) {
        Tuple prev = oldVersion == null// when it's INSERT, not UPDATE
                ? new Tuple(id, null)//just to eliminate all the null checks, it will be GCed quickly
                : oldVersion;
        Tuple newVersion = new Tuple(id, data);
        newVersion.currentWriter = id;
        prev.nextVersion = newVersion;
        prev.xmax = id;
        return newVersion;
    }
}
