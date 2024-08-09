package io.qala.db.pg;

import io.qala.db.TxId;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TxId.xid;
import static io.qala.db.pg.TxOutcome.COMMITTED;
import static io.qala.db.pg.TxOutcome.UNKNOWN;

public class TupleTest {
    public static Tuple inserted() {
        return inserted(xid(integer()));
    }
    public static Tuple inserted(TxId xmin) {
        return tuple(xmin, null, COMMITTED, UNKNOWN);
    }
    public static Tuple deleted() {
        return deleted(integer());
    }
    public static Tuple deleted(int xmax) {
        return tuple(xid(integer(Integer.MIN_VALUE, xmax)), xid(xmax), COMMITTED, COMMITTED);
    }
    public static Tuple deleted(TxId xmax) {
        return tuple(xmax.add(integer(Integer.MIN_VALUE, 0)), xmax, COMMITTED, COMMITTED);
    }
    public static Tuple tuple(TxId xmin, TxId xmax,
                              TxOutcome xminStatus, TxOutcome xmaxStatus) {
        Tuple t = new Tuple(xmin, null);
        t.xmax = xmax;
        t.xminStatus = xminStatus;
        t.xmaxStatus = xmaxStatus;
        return t;
    }
}