package io.qala.db.pg;

public interface TxReader {
    boolean canRead(Tuple t);
}
