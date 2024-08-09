package io.qala.db.mysql;

/**
 * The key for the clustered index. All the data must be stored in an index-organized table (Clustered Index)
 * in MySQL. There must be such a key, and if you didn't define it explicitly, it's generated automatically.
 */
class RecId<T extends Comparable<T>> implements Comparable<RecId<T>> {
    private final T value;

    RecId(Comparable<?> value) {
        this.value = (T) value;
    }
    static <T extends Comparable<T>> RecId<T> create(T o) {
        return new RecId<>(o);
    }

    @Override
    public int compareTo(RecId<T> o) {
        return this.value.compareTo(o.value);
    }
}
