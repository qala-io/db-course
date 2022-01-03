package io.qala.db;

import static io.qala.db.TransactionStatus.ABORTED;
import static io.qala.db.TransactionStatus.INVALID;

/**
 * aka Table Record, Row
 *
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/access/htup.h#L62
 */
public class Tuple {
    final Object[] data;
    TransactionId xmin, xmax = TransactionId.NULL;
    TransactionStatus xminStatus = INVALID, xmaxStatus = ABORTED;
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
        Tuple nextVersion = oldVersion;
        while(nextVersion.nextVersion != null)
            nextVersion = nextVersion.nextVersion;
        return nextVersion;
    }
}
