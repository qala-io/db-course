package io.qala.db.mysql;

import io.qala.db.TxId;

/**
 * In MySQL they call it TRX. But to keep things consistent across our models let's have it as Tx.
 *
 * @see <a href="https://github.com/mysql/mysql-server/blob/b09619ebfedf26d155f24f46b580a743caf00d2b/storage/innobase/include/trx0trx.h#L684">struct trx_t</a>
 */
class Tx {
    final TxId id;
    private final InnoDb storage;

    Tx(TxId id, InnoDb storage) {
        this.id = id;
        this.storage = storage;
    }

    public Tuple selectForShareByClusteredIndex(Comparable<?> id) {
        RecId<?> recId = new RecId<>(id);
        return storage.selectForUpdate(this, recId, Lock.LockMode.SHARED);
    }

    public Tuple insert(Comparable<?> id, Object[] data) {
        RecId<?> recId = new RecId<>(id);
        return storage.insert(this, recId, data);
    }
}
