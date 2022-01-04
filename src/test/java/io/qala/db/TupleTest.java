package io.qala.db;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TxId.NULL;
import static io.qala.db.TxId.xid;
import static io.qala.db.TxStatus.COMMITTED;
import static io.qala.db.TxStatus.INVALID;

public class TupleTest {
    public static Tuple inserted() {
        return inserted(xid(integer()));
    }
    public static Tuple inserted(TxId xmin) {
        return tuple(xmin, NULL, COMMITTED, INVALID);
    }
    public static Tuple deleted() {
        return deleted(integer());
    }
    public static Tuple deleted(int xmax) {
        return tuple(xid(integer(Integer.MIN_VALUE, xmax)), xid(xmax), COMMITTED, COMMITTED);
    }
    public static Tuple tuple(TxId xmin, TxId xmax,
                              TxStatus xminStatus, TxStatus xmaxStatus) {
        Tuple t = new Tuple(xmin, null);
        t.xmax = xmax;
        t.xminStatus = xminStatus;
        t.xmaxStatus = xmaxStatus;
        return t;
    }
}