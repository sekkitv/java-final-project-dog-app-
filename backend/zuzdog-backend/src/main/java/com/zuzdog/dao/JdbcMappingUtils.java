package com.zuzdog.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant; //this is used for java time standard library

// this is a utility class that containts methods that helps with with dao . its a final class because we don`t want it to be extended.


public final class JdbcMappingUtils {


    private JdbcMappingUtils() {
    }
    
    //we get back here a java instant object from the result test.
    // if there is a timestamp in the result set, we convert it to an instant object/
    // sql exception is thrown if there is an error with the result set or the column label is not found
    public static Instant getInstant(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel);
        return ts == null ? null : ts.toInstant();
    }
}
