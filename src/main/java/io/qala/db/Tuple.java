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
    /**
     * Transaction that created the record
     */
    final TxId xmin;
    /**
     * Transaction that deleted the record. This happens when the transaction either issued a DELETE statement, or
     * an UPDATE (the previous version of the Tuple is marked as deleted, and the new one is created).
     *
     * <p>
     * Note, however, that it's possible that some TX set this value, but then the TX was aborted. It doesn't clear
     * this value. That's why we can't assume it's always correct - a new TX that accesses this record will have to
     * check the TX status and write it to {@link #xmaxStatus}.
     * <p/>
     */
    volatile TxId xmax;
    /**
     * Whether the TX that created this version of a tuple was committed or aborted. Originally the status is unknown
     * because the tuple is created... well, before the TX has committed or aborted. So it's the job of the <i>next</i>
     * TX to figure out its status and set this field.
     *
     * @see #xmin
     * @see TxsOutcomes#updateXminStatus(Tuple)
     */
    volatile TxOutcome xminStatus = UNKNOWN;
    /**
     * Similar to {@link #xminStatus}, but for the transaction that deleted the record.
     *
     * @see #xmax
     * @see TxsOutcomes#updateXmaxStatus(Tuple, boolean)
     */
    volatile TxOutcome xmaxStatus = ABORTED;
    volatile Tuple nextVersion;

    public Tuple(TxId creator, Object[] data) {
        this.data = data;
        this.xmin = creator;
    }
}
