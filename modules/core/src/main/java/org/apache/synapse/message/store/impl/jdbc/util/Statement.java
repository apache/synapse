package org.apache.synapse.message.store.impl.jdbc.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Statement class for raw SQL statement
 */
public abstract class Statement {
    /**
     * Defines the SQL statement which should be executed.
     */
    private String statement = null;

    /**
     * List of parameters which should be included in the statement.
     */
    private final List<Object> parameters = new ArrayList<Object>();

    /**
     * Provides the de-serialized outcome of the query.
     *
     * @param resultSet the result-set obtained from the DB.
     * @return the result which contain each row and the corresponding column.
     */
    public abstract List<Map> getResult(ResultSet resultSet) throws SQLException;

    public Statement(String rawStatement) {
        this.statement = rawStatement;
    }

    public String getStatement() {
        return statement;
    }

    public void addParameter(Object o) {
        parameters.add(o);
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
