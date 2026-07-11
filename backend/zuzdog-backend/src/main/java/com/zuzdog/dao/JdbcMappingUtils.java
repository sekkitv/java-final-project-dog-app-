package com.zuzdog.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant; //this is used for java time standard library

public final class JdbcMappingUtils {

    private JdbcMappingUtils() {
    }
    //we get back here a java instant object from the result test.
    // if there is a timestamp in the result set, we convert it to an instant object/
    public static Instant getInstant(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel);
        return ts == null ? null : ts.toInstant();
    }
}
