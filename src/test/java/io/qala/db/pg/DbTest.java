package io.qala.db.pg;

import org.junit.Test;

import static io.qala.datagen.RandomShortApi.callNoneOrMore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DbTest {
    @Test public void creatingTx_updatesLastStarted() {
        Db db = new Db();
        callNoneOrMore(() -> db.beginTx(TxIsolationLevel.SNAPSHOT));
        Tx tx = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertEquals(tx.id, db.getLastStarted());
    }
    @Test public void committingTx_updatesSmallestFinished() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.SNAPSHOT);

        callNoneOrMore(() -> db.beginTx(TxIsolationLevel.SNAPSHOT));
        db.commit(tx.id);
        assertEquals(tx.id, db.getSmallestFinished());
    }
    @Test public void committingTx_doesNotUpdateSmallestFinished_ifSmallerTxIsActive() {
        Db db = new Db();
        db.beginTx(TxIsolationLevel.SNAPSHOT);
        Tx tx2 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        db.commit(tx2.id);
        assertEquals(new TxId(0), db.getSmallestFinished());
    }
    @Test public void startingTx_addsItToListOfActive(){
        Db db = new Db();
        assertTrue(db.createSnapshot().activeTxs.isEmpty());

        Tx tx1 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertTrue(db.createSnapshot().activeTxs.contains(tx1.id));
        Tx tx2 = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertTrue(db.createSnapshot().activeTxs.contains(tx2.id));
        assertEquals(2, db.createSnapshot().activeTxs.size());
    }
    @Test public void committingTx_removesItFromListOfActive() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.SNAPSHOT);
        db.commit(tx.id);
        assertEquals(0, db.createSnapshot().activeTxs.size());
    }
    @Test public void committingTx_changesItsStatus() {
        Db db = new Db();
        Tx tx = db.beginTx(TxIsolationLevel.SNAPSHOT);
        assertEquals(TxOutcome.UNKNOWN, db.txsOutcomes.getOutcome(tx.id));

        db.commit(tx.id);
        assertEquals(TxOutcome.COMMITTED, db.txsOutcomes.getOutcome(tx.id));
    }
}