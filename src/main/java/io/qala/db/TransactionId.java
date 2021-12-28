package io.qala.db;

class TransactionId {
    private final int id;
    public static final TransactionId NULL = new TransactionId(0);

    public TransactionId(int id) {
        this.id = id;
    }

    public boolean between(TransactionId beginTx, TransactionId endTx) {
        return beginTx.id < id && (endTx == null || id < endTx.id);
    }
    public boolean precedes(TransactionId that) {
        return this.id < that.id;
    }
    public boolean followsOrEqual(TransactionId that) {
        return this.id >= that.id;
    }

}
