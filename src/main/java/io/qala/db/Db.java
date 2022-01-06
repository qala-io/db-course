package io.qala.db;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Db {
    private final AtomicInteger nextXid = new AtomicInteger(1);
    private volatile TxId smallestFinished = new TxId(0), lastStarted = new TxId(0);
    private final NavigableSet<TxId> activeTxs = new ConcurrentSkipListSet<>();
    final TxsOutcomes txsOutcomes = new TxsOutcomes();

    public Tx beginTx(TxIsolationLevel isolation) {
        TxId xid = new TxId(nextXid.getAndIncrement());
        Snapshot snapshot = createSnapshot();
        this.lastStarted = xid;
        this.activeTxs.add(xid);
        return createTx(xid, isolation, snapshot);
    }

    public void abort(TxId xid) {
        finishTx(xid, TxOutcome.ABORTED);
    }
    public void commit(TxId xid) {
        finishTx(xid, TxOutcome.COMMITTED);
    }
    private void finishTx(TxId xid, TxOutcome outcome) {
        txsOutcomes.setStatus(xid, outcome);
        activeTxs.remove(xid);
        if(activeTxs.floor(xid) == null)
            smallestFinished = xid;
    }
    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/replication/logical/snapbuild.c#L613
     */
    Snapshot createSnapshot() {
        return new Snapshot(smallestFinished, lastStarted, activeTxs);
    }


    TxId getLastStarted() {
        return lastStarted;
    }
    TxId getSmallestFinished() {
        return smallestFinished;
    }

    private Tx createTx(TxId xid, TxIsolationLevel isolation, Snapshot snapshot) {
        TxReader txReader;
        TxWriter txWriter;
        if(isolation == TxIsolationLevel.SNAPSHOT) {
            txReader = new SnapshotIsolationReader(xid, snapshot, txsOutcomes);
            txWriter = new SnapshotIsolationWriter(xid, snapshot, txsOutcomes);
        } else
            throw new IllegalArgumentException("TX Isolation level isn't supported: " + isolation);
        return new Tx(xid, txReader, txWriter);
    }
}
