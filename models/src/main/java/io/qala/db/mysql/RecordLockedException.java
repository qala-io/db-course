package io.qala.db.mysql;

/**
 * Thrown when record has a conflicting lock.
 */
class RecordLockedException extends Exception {
    private final RecId recId;
    private final Lock lock;

    RecordLockedException(RecId recId, Lock lock) {
        super("Record " + recId + " is locked: " + lock);
        this.recId = recId;
        this.lock = lock;
    }
}
