package io.qala.db;

import static io.qala.db.TxOutcome.ABORTED;
import static io.qala.db.TxOutcome.UNKNOWN;

/**
 * aka Table Record, Row
 *
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/access/htup.h#L62
 */
public class Tuple {
    final Object[] data;
    final TxId xmin;
    volatile TxId xmax;
    volatile TxOutcome xminStatus = UNKNOWN, xmaxStatus = ABORTED;
    volatile Tuple nextVersion;

    public Tuple(TxId creator, Object[] data) {
        this.data = data;
        this.xmin = creator;
    }
}
