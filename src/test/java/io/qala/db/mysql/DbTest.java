package io.qala.db.mysql;

import io.qala.db.TxIsolationLevel;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DbTest {
    @Test
    public void nextTxHasIdPlusOne() {
        Db db = new Db();
        Tx tx1 = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        Tx tx2 = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        assertTrue(tx1.id.precedes(tx2.id));
    }
}
