package io.qala.db;

import java.util.ArrayList;
import java.util.List;

public class Session {
    private final Db db;
    private Tx tx;

    public Session(Db db) {
        this.db = db;
    }

    public void beginTx() {
        TxId txId = new TxId((int) System.nanoTime());
        tx = new Tx(txId, db.createSnapshot(), db.getTransactionsStatus());
        db.addActiveTx(tx);
    }
    public void commit() {
        db.commit(tx);
        tx = null;
    }

    public void insert(String rel, Tuple t) {
        t.xmin = tx.id;
        db.insert(rel, t);
    }
    public List<Tuple> select(String rel) {
        List<Tuple> resultSet = new ArrayList<>();
        for (Tuple tuple : db.select(rel))
            if(tx.canRead(tuple))
                resultSet.add(tuple);
        return resultSet;
    }

}
