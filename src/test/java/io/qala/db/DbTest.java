package io.qala.db;

import io.qala.datagen.RandomShortApi;
import org.junit.Test;

import static io.qala.datagen.RandomShortApi.callNoneOrMore;
import static org.junit.Assert.*;

public class DbTest {
    @Test public void creatingTx_updatesLastStarted() {
        Db db = new Db();
        callNoneOrMore(db::beginTx);
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
    @Test public void startingTx_addsItToListOfActive(){
        Db db = new Db();
        assertTrue(db.activeTxs.isEmpty());

        Tx tx1 = db.beginTx();
        assertTrue(db.activeTxs.contains(tx1.id));
        Tx tx2 = db.beginTx();
        assertTrue(db.activeTxs.contains(tx2.id));
        assertEquals(2, db.activeTxs.size());
    }
    @Test public void committingTx_removesItFromListOfActive() {
        Db db = new Db();
        Tx tx = db.beginTx();
        db.commit(tx.id);
        assertEquals(0, db.activeTxs.size());
    }
}