package io.qala.db.pg;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        SnapshotIsolationReaderXminTests.class,
        SnapshotIsolationReaderXmaxTests.class,
})
public class SnapshotIsolationReaderTest {}