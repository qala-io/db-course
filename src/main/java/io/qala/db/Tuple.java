package io.qala.db;

import static io.qala.db.TransactionStatus.INVALID;

/**
 * aka Table Record, Row
 *
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/access/htup.h#L62
 */
public class Tuple {
    final Object[] data;
    TransactionId xmin, xmax = TransactionId.NULL, readTx;
    TransactionStatus xminStatus = INVALID, xmaxStatus = INVALID;
    volatile TransactionId currentWriter;
    Tuple nextVersion;

    public Tuple(Object[] data) {
        this.data = data;
    }

    public void lock(TransactionId tx) {
        this.currentWriter = tx;
    }
    public boolean isWriteLockIsHeldByAnother(TransactionId currentTx) {
        return this.currentWriter != null && this.currentWriter.equals(currentTx);
    }
    public void unlock() {
        this.currentWriter = null;
    }
    public Tuple getLatestVersion(Tuple oldVersion) {
        while(oldVersion.nextVersion != null)
            oldVersion = oldVersion.nextVersion;
        return oldVersion;
    }
}
