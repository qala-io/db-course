package io.qala.db;

import io.qala.datagen.RandomShortApi;

public enum TxStatus {
    COMMITTED, ABORTED,
    /**
     * Often it's the initial state of tuple's xmin/xmax - meaning it wasn't set yet even tx ended.
     */
    INVALID;

    public static TxStatus random() {
        return RandomShortApi.sample(values());
    }
}
