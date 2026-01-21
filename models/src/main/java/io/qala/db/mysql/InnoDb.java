package io.qala.db.mysql;

class InnoDb {
    private final LockSystem lockSystem = new LockSystem();
    private final IndexOrganizedTable indexOrganizedTable = new IndexOrganizedTable(lockSystem);

    public Tuple selectForUpdate(Tx tx, RecId<?> recId, Lock.LockMode lockMode) {
        try {
            return indexOrganizedTable.readAndCheckLock(tx, recId, lockMode);
        } catch (RecordLockedException e) {
            throw new RuntimeException(e);
        }
    }

    public Tuple insert(Tx tx, RecId<?> recId, Object[] data) {
        return indexOrganizedTable.insert(tx, recId, data);
    }
}
