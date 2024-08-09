package io.qala.db.mysql;

/**
 * <h2>Explicit vs Implicit</h1>
 * This is an explicit lock. Often, MySQL doesn't need them - the fact that Tuples reference the transactions that
 * created/modified then, if you know that this tx is still in progress, then you know that the Tuple is locked
 * <i>implicitly</i>.
 * <p>
 * But at some point if tx1 runs into such a lock, it can convert it from implicit to explicit
 * (by creating an object of this class). This allows us to monitor such explicit locks.
 *
 * <h2>Locks vs MVCC</h2>
 * Shared locks are used for to lock the readers when a Writer is currently working with a record.
 * This different from MVCC where readers are never blocked. MySQL supports both types of parallelism.
 *
 * <h2>Intention locks</h2>
 * It's possible in MySQL to lock the whole table. But what if someone is reading/writing rows in it? We can't lock
 * the whole table if someone is working with the rows, and we can't lock rows if the whole table is locked.
 * <p>
 * S/X locks on a table - is a lock on the whole table
 * Intention locks (IS/IX) - are "partial" locks on tables, basically asking for permission to work with the rows
 *
 * <h2>Record & Gap locks</h2>
 * MySQL can lock both a single record and a gap between records (or between a record and infinity). Each page of an
 * index contains a so-called "supremum pseudo-record" - the infinity.
 *
 * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L55>Documetation on the Locking System</a>
 * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0priv.h#L136">struct lock_t</a>
 * @see <a href="https://dev.mysql.com/blog-archive/innodb-data-locking-part-2-locks/">InnoDB Data Locking - Part 2 "Locks"</a>
 */
class Lock {
    final Tx tx;
    private final LockTarget target;
    private final LockMode mode;
    private volatile LockState state;

    public Lock(Tx tx, LockTarget target, LockMode mode) {
        this.tx = tx;
        this.target = target;
        this.mode = mode;
    }

    public boolean isRecordLock() {
        return target == LockTarget.RECORD;
    }

    /**
     * In MySQL it's actually an int, and there are masks to check the type or other info like {@link LockMode}.
     * <p>
     * mask used to extract lock type from the type_mode field in a lock
     * <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L970">uint32_t LOCK_TYPE_MASK = 0xF0UL</a>
     *
     * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0priv.h#L170">uint32_t type_mode</a>
     */
    enum LockTarget {
        /**
         * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L968">uint32_t LOCK_REC = 32;</a>
         */
        RECORD,
        GAP, RECORD_AND_GAP, INSERT_INTENTION
    }

    enum LockMode {
        EXCLUSIVE, SHARED
    }
    enum LockState {
        GRANTED, WAITING
    }
}
