package io.qala.db;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Db {
    private final AtomicInteger nextXid = new AtomicInteger(1);
    private final Set<TxId> activeTxs = new HashSet<>();
    private volatile TxId smallestFinished = new TxId(0), lastStarted = new TxId(0);
    private final TxsStatus txsStatus = new TxsStatus();

    public void commit(TxId xid) {
        activeTxs.remove(smallestFinished);
        boolean isSmallestCommitted = true;
        for (TxId active : activeTxs)
            if(active.precedes(xid)) {
                isSmallestCommitted = false;
                break;
            }
        if(isSmallestCommitted)
            smallestFinished = xid;
    }
    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/replication/logical/snapbuild.c#L613
     */
    public Snapshot createSnapshot() {
        return new Snapshot(smallestFinished, lastStarted, activeTxs);
    }
    public TxsStatus getTransactionsStatus() {
        return txsStatus;
    }

    public Tx beginTx() {
        TxId xid = new TxId(nextXid.getAndIncrement());
        Snapshot snapshot = createSnapshot();
        this.lastStarted = xid;
        this.activeTxs.add(xid);
        return new Tx(xid, snapshot, txsStatus);
    }

    public TxId getLastStarted() {
        return lastStarted;
    }
    public TxId getSmallestFinished() {
        return smallestFinished;
    }
}
