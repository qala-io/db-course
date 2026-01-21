package io.qala.db.pg;

import io.qala.db.TxId;

import java.util.Set;

/**
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/utils/snapshot.h#L142
 */
class Snapshot {
    final TxId xmax, xmin;
    final Set<TxId> activeTxs;

    Snapshot(TxId xmin, TxId xmax, Set<TxId> activeTxs) {
        for (TxId active : activeTxs)
            assert xmin.precedesOrEqual(active) && xmax.followsOrEqual(active);
        this.xmax = xmax;
        this.xmin = xmin;
        this.activeTxs = Set.copyOf(activeTxs);
    }

    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/access/heap/heapam_visibility.c#L959
     */
    public boolean isVisible(Tuple t) {
        return isInSnapshot(t.xmin) && !isInSnapshot(t.xmax);
    }

    /**
     * For some reason in Postgres "in snapshot" means the opposite of this function (PG returns true if TX
     * is in progress), didn't get why:
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/utils/time/snapmgr.c#L2242
     */
    public boolean isInSnapshot(TxId xid) {
        TxId.assertNotNull(xid);
        if(xid.precedesOrEqual(xmin))
            return true;
        if(xid.followsOrEqual(xmax))
            return false;
        // if it's xmin < xid < xmax, then check if it was running at the time of creating snapshot:
        return !activeTxs.contains(xid);
    }
}
