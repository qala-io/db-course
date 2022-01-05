package io.qala.db;

public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException() {
        super("could not serialize access due to concurrent update");
    }
}
