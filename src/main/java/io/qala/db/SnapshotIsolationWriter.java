package io.qala.db;

public class SnapshotIsolationWriter {
    final TransactionId id;
    private final Snapshot snapshot;
    private final Transactions transactions;

    public SnapshotIsolationWriter(TransactionId id, Snapshot snapshot, Transactions transactions) {
        this.id = id;
        this.snapshot = snapshot;
        this.transactions = transactions;
    }

    public boolean write(Tuple oldVersion, Object[] data) {
        Tuple latest = oldVersion.getLatestVersion(oldVersion);
        waitIfLocked(latest);
        // If another tx updated the tuple concurrently:
        // - Read Committed would simply re-evaluate where condition and continue updating
        //   the tuple if still satisfied
        // - Snapshot isolation on the other hand will err
        oldVersion.lock(id);
        Tuple newVersion = new Tuple(data);
        newVersion.xmin = newVersion.currentWriter = id;
        oldVersion.nextVersion = newVersion;
        oldVersion.xmax = id;
        return true;
    }
    private void waitIfLocked(Tuple latest) {
        // wait until it's unlocked, need to use something else instead of spin lock
        while(latest.isWriteLockIsHeldByAnother(id));
    }
}
