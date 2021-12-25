package io.qala.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Aka Table.
 */
public class Relation {
    private final List<Tuple> tuples = new ArrayList<>();

    public void insert(Tuple t) {
        tuples.add(t);
    }
    public Tuple select(int rowid) {
        return tuples.get(rowid);
    }
    public List<Tuple> select() {
        return tuples;
    }
}
