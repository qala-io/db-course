package io.qala.db.pg;

public interface TxWriter {
    Tuple write(Tuple oldVersion, Object[] data) throws ConcurrentUpdateException;
}
