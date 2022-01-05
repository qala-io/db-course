package io.qala.db;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Db {
    private final AtomicInteger nextXid = new AtomicInteger(1);
    private volatile TxId smallestFinished = new TxId(0), lastStarted = new TxId(0);
    final NavigableSet<TxId> activeTxs = new ConcurrentSkipListSet<>();
    private final TxsStatus txsStatus = new TxsStatus();

    public void commit(TxId xid) {
        activeTxs.remove(xid);
        if(activeTxs.floor(xid) == null)
            smallestFinished = xid;
    }
    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/replication/logical/snapbuild.c#L613
     */
    public Snapshot createSnapshot() {
        return new Snapshot(smallestFinished, lastStarted, activeTxs);
    }

    public Tx beginTx() {
        TxId xid = new TxId(nextXid.getAndIncrement());
        Snapshot snapshot = createSnapshot();
        this.lastStarted = xid;
        this.activeTxs.add(xid);
        return new Tx(xid, snapshot, txsStatus);
    }

    TxId getLastStarted() {
        return lastStarted;
    }
    TxId getSmallestFinished() {
        return smallestFinished;
    }
}
