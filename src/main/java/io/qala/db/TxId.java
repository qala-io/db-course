package io.qala.db;

import java.util.Objects;

class TxId {
    private final Integer id;
    public static final TxId NULL = new TxId();

    private TxId() {
        this.id = null;
    }
    public TxId(int id) {
        this.id = id;
    }
    public static TxId xid(int transactionId) {
        return new TxId(transactionId);
    }
    public static TxId assertNotNull(TxId xid) {
        if(xid == null || xid.equals(NULL))
            throw new IllegalArgumentException(
                    "Check Transaction ID for null value explicitly before checking if it's in snapshot");
        return xid;
    }

    public boolean between(TxId beginTx, TxId endTx) {
        return beginTx.id < id && (endTx == NULL || id < endTx.id);
    }
    public boolean precedes(TxId that) {
        return this.id < that.id;
    }
    public boolean followsOrEqual(TxId that) {
        return this.id >= that.id;
    }
    public TxId add(int offset) {
        //noinspection ConstantConditions
        return new TxId(id + offset);
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TxId that = (TxId) o;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() {
        return Objects.hash(id);
    }
    @Override public String toString() {
        return "xid#" + id;
    }
}
