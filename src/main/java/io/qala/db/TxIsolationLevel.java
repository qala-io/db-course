package io.qala.db;

/**
 * All possible isolation levels, different databases implement different ones.
 */
public enum TxIsolationLevel {
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE,
    /**
     * While PG calls its levels {@link #REPEATABLE_READ}, {@link #SERIALIZABLE}, strictly speaking these are
     * different levels. Keeping them separately for now, not sure if it makes sense to unify the names.
     */
    SNAPSHOT,
    SNAPSHOT_SERIALIZABLE
}
