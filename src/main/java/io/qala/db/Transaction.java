package io.qala.db;

import java.util.HashSet;
import java.util.Set;

import static io.qala.db.TransactionStatus.COMMITTED;
import static io.qala.db.TransactionStatus.INVALID;

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
        readTuples.add(t);
        t.readTx = id;
        return true;
    }
    private TransactionStatus getBeginTxStatus(Tuple t) {
        if(t.xminStatus != INVALID)
            return t.xminStatus;
        return transactions.getStatus(t.xmin);
    }
    private TransactionStatus getEndTxStatus(Tuple t) {
        if(t.xmax == TransactionId.NULL)
            return null;
        if(t.xmaxStatus != INVALID)
            return t.xmaxStatus;
        return transactions.getStatus(t.xmax);
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
