package io.qala.db.mysql;

import io.qala.db.mysql.Lock.LockMode;

import java.util.Map;
import java.util.TreeMap;

/**
 * a.k.a. Clustered Index. All tables are index-organized in MySQL. There are no heap tables.
 */
class IndexOrganizedTable {
    private final Map<RecId<?>, Tuple> tuples = new TreeMap<>();
    private final LockSystem lockSystem;

    IndexOrganizedTable(LockSystem lockSystem) {
        this.lockSystem = lockSystem;
    }

    public Tuple readAndCheckLock(Tx tx, RecId recId, LockMode lockMode) throws RecordLockedException {
        Lock l = lockSystem.forEach(recId, (lock) -> {
            if (lock.isRecordLock() && !lock.tx.id.equals(tx.id))
                return false;
            return true;
        });
        if(l != null)
            throw new RecordLockedException(recId, l);
        // the lock will be held until the tx end
        lockSystem.lockRecord(recId, new Lock(tx, Lock.LockTarget.RECORD, lockMode));
        return tuples.get(recId);
    }
    public Tuple insert(Tx tx, RecId<?> recId, Object[] data) {
        lockSystem.lockRecord(recId, new Lock(tx, Lock.LockTarget.RECORD, LockMode.EXCLUSIVE));
        Tuple t = new Tuple(data, tx.id, recId);
        tuples.put(recId, t);
        return t;
    }
}
