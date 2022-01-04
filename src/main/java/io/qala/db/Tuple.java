package io.qala.db;

import static io.qala.db.TxStatus.ABORTED;
import static io.qala.db.TxStatus.INVALID;

/**
 * aka Table Record, Row
 *
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/access/htup.h#L62
 */
public class Tuple {
    final Object[] data;
    TxId xmin, xmax = TxId.NULL;
    TxStatus xminStatus = INVALID, xmaxStatus = ABORTED;
    volatile TxId currentWriter;
    Tuple nextVersion;

    public Tuple(TxId creator, Object[] data) {
        this.data = data;
        this.xmin = creator;
    }

    public void lock(TxId tx) {
        this.currentWriter = tx;
    }
    public boolean isWriteLockIsHeldByAnother(TxId currentTx) {
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
