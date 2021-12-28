package io.qala.db;

import java.util.ArrayList;
import java.util.List;

public class Session {
    private final Db db;
    private Transaction tx;

    public Session(Db db) {
        this.db = db;
    }

    public void beginTx() {
        TransactionId txId = new TransactionId((int) System.nanoTime());
        tx = new Transaction(txId, db.createSnapshot(), transactions);
        db.addActiveTx(tx);
    }
    public void commit() {
        db.commit(tx);
        tx = null;
    }

    public void insert(String rel, Tuple t) {
        t.beginTx = tx.id;
        db.insert(rel, t);
    }
    public List<Tuple> select(String rel) {
        List<Tuple> resultSet = new ArrayList<>();
        for (Tuple tuple : db.select(rel))
            if(tx.read(tuple))
                resultSet.add(tuple);
        return resultSet;
    }

}
