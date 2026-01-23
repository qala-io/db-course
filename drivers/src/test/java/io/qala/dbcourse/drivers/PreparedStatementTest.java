package io.qala.dbcourse.drivers;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.Test;
import org.postgresql.core.CachedQuery;

import java.sql.*;
import java.util.Map;

import static io.qala.dbcourse.drivers.Utils.*;
import static org.junit.Assert.*;

public class PreparedStatementTest {
    public static final String PG_QUERY_ONE_PARAM = "select * from pg_attribute where length(attname) > ?";
    public static final String PG_QUERY_NO_PARAMS = "select * from pg_attribute where length(attname) > 5";

    @Test
    public void pgServerPrepStatementsAreCached_ifPrevStatementClosed() throws Exception {
        try(Connection c = connect(Map.of("prepareThreshold", "-1"))) {
            PreparedStatement s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertNull("Statement Name shouldn't be assigned before execution", getPgStatementName(s));

            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            String statementName = getPgStatementName(s);
            assertNotNull("Cached queries are named", getPgStatementName(s));
            s.close();

            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertEquals(statementName, getPgStatementName(s));
            s.setInt(1, 10);
            s.executeQuery();
            assertEquals(statementName, getPgStatementName(s));

            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertNull("Statement isn't borrowed from the pool if prev one wasn't closed", getPgStatementName(s));
        }
    }
    @Test
    public void pgServerPrepStatementsAreDiscarded_ifCacheSizeExceeded() throws Exception {
        Map<String, String> props = Map.of(
                "prepareThreshold", "-1",
                "preparedStatementCacheQueries", "2");
        try(Connection c = connect(props)) {
            PreparedStatement s = c.prepareStatement(PG_QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();

            s = c.prepareStatement(PG_QUERY_NO_PARAMS + " and true"); // generates another PS
            s.executeQuery();
            s.close();

            s = c.prepareStatement(PG_QUERY_NO_PARAMS + " or false"); // yet another PS
            s.executeQuery();
            s.close();

            s = c.prepareStatement(PG_QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();
            assertEquals("S_4", getPgStatementName(s));// can't reuse the query, it was evicted by the prev one

            s = c.prepareStatement(PG_QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();
            assertEquals("S_4", getPgStatementName(s)); // reuses the query that's in the cache
        }
    }
    @Test
    public void pgDoesNotAssignPreparedStatementName_andKeepsStatementsOneShot_untilPrepareThresholdReached() throws Exception {
        try(Connection c = connect(Map.of("prepareThreshold", "3"))) {
            PreparedStatement s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertNull("Statement Name shouldn't be assigned before execution", getPgStatementName(s));
            // the name isn't assigned, it's a One-Shot query:
            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            assertNull("One-Shot statements (those that didn't reach prepareThreshold) are not named, and the name isn't sent to PG",
                    getPgStatementName(s));
            assertNull(getPgStatementName(s));
            s.close();
            // still a One-Shot query:
            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            assertNull(getPgStatementName(s));
            s.close();
            // finally prepareThreshold is reached, and the name gets assigned:
            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            assertNotNull(getPgStatementName(s));
        }
    }

    @Test
    public void pgDoesNotAssignStatementName_andKeepsStatementsOneShot_untilPrepareThresholdReached() throws Exception {
        try (Connection c = connect(Map.of("prepareThreshold", "3"))) {
            Statement s = c.createStatement();
            assertNotEquals(0, getRowCnt(s.executeQuery(PG_QUERY_NO_PARAMS)));

            s.executeQuery(PG_QUERY_NO_PARAMS);
            s.close();

            s = c.createStatement();
            s.executeQuery(PG_QUERY_NO_PARAMS);
        }
    }
    @Test
    public void doesNotFetchAllRows_ifFetchSizeIsSet() throws Exception {
        try (Connection c = connect(Map.of())) {
            // fetchSize doesn't work with autoCommit=true, see PgStatement#executeInternal(), look for QUERY_FORWARD_CURSOR
            c.setAutoCommit(false);
            Statement s = c.createStatement();
            s.setFetchSize(10);
            ResultSet rs = s.executeQuery(PG_QUERY_NO_PARAMS);
            int rowCnt = 0;
            while (rs.next()) {
                rs.getString(1);
                rowCnt++;
            }
            System.out.println(rowCnt);
            c.setAutoCommit(true);
        }
    }

    /**
     * C3P0 statement cache is useless for PG. PG already caches values inside using {@link CachedQuery},
     * there's no need to cache & reuse PgPreparedStatement itself.
     */
    @Test
    public void c3p0_prepStatementCache() throws Exception {
        try (ComboPooledDataSource pool = dbPool(Map.of())) {
//            pool.setMaxStatementsPerConnection(10);
            try (Connection c = pool.getConnection()) {
                PreparedStatement s = c.prepareStatement(PG_QUERY_ONE_PARAM);
                s.setInt(1, 5);
                s.executeQuery();
                s.close();

                s = c.prepareStatement(PG_QUERY_ONE_PARAM);
                s.setInt(1, 5);
                s.executeQuery();
                s.close();
            }
        }
    }

    private static String getPgStatementName(PreparedStatement s) {
        return getPgStatementName(getPgCachedQuery(s));
    }
    private static String getPgStatementName(CachedQuery q) {
        return getField(q.query, "statementName");
    }
    private static CachedQuery getPgCachedQuery(PreparedStatement s) {
        return getField(s, "preparedQuery");
    }

    private static int getRowCnt(ResultSet rs) throws SQLException {
        int rowCnt = 0;
        while (rs.next()) {
            rs.getString(1);
            rowCnt++;
        }
        return rowCnt;
    }
}
