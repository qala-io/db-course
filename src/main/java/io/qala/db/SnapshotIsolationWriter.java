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
//        Tuple latest = oldVersion.getLatestVersion(oldVersion);
        waitIfLocked(prev);
        // If another tx updated the tuple concurrently:
        // - Read Committed would simply re-evaluate where condition and continue updating
        //   the tuple if still satisfied
        // - Snapshot isolation on the other hand will err
        prev.lock(id);
        Tuple newVersion = new Tuple(id, data);
        newVersion.currentWriter = id;
        prev.nextVersion = newVersion;
        prev.xmax = id;
        return newVersion;
    }
    private void waitIfLocked(Tuple latest) {
        // wait until it's unlocked, need to use something else instead of spin lock
        while(latest.isWriteLockIsHeldByAnother(id));
    }
}
