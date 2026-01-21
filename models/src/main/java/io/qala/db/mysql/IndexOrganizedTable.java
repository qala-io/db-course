package io.qala.db.mysql;

import io.qala.db.mysql.Lock.LockMode;

import java.util.Map;
import java.util.TreeMap;

/**
 * a.k.a. Clustered Index. All tables are index-organized in MySQL. There are no heap tables.
 */
class IndexOrganizedTable {
    /**
     * In reality the key isn't the ID itself, rather it's constructed from the physical location, similar to PG CTID.
     * So the key is concatenated {@code space_id + page_no + heap_no} (space_id is probably a tablespace id?).
     * <p>
     * <a href="https://dev.mysql.com/blog-archive/innodb-data-locking-part-2-5-locks-deeper-dive/">Regarding the num_within_page</a>:
     * <i>
     * these numbers do not in general have to be in the same order as record values on the page as they are
     * assigned by a small heap allocator which tries to reuse space within the page the best in can when you remove,
     * insert, and resize the row
     * </i>
     */
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
