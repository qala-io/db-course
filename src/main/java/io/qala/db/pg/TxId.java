package io.qala.db.pg;

import java.util.Objects;

class TxId implements Comparable<TxId> {
    private final Integer id;

    public TxId(int id) {
        this.id = id;
    }
    public static TxId xid(int transactionId) {
        return new TxId(transactionId);
    }
    public static TxId assertNotNull(TxId xid) {
        if(xid == null)
            throw new IllegalArgumentException(
                    "Check Transaction ID for null value explicitly before checking if it's in snapshot");
        return xid;
    }

    public boolean precedesOrEqual(TxId that) {
        return compareTo(that) <= 0;
    }
    public boolean followsOrEqual(TxId that) {
        return compareTo(that) >= 0;
    }
    public TxId add(int offset) {
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
    @Override public int compareTo(TxId o) {
        return Integer.compare(id, o.id);
    }
    @Override public String toString() {
        return "xid#" + id;
    }
}
