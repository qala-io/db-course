package io.qala.db;

import java.util.Objects;

class TransactionId {
    private final Integer id;
    public static final TransactionId NULL = new TransactionId();

    private TransactionId() {
        this.id = null;
    }
    public TransactionId(int id) {
        this.id = id;
    }
    public static TransactionId xid(int transactionId) {
        return new TransactionId(transactionId);
    }
    public static TransactionId assertNotNull(TransactionId xid) {
        if(xid == null || xid.equals(NULL))
            throw new IllegalArgumentException(
                    "Check Transaction ID for null value explicitly before checking if it's in snapshot");
        return xid;
    }

    public boolean between(TransactionId beginTx, TransactionId endTx) {
        return beginTx.id < id && (endTx == NULL || id < endTx.id);
    }
    public boolean precedes(TransactionId that) {
        return this.id < that.id;
    }
    public boolean followsOrEqual(TransactionId that) {
        return this.id >= that.id;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() {
        return Objects.hash(id);
    }
    @Override public String toString() {
        return "xid#" + id;
    }
}
