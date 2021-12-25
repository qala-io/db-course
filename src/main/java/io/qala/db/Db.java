package io.qala.db;

import java.util.*;

public class Db {
    private final Map<String, Relation> relations = new HashMap<>();
    private final Set<TransactionId> activeTxs = new HashSet<>();
    private volatile TransactionId lastCommittedTx, lastStartedTx;

    public Db(String relName, Relation rel) {
        this.relations.put(relName, rel);
    }

    public void insert(String rel, Tuple t) {
        relations.get(rel).insert(t);
    }
    public List<Tuple> select(String rel) {
        return relations.get(rel).select();
    }
    public void addActiveTx(Transaction tx) {
        lastStartedTx = tx.id;
        activeTxs.add(lastStartedTx);
    }
    public void commit(Transaction tx) {
        lastCommittedTx = tx.id;
        activeTxs.remove(lastCommittedTx);
    }
    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/replication/logical/snapbuild.c#L613
     */
    public Snapshot createSnapshot() {
        return new Snapshot(lastCommittedTx, lastStartedTx, activeTxs);
    }
}
