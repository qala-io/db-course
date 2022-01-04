package io.qala.db;

public interface TxReader {
    boolean canRead(Tuple t);
}
