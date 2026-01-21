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
        Tuple t = tx.insert("id", new Object[]{"id", "value"});
        assertSame(t, tx.selectForShareByClusteredIndex("id"));
    }
    @Test
    public void selectForShare_returnsRecordIfCommittedByPreviousTx() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        Tuple t = tx.insert("id", new Object[]{"id", "value"});
        db.abort(tx.id);

        tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        assertNull(tx.selectForShareByClusteredIndex("id"));
    }
    @Test
    public void selectForShare_returnsNoRecord_ifPreviousTxAborted() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        Tuple t = tx.insert("id", new Object[]{"id", "value"});

        tx = db.beginTx(TxIsolationLevel.READ_COMMITTED);
        assertSame(t, tx.selectForShareByClusteredIndex("id"));
    }
}
