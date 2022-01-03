package io.qala.db;

import java.util.HashSet;
import java.util.Set;

public class Transaction {
    final TransactionId id;
    private final Set<Tuple> readTuples = new HashSet<>();
    private final Set<Tuple> writeTuples = new HashSet<>();
    private final SnapshotIsolationReader reader;
    private final TxWriter writer;

    public Transaction(TransactionId id, Snapshot snapshot, Transactions transactions) {
        this.id = id;
        this.reader = new SnapshotIsolationReader(id, snapshot, transactions);
        this.writer = new SnapshotIsolationWriter(id, snapshot, transactions);
    }

    public boolean canRead(Tuple t) {
        return reader.canRead(t);
    }

    public Tuple update(Tuple oldVersion, Object[] data) {
        return writer.write(oldVersion, data);
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
}
