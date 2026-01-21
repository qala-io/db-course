package io.qala.db.pg;

import io.qala.datagen.RandomShortApi;

public enum TxOutcome {
    COMMITTED, ABORTED,
    /**
     * Often it's the initial state of tuple's xmin/xmax - meaning it wasn't set yet even tx ended.
     */
    UNKNOWN;

    public static TxOutcome random() {
        return RandomShortApi.sample(values());
    }
}
