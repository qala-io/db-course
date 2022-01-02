package io.qala.db;

import io.qala.datagen.RandomShortApi;

public enum TransactionStatus {
    COMMITTED, ABORTED,
    /**
     * Often it's the initial state of tuple's xmin/xmax - meaning it wasn't set yet even tx ended.
     */
    INVALID;

    public static TransactionStatus random() {
        return RandomShortApi.sample(values());
    }
}
