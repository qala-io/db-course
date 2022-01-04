package io.qala.db;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TransactionId.NULL;
import static io.qala.db.TransactionId.xid;
import static io.qala.db.TransactionStatus.COMMITTED;
import static io.qala.db.TransactionStatus.INVALID;

public class TupleTest {
    public static Tuple inserted() {
        return inserted(xid(integer()));
    }
    public static Tuple inserted(TransactionId xmin) {
        return tuple(xmin, NULL, COMMITTED, INVALID);
    }
    public static Tuple deleted() {
        return deleted(integer());
    }
    public static Tuple deleted(int xmax) {
        return tuple(xid(integer(Integer.MIN_VALUE, xmax)), xid(xmax), COMMITTED, COMMITTED);
    }
    public static Tuple tuple(TransactionId xmin, TransactionId xmax,
                              TransactionStatus xminStatus, TransactionStatus xmaxStatus) {
        Tuple t = new Tuple(xmin, null);
        t.xmax = xmax;
        t.xminStatus = xminStatus;
        t.xmaxStatus = xmaxStatus;
        return t;
    }
}