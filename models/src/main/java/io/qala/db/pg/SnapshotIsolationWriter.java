package io.qala.db.pg;

import io.qala.db.TxId;

public class SnapshotIsolationWriter implements TxWriter {
    final TxId id;
    private final Snapshot snapshot;
    private final TxsOutcomes txsOutcomes;

    public SnapshotIsolationWriter(TxId id, Snapshot snapshot, TxsOutcomes txsOutcomes) {
        this.id = id;
        this.snapshot = snapshot;
        this.txsOutcomes = txsOutcomes;
    }

    /**
     * @param oldVersion must be visible to current TX, though there are edge cases
     *                   https://github.com/postgres/postgres/blob/650663b4cb4714a34d7171981de4392486a85f86/src/backend/executor/nodeModifyTable.c#L2024
     */
    public Tuple write(Tuple oldVersion, Object[] data) throws ConcurrentUpdateException {
        Tuple prev = oldVersion == null// when it's INSERT, not UPDATE
                ? new Tuple(id, null)//just to eliminate all the null checks, it will be GCed quickly
                : oldVersion;
        txsOutcomes.updateXmaxStatus(prev, true);
        if(prev.xmaxStatus == TxOutcome.COMMITTED && !snapshot.isInSnapshot(prev.xmax))
            throw new ConcurrentUpdateException();
        Tuple newVersion = new Tuple(id, data);
        prev.nextVersion = newVersion;
        prev.xmax = id;
        prev.xmaxStatus = TxOutcome.UNKNOWN;
        return newVersion;
    }
}
