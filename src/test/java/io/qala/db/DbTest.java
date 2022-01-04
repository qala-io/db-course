package io.qala.db;

import io.qala.datagen.RandomShortApi;
import org.junit.Test;

import static io.qala.datagen.RandomShortApi.callNoneOrMore;
import static org.junit.Assert.*;

public class DbTest {
    @Test public void creatingTx_updatesLastStarted() {
        Db db = new Db();
        Tx tx = db.beginTx();
        assertEquals(tx.id, db.getLastStarted());
    }
    @Test public void committingTx_updatesSmallestFinished() {
        Db db = new Db();
        Tx tx = db.beginTx();

        callNoneOrMore(db::beginTx);
        db.commit(tx.id);
        assertEquals(tx.id, db.getSmallestFinished());
    }
    @Test public void committingTx_doesNotUpdateSmallestFinished_ifSmallerTxIsActive() {
        Db db = new Db();
        db.beginTx();
        Tx tx2 = db.beginTx();
        db.commit(tx2.id);
        assertEquals(new TxId(0), db.getSmallestFinished());
    }
}