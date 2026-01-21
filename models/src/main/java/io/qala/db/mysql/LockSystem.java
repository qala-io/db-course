package io.qala.db.mysql;

import io.qala.db.TxId;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Predicate;

/**
 * Contains all the locks within MySQL. If you need to check if lock exists or if you want to lock/unlock something -
 * this is the place.
 * <p>
 * For now methods are `synchronized` because for each record/range we need to keep a list of locks (there could be
 * more than one) and we need to ensure we don't add more than one exclusive lock to the list.
 *
 * @see Lock
 * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L55">Documetation on the Locking System</a>
 * @see <a href="https://dev.mysql.com/blog-archive/innodb-data-locking-part-4-scheduling">InnoDB Data Locking - Part 4 "Scheduling</a>
 * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L1020">lock_sys_t</a>
 */
class LockSystem {
    /**
     * Each record may have multiple locks - some are granted (we can have multiple shared locks granted), some are
     * waiting. Each time a lock is released, we need to traverse the queue and choose which of the locks to grant
     * next. It's not a trivial task - if tx1 is blocked by tx0, and tx2 is blocked by tx1 - we have two transactions
     * blocked by tx0 (directly and transitively). The total number of transactions blocked is taken into account
     * when choosing which lock to grant.
     *
     * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0lock.h#L127">The scheduling algorithm</a>
     */
    private final ConcurrentHashMap<RecId, Queue<Lock>> recordLocks = new ConcurrentHashMap<>();

    /**
     * Executes a function for each lock on the record (there may be multiple), returns the lock that prevents us
     * from progressing (if any).
     *
     * @return lock when the callback returns false(!), meaning this lock is actually blocking us from doing something.
     *         Returns null otherwise.
     *
     * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/include/lock0priv.h#L1077">foreach()</a>
     */
    public synchronized Lock forEach(RecId recId, Predicate<Lock> f) {
        Queue<Lock> locks = recordLocks.getOrDefault(recId, newLockQueue());
        for (Lock lock : locks) {
            assert lock.isRecordLock();
            if(!f.test(lock))
                return lock;
        }
        return null;
    }

    /**
     * @see <a href="https://github.com/mysql/mysql-server/blob/91659ce3030a1630eabdded4e17b1ba5743c9473/storage/innobase/lock/lock0lock.cc#L1593">lock_rec_add_to_queue()</a>
     */
    public synchronized void lockRecord(RecId recId, Lock lock) {
        recordLocks.computeIfAbsent(recId, (o) -> newLockQueue()).add(lock);
    }

    public synchronized void releaseLock(RecId recId, TxId txId) {
        recordLocks.get(recId).remove();
    }

    private static LinkedBlockingDeque<Lock> newLockQueue() {
        return new LinkedBlockingDeque<>();
    }
}
