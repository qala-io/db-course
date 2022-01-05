package io.qala.db;

public class Tx {
    final TxId id;
    private final TxReader reader;
    private final TxWriter writer;

    public Tx(TxId id, TxReader txReader, TxWriter txWriter) {
        this.id = id;
        this.reader = txReader;
        this.writer = txWriter;
    }

    public boolean canRead(Tuple t) {
        return reader.canRead(t);
    }

    public Tuple write(Tuple oldVersion, Object[] data) throws ConcurrentUpdateException {
        return writer.write(oldVersion, data);
    }
}
