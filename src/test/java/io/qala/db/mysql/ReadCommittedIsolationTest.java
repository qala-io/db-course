package io.qala.db.mysql;

import io.qala.db.TxIsolationLevel;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ReadCommittedIsolationTest {
    @Test
    public void selectForShare_returnsNoRecordsIfDbIsEmpty() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        assertNull(tx.selectForShareByClusteredIndex("a"));
    }
    @Test
    public void selectForShare_returnsInsertedRecord() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        Tuple tuple = tx.insert("id", new Object[]{"id", "value"});
        assertSame(tuple, tx.selectForShareByClusteredIndex("id"));
    }
}
