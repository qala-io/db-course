package io.qala.db;

import java.util.HashSet;
import java.util.Set;

import static io.qala.db.TransactionStatus.COMMITTED;

public class Transaction {
    final TransactionId id;
    private final Snapshot snapshot;
    private final Set<Tuple> readTuples = new HashSet<>();
    private final Set<Tuple> writeTuples = new HashSet<>();
    private final Transactions transactions;

    public Transaction(TransactionId id, Snapshot snapshot, Transactions transactions) {
        this.id = id;
        this.snapshot = snapshot;
        this.transactions = transactions;
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
        if(t.endTx.equals(id))
            return false;// we deleted this tuple
        if(t.beginTx.equals(id))
            return true;// we created this tuple
        t.beginTxStatus = getBeginTxStatus(t);
        if(t.beginTxStatus != COMMITTED || !snapshot.isInSnapshot(t.beginTx))
            return false;// inserting transaction hasn't been committed
        t.endTxStatus = getEndTxStatus(t);
        TransactionId endTx = t.endTxStatus == COMMITTED ? t.endTx : TransactionId.NULL;
        // the record has been deleted, but maybe it happened in parallel and our snapshot doesn't now yet
        if(t.endTxStatus == COMMITTED && snapshot.isInSnapshot(endTx))
            return false;
        readTuples.add(t);
        t.readTx = id;
        return true;
    }
    private TransactionStatus getBeginTxStatus(Tuple t) {
        if(t.beginTxStatus != null)
            return t.beginTxStatus;
        return transactions.getStatus(t.beginTx);
    }
    private TransactionStatus getEndTxStatus(Tuple t) {
        if(t.endTx == null)
            return null;
        if(t.endTxStatus != null)
            return t.endTxStatus;
        return transactions.getStatus(t.endTx);
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
