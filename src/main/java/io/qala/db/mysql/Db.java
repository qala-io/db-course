package io.qala.db.mysql;

import io.qala.db.TxId;
import io.qala.db.TxIsolationLevel;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

class Db {
    private final AtomicInteger nextXid = new AtomicInteger(1);
    private volatile TxId smallestFinished = new TxId(0), lastStarted = new TxId(0);
    private final NavigableSet<TxId> activeTxs = new ConcurrentSkipListSet<>();
    private final InnoDb storage = new InnoDb();

    public Tx beginTx(TxIsolationLevel isolation) {
        TxId xid = new TxId(nextXid.getAndIncrement());
        this.lastStarted = xid;
        this.activeTxs.add(xid);
        return createTx(xid);
    }

    private Tx createTx(TxId xid) {
        return new Tx(xid, storage);
    }
}
