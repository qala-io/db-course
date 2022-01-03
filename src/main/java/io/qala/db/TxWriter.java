package io.qala.db;

public interface TxWriter {
    Tuple write(Tuple oldVersion, Object[] data);
}
