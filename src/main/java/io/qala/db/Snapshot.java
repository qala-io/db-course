package io.qala.db;

import java.util.HashSet;
import java.util.Set;

/**
 * https://github.com/postgres/postgres/blob/ca3b37487be333a1d241dab1bbdd17a211a88f43/src/include/utils/snapshot.h#L142
 */
class Snapshot {
    final TransactionId xmax, xmin;
    final Set<TransactionId> inProgress;

    Snapshot(TransactionId xmax, TransactionId xmin, Set<TransactionId> inProgress) {
        this.xmax = xmax;
        this.xmin = xmin;
        this.inProgress = new HashSet<>(inProgress);
    }

    /**
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/access/heap/heapam_visibility.c#L959
     */
    public boolean isVisible(Tuple t) {
        return isInSnapshot(t.beginTx) && !isInSnapshot(t.endTx);
    }

    /**
     * For some reason in Postgres "in snapshot" means the opposite of this function (PG returns true if TX
     * is in progress), didn't get why:
     * https://github.com/postgres/postgres/blob/def5b065ff22a16a80084587613599fe15627213/src/backend/utils/time/snapmgr.c#L2242
     */
    private boolean isInSnapshot(TransactionId tx) {
        if(tx.precedes(xmin))
            return true;
        if(tx.followsOrEqual(xmax))
            return false;
        // if it's xmin < tx < xmax, then check if it was running at the time of creating snapshot:
        return !inProgress.contains(tx);
    }
}
