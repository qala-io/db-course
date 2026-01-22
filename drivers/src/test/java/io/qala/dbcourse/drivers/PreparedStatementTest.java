package io.qala.dbcourse.drivers;

import org.junit.Test;
import org.postgresql.core.CachedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static io.qala.dbcourse.drivers.Utils.connect;
import static io.qala.dbcourse.drivers.Utils.getField;
import static org.junit.Assert.*;

public class PreparedStatementTest {
    public static final System.Logger LOG = System.getLogger(PreparedStatementTest.class.getName());
    public static final String PG_QUERY_ONE_PARAM = "select * from pg_attribute where length(attname) > ?";

    /**
     * This is true in Postgres.
     */
    @Test
    public void serverPrepStatementsAreCached_ifPrevStatementClosed() throws Exception {
        try(Connection c = connect(Map.of("prepareThreshold", "-1"))) {
            PreparedStatement s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertNull("Statement Name shouldn't be assigned before execution", getPgStatementName(s));

            s.setInt(1, 1);
            assertNotEquals(0, getRowCnt(s.executeQuery()));
            String statementName = getPgStatementName(s);
            s.close();

            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertEquals(statementName, getPgStatementName(s));
            s.setInt(1, 10);
            s.executeQuery();

            s = c.prepareStatement(PG_QUERY_ONE_PARAM);
            assertNull("Statement isn't borrowed from the pool if prev one wasn't closed", getPgStatementName(s));
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
