package io.qala.db.mysql;

import io.qala.db.TxId;

class Tuple {
    private final Object[] data;
    /**
     * DB_TRX_ID in MySQL
     */
    private final TxId txId;
    /**
     * DB_ROW_ID in MySQL
     */
    private final RecId recId;

    Tuple(Object[] data, TxId txId, RecId recId) {
        this.data = data;
        this.txId = txId;
        this.recId = recId;
    }
}
