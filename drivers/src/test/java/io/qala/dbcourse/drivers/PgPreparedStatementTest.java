package io.qala.dbcourse.drivers;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.junit.Test;
import org.postgresql.core.CachedQuery;

import java.sql.*;
import java.util.Map;

import static io.qala.dbcourse.drivers.Utils.*;
import static org.junit.Assert.*;

public class PgPreparedStatementTest {
    private static final String QUERY_ONE_PARAM = "select * from information_schema.tables where length(table_name) > ?";
    private static final String QUERY_NO_PARAMS = "select * from information_schema.tables where length(table_name) > 5";

    @Test
    public void pgServerPrepStatementsAreCached_ifPrevStatementClosed() throws Exception {
        try(Connection c = connect(Map.of("prepareThreshold", "-1"))) {
            PreparedStatement s = c.prepareStatement(QUERY_ONE_PARAM);
            assertNull("Statement Name shouldn't be assigned before execution", getPgStatementName(s));

            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            String statementName = getPgStatementName(s);
            assertNotNull("Cached queries are named", getPgStatementName(s));
            s.close();

            s = c.prepareStatement(QUERY_ONE_PARAM);
            assertEquals(statementName, getPgStatementName(s));
            s.setInt(1, 10);
            s.executeQuery();
            assertEquals(statementName, getPgStatementName(s));

            s = c.prepareStatement(QUERY_ONE_PARAM);
            assertNull("Statement isn't borrowed from the pool if prev one wasn't closed", getPgStatementName(s));
        }
    }

    @Test
    public void pgDoesNotAssignPreparedStatementName_andKeepsStatementsOneShot_untilPrepareThresholdReached() throws Exception {
        try (Connection c = connect(Map.of("prepareThreshold", "3"))) {
            PreparedStatement s = c.prepareStatement(QUERY_ONE_PARAM);
            assertNull("Statement Name shouldn't be assigned before execution", getPgStatementName(s));
            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            assertNull(getPgStatementName(s));// the name isn't assigned, it's a One-Shot query:
            s.close();

            s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            assertNull(getPgStatementName(s)); // still a One-Shot query:
            s.close();

            // reached threshold, the name is assigned and the result columns meta (from PG) is stored in CachedQuery
            s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            String statementName = getPgStatementName(s);
            assertNotNull(statementName);
            s.close();

            // Finally instead of the whole query, we start sending the query name
            s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            assertEquals(statementName, getPgStatementName(s));
        }
    }
    @Test
    public void pgServerPrepStatementsAreDiscarded_ifCacheSizeExceeded() throws Exception {
        Map<String, String> props = Map.of(
                "prepareThreshold", "-1",
                "preparedStatementCacheQueries", "2");
        try(Connection c = connect(props)) {
            PreparedStatement s = c.prepareStatement(QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS + " and true"); // generates another PS
            s.executeQuery();
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS + " or false"); // yet another PS
            s.executeQuery();
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();
            assertEquals("S_4", getPgStatementName(s));// can't reuse the query, it was evicted by the prev one

            s = c.prepareStatement(QUERY_NO_PARAMS);
            s.executeQuery();
            s.close();
            assertEquals("S_4", getPgStatementName(s)); // reuses the query that's in the cache
        }
    }

    @Test
    public void pgDoesNotAssignStatementName_andKeepsStatementsOneShot_untilPrepareThresholdReached() throws Exception {
        try (Connection c = connect(Map.of("prepareThreshold", "3"))) {
            Statement s = c.createStatement();
            assertNotEquals(0, getRowCnt(s.executeQuery(QUERY_NO_PARAMS)));

            s.executeQuery(QUERY_NO_PARAMS);
            s.close();

            s = c.createStatement();
            s.executeQuery(QUERY_NO_PARAMS);
        }
    }
    @Test
    public void doesNotFetchAllRows_ifFetchSizeIsSet() throws Exception {
        try (Connection c = connect(Map.of())) {
            // fetchSize doesn't work with autoCommit=true, see PgStatement#executeInternal(), look for QUERY_FORWARD_CURSOR
            c.setAutoCommit(false);
            Statement s = c.createStatement();
            s.setFetchSize(10);
            ResultSet rs = s.executeQuery(QUERY_NO_PARAMS);
            int rowCnt = 0;
            while (rs.next()) {
                rs.getString(1);
                rowCnt++;
            }
            System.out.println(rowCnt);
            c.setAutoCommit(true);
        }
    }

    @Test
    public void failsIfDdlCreatesColumnsBeforeQueryInSameStatetemnt() throws Exception {
        // This seems like a bug - if a single PreparedStatement contains DDL & Query, the query seems to be
        // validated before the DDL is issued and therefore we get an error that the table doesn't exist.
        // This is exacerbated by the fact that PG uses server-prepared statements even for usual Statements.
        //
        // If we configure preferQueryMode=extendedForPrepared (see below), then at least Statement won't be prepared
        // and this problem won't exist for it.
        try (Connection c = connect(Map.of("prepareThreshold", "-1"))) {
            Statement s = c.createStatement();
            s.execute("drop table if exists t");
            Exception e = assertThrows(Exception.class, () ->
                    s.executeQuery("create table t (id text); select count(*) from t;"));
            assertTrue("Actual: " + e.getMessage(), e.getMessage().startsWith("ERROR: relation \"t\" does not exist"));
        }
        // When Statement doesn't turn into a PreparedStatement (this happens only with prepareThreshold=-1), then
        // we're good:
        try (Connection c = connect(Map.of())) {
            Statement s = c.createStatement();
            s.execute("drop table if exists t");
            s.execute("create table t (id text); select count(*) from t;");
        }
        // This forces to use Simple Query Protocol for Statements, and therefore no separate BIND is sent -> hence no error
        try (Connection c = connect(Map.of("prepareThreshold", "-1", "preferQueryMode", "extendedForPrepared"))) {
            Statement s = c.createStatement();
            s.execute("drop table if exists t");
            s.execute("create table t (id text); select count(*) from t;");
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
                PreparedStatement s = c.prepareStatement(QUERY_ONE_PARAM);
                s.setInt(1, 5);
                s.executeQuery();
                s.close();

                s = c.prepareStatement(QUERY_ONE_PARAM);
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
