package io.qala.dbcourse.drivers;

import org.junit.Test;
import org.mariadb.jdbc.BasePreparedStatement;
import org.mariadb.jdbc.export.Prepare;
import org.mariadb.jdbc.message.server.CachedPrepareResultPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static io.qala.dbcourse.drivers.Utils.connect;
import static io.qala.dbcourse.drivers.Utils.getField;
import static org.junit.Assert.*;

public class MariaPreparedStatementTest {
    private static final String QUERY_ONE_PARAM = "select * from information_schema.tables where length(table_name) > ?";
    private static final String QUERY_NO_PARAMS = "select * from information_schema.tables where length(table_name) > 5";

    @Test
    public void usesClientPreparedStatementsByDefault() throws Exception {
        try (Connection c = connect(Map.of())) {
            PreparedStatement s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            assertNull(getCachedStatement(s));
            s.close();
        }
    }

    /**
     * See {@link org.mariadb.jdbc.client.context.BaseContext#putPrepareCacheCmd(String, Prepare, BasePreparedStatement)}
     */
    @Test
    public void serverPrepStatementsAreCached_ifPrevStatementClosed() throws Exception {
        try(Connection c = connect(Map.of("useServerPrepStmts", "true"))) {
            PreparedStatement s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            int statementId = getStatementId(s);
            assertNotEquals(0, statementId);
            s.close();

            s = c.prepareStatement(QUERY_ONE_PARAM);
            s.setInt(1, 10);
            s.executeQuery();
            assertEquals(statementId, getStatementId(s));
        }
    }

    /** When evicting statements from the cache, sends {@link org.mariadb.jdbc.message.client.ClosePreparePacket} */
    @Test
    public void serverPrepStatementsAreDiscarded_ifCacheSizeExceeded() throws Exception {
        Map<String, String> props = Map.of(
                "useServerPrepStmts", "true",
                "prepStmtCacheSize", "2");
        try(Connection c = connect(props)) {
            PreparedStatement s = c.prepareStatement(QUERY_NO_PARAMS);
            s.executeQuery();
            int statementId = getStatementId(s);
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS); // same PS, reused
            s.executeQuery();
            assertEquals(statementId, getStatementId(s));
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS + " and true"); // another PS
            s.executeQuery();
            assertNotEquals(statementId, getStatementId(s));
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS + " and false"); // another PS
            s.executeQuery();
            s.close();

            s = c.prepareStatement(QUERY_NO_PARAMS); // can't reuse - prev value was evicted
            s.executeQuery();
            assertNotEquals(statementId, getStatementId(s));// can't reuse the query, it was evicted by the prev one
            s.close();
        }
    }

    private static CachedPrepareResultPacket getCachedStatement(PreparedStatement s) {
        return getField(s, "prepareResult");
    }
    private static int getStatementId(PreparedStatement s) {
        return getCachedStatement(s).getStatementId();
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
