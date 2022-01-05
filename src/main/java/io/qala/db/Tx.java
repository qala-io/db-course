package io.qala.db;

public class Tx {
    final TxId id;
    private final TxReader reader;
    private final TxWriter writer;

    public Tx(TxId id, Snapshot snapshot, TxsStatus txsStatus) {
        this.id = id;
        this.reader = new SnapshotIsolationReader(id, snapshot, txsStatus);
        this.writer = new SnapshotIsolationWriter(id, snapshot, txsStatus);
    }

    public boolean canRead(Tuple t) {
        return reader.canRead(t);
    }

    public Tuple write(Tuple oldVersion, Object[] data) {
        return writer.write(oldVersion, data);
    }
}
