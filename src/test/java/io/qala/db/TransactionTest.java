package io.qala.db;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        TransactionCanReadXminTests.class,
        TransactionCanReadXmaxTests.class,
})
public class TransactionTest {}