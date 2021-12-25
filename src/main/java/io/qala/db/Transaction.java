package io.qala.db;

import java.util.HashSet;
import java.util.Set;

public class Transaction {
    final TransactionId id;
    private final Snapshot snapshot;
    private final Set<Tuple> readTuples = new HashSet<>();
    private final Set<Tuple> writeTuples = new HashSet<>();

    public Transaction(TransactionId id, Snapshot snapshot) {
        this.id = id;
        this.snapshot = snapshot;
    }

    public void commit() {
        releaseLocks();
    }
    public void rollback() {
        releaseLocks();
    }
    private void releaseLocks() {
        for (Tuple t : writeTuples)
            t.unlock();
        writeTuples.clear();
        readTuples.clear();
    }

    public boolean read(Tuple t) {
        if(!snapshot.isVisible(t))
            return false;
        readTuples.add(t);
        t.readTx = id;
        return true;
    }
    public boolean update(Tuple oldVersion, Object[] data) {
        Tuple latest = oldVersion.getLatestVersion(oldVersion);
        waitIfLocked(latest);
        // If another tx updated the tuple concurrently:
        // - Read Committed would simply re-evaluate where condition and continue updating
        //   the tuple if still satisfied
        // - Snapshot isolation on the other hand will err
        oldVersion.lock(id);
        Tuple newVersion = new Tuple(data);
        newVersion.beginTx = newVersion.currentWriter = id;
        oldVersion.nextVersion = newVersion;
        oldVersion.endTx = id;
        return true;
    }
    private void waitIfLocked(Tuple latest) {
        // wait until it's unlocked, need to use something else instead of spin lock
        while(latest.isWriteLockIsHeldByAnother(id));
    }

    private boolean canRead(Tuple tuple) {
        if(tuple.currentWriter == id)
            return true;
        //todo: read committed should use current query ts instead of tx start ts
        return tuple.currentWriter == null && id.between(tuple.beginTx, tuple.endTx);
    }
}
