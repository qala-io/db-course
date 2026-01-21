package io.qala.db.pg;

import io.qala.db.TxId;
import org.junit.Test;

import java.util.Set;

import static io.qala.datagen.RandomShortApi.integer;
import static io.qala.db.TxId.xid;
import static org.junit.Assert.*;

public class SnapshotTest {
    @Test public void inSnapshot_ifXidIsBelowOrEqualXmin() {
        int xmin = integer();
        int xmax = integer(xmin, xmax(xmin));
        assertTrue(snapshot(xmin, xmax).isInSnapshot(xid(xmin)));
        assertTrue(snapshot(xmin, xmax).isInSnapshot(xid(xmin - 1)));
        assertTrue(snapshot(xmin, xmax).isInSnapshot(xid(integer(Integer.MIN_VALUE, xmin))));
    }
    @Test public void notInSnapshot_ifXidIsGreaterThanOrEqualsXmax() {
        int xmin = integer();
        int xmax = integer(xmin, xmax(xmin));
        assertFalse(snapshot(xmin, xmax).isInSnapshot(xid(xmax)));
        assertFalse(snapshot(xmin, xmax).isInSnapshot(xid(xmax + 1)));
    }
    @Test public void inSnapshot_ifXidBetweenXminAndXmax_andIsNotPresentInActiveTransactions() {
        int xmin = integer();
        int xmax = integer(xmin, xmax(xmin+1));
        assertTrue(snapshot(xmin, xmin).isInSnapshot(xid(xmin)));//lower boundary
        assertTrue(snapshot(xmin, xmax).isInSnapshot(xid(xmax-1)));//upper boundary + activeTxs
        assertTrue(snapshot(xmin, xmax).isInSnapshot(xid(integer(xmin+1, xmax-1))));
    }
    @Test public void notInSnapshot_ifXidBetweenXminAndXmax_andIsPresentInActiveTransactions() {
        int xmin = integer();
        int xmax = integer(xmin, xmax(xmin+1));

        TxId xid = xid(xmin + 1);//lower boundary
        assertFalse(snapshot(xmin, xmax, xid).isInSnapshot(xid));

        xid = xid(xmax - 1);//upper boundary
        assertFalse(snapshot(xmin, xmax, xid).isInSnapshot(xid));

        xid = xid(integer(xmin, xmax - 1));
        assertFalse(snapshot(xmin, xmax, xid).isInSnapshot(xid));
    }
    @Test public void cannotCheckNullXidInSnapshot_itErrs() {
        int xmin = integer();
        int xmax = integer(xmin, xmax(xmin+1));
        assertThrows(IllegalArgumentException.class, () -> snapshot(xmin, xmax).isInSnapshot(null));
    }

    public static Snapshot snapshot() {
        int xmin = integer();
        return snapshot(xmin, xmax(xmin));
    }
    public static Snapshot snapshot(int xmin, int xmax, TxId... active) {
        return new Snapshot(xid(xmin), xid(xmax), Set.of(active));
    }
    public static Snapshot snapshot(TxId xmin, TxId xmax, TxId... active) {
        return new Snapshot(xmin, xmax, Set.of(active));
    }

    private static int xmax(int xmin) {
        //at some point we need to implement tx overflow
        return integer(xmin, Integer.MAX_VALUE);
    }
}