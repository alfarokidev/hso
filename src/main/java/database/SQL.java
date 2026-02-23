package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import model.ModelMapper;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SQL {

    private static volatile SQL instance;
    private final HikariDataSource dataSource;

    @FunctionalInterface
    public interface ResultSetHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }

    private SQL() {
        DBConfig cfg = DBConfig.gI();

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?autoReconnect=true&useUnicode=yes&characterEncoding=UTF-8&serverTimezone=UTC",
                cfg.getHost(),
                cfg.getPort(),
                cfg.getName()
        );

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(cfg.getUsername());
        hc.setPassword(cfg.getPassword());

        hc.setMaximumPoolSize(cfg.getMaximumPoolSize());
        hc.setMinimumIdle(cfg.getMinimumIdle());
        hc.setConnectionTimeout(cfg.getConnectionTimeout());
        hc.setIdleTimeout(cfg.getIdleTimeout());
        hc.setMaxLifetime(cfg.getMaxLifetime());
        hc.setPoolName("HSO_POOL");

        hc.setAutoCommit(false);
        hc.setLeakDetectionThreshold(60000); // 60 seconds

        dataSource = new HikariDataSource(hc);
        log.info("HikariCP initialized successfully");
    }

    public static SQL gI() {
        if (instance == null) {
            synchronized (SQL.class) {
                if (instance == null) {
                    instance = new SQL();
                }
            }
        }
        return instance;
    }

    // ==================== MAIN ENTRY POINTS ====================

    /**
     * Start building a query for a model
     * Usage: SQL.from(Player.class).where("level", ">", 10).get()
     * Usage: SQL.from(Player.class).table("custom_players").get()
     */
    public static <T> QueryBuilder<T> from(Class<T> modelClass) {
        return new QueryBuilder<>(gI(), modelClass);
    }

    /**
     * Start building an insert operation
     * Usage: SQL.insert(player).execute()
     * Usage: SQL.insert(player).table("custom_table").execute()
     */
    public static <T> InsertBuilder<T> insert(T model) {
        return new InsertBuilder<>(gI(), model);
    }

    /**
     * Start building an update operation from class
     * Usage: SQL.update(Player.class).set("level", 20).where("id", "==", 1).execute()
     * Usage: SQL.update(Player.class).table("custom_table").set("level", 20).execute()
     */
    public static <T> UpdateBuilder<T> update(Class<T> modelClass) {
        return new UpdateBuilder<>(gI(), modelClass);
    }

    /**
     * Update from model instance
     * Usage: SQL.update(player).where("id", player.getId()).execute()
     * Usage: SQL.update(player).table("custom_table").whereId().execute()
     */
    public static <T> UpdateBuilder<T> update(T model) {
        return new UpdateBuilder<>(gI(), model);
    }

    /**
     * Start building a delete operation
     * Usage: SQL.delete(Player.class).where("level", "<", 5).execute()
     * Usage: SQL.delete(Player.class).table("custom_table").where("level", "<", 5).execute()
     */
    public static <T> DeleteBuilder<T> delete(Class<T> modelClass) {
        return new DeleteBuilder<>(gI(), modelClass);
    }

    /**
     * Find by ID shorthand
     * Usage: SQL.findById(Player.class, 123)
     */
    public static <T> T findById(Class<T> modelClass, Object id) throws SQLException {
        return from(modelClass).where("id", "==", id).first();
    }

    /**
     * Save (insert or update) a model
     * Usage: SQL.save(player)
     */
    public static <T> boolean save(T model) throws SQLException {
        return save(model, "id");
    }

    /**
     * Save with custom ID field
     */
    public static <T> boolean save(T model, String idField) throws SQLException {
        return gI().saveInternal(model, idField, null);
    }

    /**
     * Save with custom table name
     * Usage: SQL.save(player, "id", "custom_table")
     */
    public static <T> boolean save(T model, String idField, String tableName) throws SQLException {
        return gI().saveInternal(model, idField, tableName);
    }

    // ==================== QUERY BUILDER ====================

    public static class QueryBuilder<T> {
        private final SQL sql;
        private final Class<T> modelClass;
        private String tableName;
        private final List<WhereClause> whereClauses = new ArrayList<>();
        private final List<String> orderByFields = new ArrayList<>();
        private Integer limitValue;
        private Integer offsetValue;

        private QueryBuilder(SQL sql, Class<T> modelClass) {
            this.sql = sql;
            this.modelClass = modelClass;
            this.tableName = ModelMapper.getTableName(modelClass);
        }

        /**
         * Override the table name for this query
         * Usage: SQL.from(Player.class).table("custom_players").get()
         */
        public QueryBuilder<T> table(String customTableName) {
            this.tableName = customTableName;
            return this;
        }

        public QueryBuilder<T> where(String field, String operator, Object value) {
            whereClauses.add(new WhereClause(field, operator, value));
            return this;
        }

        public QueryBuilder<T> where(String field, Object value) {
            return where(field, "==", value);
        }

        public QueryBuilder<T> whereIn(String field, Collection<?> values) {
            return where(field, "in", values);
        }

        public QueryBuilder<T> whereNotIn(String field, Collection<?> values) {
            return where(field, "not-in", values);
        }

        public QueryBuilder<T> whereLike(String field, String pattern) {
            return where(field, "like", pattern);
        }

        public QueryBuilder<T> orderBy(String field) {
            this.orderByFields.add("`" + ModelMapper.toSnakeCase(field) + "` ASC");
            return this;
        }

        public QueryBuilder<T> orderByDesc(String field) {
            this.orderByFields.add("`" + ModelMapper.toSnakeCase(field) + "` DESC");
            return this;
        }

        public QueryBuilder<T> limit(int limit) {
            this.limitValue = limit;
            return this;
        }

        public QueryBuilder<T> offset(int offset) {
            this.offsetValue = offset;
            return this;
        }

        public List<T> get() throws SQLException {
            String query = buildQuery();
            List<Object> params = buildParams();

            List<T> result = sql.executeQuery(query,
                    rs -> ModelMapper.toList(rs, modelClass),
                    params.toArray());

            return result != null ? result : Collections.emptyList();
        }

        public T first() throws SQLException {
            List<T> results = limit(1).get();
            return results.isEmpty() ? null : results.get(0);
        }

        public long count() throws SQLException {
            StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);
            List<Object> params = new ArrayList<>();

            appendWhere(query, params);

            Long result = sql.executeQuery(query.toString(), rs -> {
                if (rs.next()) return rs.getLong(1);
                return 0L;
            }, params.toArray());

            return result != null ? result : 0;
        }

        public boolean exists() throws SQLException {
            return count() > 0;
        }

        private String buildQuery() {
            StringBuilder query = new StringBuilder("SELECT * FROM ").append(tableName);
            List<Object> params = new ArrayList<>();

            appendWhere(query, params);
            appendOrderBy(query);
            appendLimitOffset(query);

            return query.toString();
        }

        private List<Object> buildParams() {
            List<Object> params = new ArrayList<>();
            whereClauses.forEach(w -> w.addParams(params));
            return params;
        }

        private void appendWhere(StringBuilder query, List<Object> params) {
            if (!whereClauses.isEmpty()) {
                query.append(" WHERE ");
                query.append(whereClauses.stream()
                        .map(w -> w.toSql(params))
                        .collect(Collectors.joining(" AND ")));
            }
        }

        private void appendOrderBy(StringBuilder query) {
            if (!orderByFields.isEmpty()) {
                query.append(" ORDER BY ").append(String.join(", ", orderByFields));
            }
        }

        private void appendLimitOffset(StringBuilder query) {
            if (limitValue != null) {
                query.append(" LIMIT ").append(limitValue);
            }
            if (offsetValue != null) {
                query.append(" OFFSET ").append(offsetValue);
            }
        }
    }

    // ==================== INSERT BUILDER ====================

    public static class InsertBuilder<T> {
        private final SQL sql;
        private final T model;
        private String tableName;
        private boolean ignoreId = true;

        private InsertBuilder(SQL sql, T model) {
            this.sql = sql;
            this.model = model;
            this.tableName = ModelMapper.getTableName(model.getClass());
        }

        /**
         * Override the table name for this insert
         * Usage: SQL.insert(player).table("custom_players").execute()
         */
        public InsertBuilder<T> table(String customTableName) {
            this.tableName = customTableName;
            return this;
        }

        public InsertBuilder<T> withId() {
            this.ignoreId = false;
            return this;
        }

        public long execute() throws SQLException {
            Map<String, Object> data = ModelMapper.toMap(model, ignoreId);
            return sql.executeInsert(tableName, data);
        }
    }

    // ==================== UPDATE BUILDER ====================

    public static class UpdateBuilder<T> {
        private final SQL sql;
        private final Class<T> modelClass;
        private String tableName;
        private final Map<String, Object> updates = new HashMap<>();
        private final List<WhereClause> whereClauses = new ArrayList<>();
        private T sourceModel;

        private UpdateBuilder(SQL sql, Class<T> modelClass) {
            this.sql = sql;
            this.modelClass = modelClass;
            this.tableName = ModelMapper.getTableName(modelClass);
        }

        @SuppressWarnings("unchecked")
        private UpdateBuilder(SQL sql, T model) {
            this.sql = sql;
            this.sourceModel = model;
            this.modelClass = (Class<T>) model.getClass();
            this.tableName = ModelMapper.getTableName(modelClass);

            // Pre-populate updates from model
            Map<String, Object> modelData = ModelMapper.toMap(model, true);
            updates.putAll(modelData);
        }

        /**
         * Override the table name for this update
         * Usage: SQL.update(Player.class).table("custom_players").set("level", 20).execute()
         */
        public UpdateBuilder<T> table(String customTableName) {
            this.tableName = customTableName;
            return this;
        }

        public UpdateBuilder<T> set(String field, Object value) {
            updates.put(field, value);
            return this;
        }

        public UpdateBuilder<T> set(Map<String, Object> fields) {
            updates.putAll(fields);
            return this;
        }

        public UpdateBuilder<T> where(String field, String operator, Object value) {
            whereClauses.add(new WhereClause(field, operator, value));
            return this;
        }

        public UpdateBuilder<T> where(String field, Object value) {
            return where(field, "==", value);
        }

        /**
         * Automatically add WHERE clause for model's ID
         * Usage: SQL.update(player).set("level", 20).whereId().execute()
         */
        public UpdateBuilder<T> whereId() {
            return whereId("id");
        }

        /**
         * Automatically add WHERE clause for custom ID field
         */
        public UpdateBuilder<T> whereId(String idField) {
            if (sourceModel == null) {
                throw new IllegalStateException("whereId() can only be used when updating from a model instance");
            }
            Object idValue = ModelMapper.getField(sourceModel, idField);
            if (idValue == null) {
                throw new IllegalStateException("ID field '" + idField + "' is null");
            }
            updates.remove(ModelMapper.toSnakeCase(idField)); // Don't update ID field
            return where(idField, idValue);
        }

        public int execute() throws SQLException {
            if (updates.isEmpty()) {
                return 0;
            }

            StringBuilder query = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            List<Object> params = new ArrayList<>();

            query.append(updates.entrySet().stream()
                    .map(e -> {
                        params.add(e.getValue());
                        return "`" + ModelMapper.toSnakeCase(e.getKey()) + "` = ?";
                    })
                    .collect(Collectors.joining(", ")));

            if (!whereClauses.isEmpty()) {
                query.append(" WHERE ");
                query.append(whereClauses.stream()
                        .map(w -> w.toSql(params))
                        .collect(Collectors.joining(" AND ")));
            }

            return sql.executeUpdate(query.toString(), params.toArray());
        }
    }

    // ==================== DELETE BUILDER ====================

    public static class DeleteBuilder<T> {
        private final SQL sql;
        private final Class<T> modelClass;
        private String tableName;
        private final List<WhereClause> whereClauses = new ArrayList<>();

        private DeleteBuilder(SQL sql, Class<T> modelClass) {
            this.sql = sql;
            this.modelClass = modelClass;
            this.tableName = ModelMapper.getTableName(modelClass);
        }

        /**
         * Override the table name for this delete
         * Usage: SQL.delete(Player.class).table("custom_players").where("level", "<", 5).execute()
         */
        public DeleteBuilder<T> table(String customTableName) {
            this.tableName = customTableName;
            return this;
        }

        public DeleteBuilder<T> where(String field, String operator, Object value) {
            whereClauses.add(new WhereClause(field, operator, value));
            return this;
        }

        public DeleteBuilder<T> where(String field, Object value) {
            return where(field, "==", value);
        }

        public int execute() throws SQLException {
            StringBuilder query = new StringBuilder("DELETE FROM ").append(tableName);
            List<Object> params = new ArrayList<>();

            if (!whereClauses.isEmpty()) {
                query.append(" WHERE ");
                query.append(whereClauses.stream()
                        .map(w -> w.toSql(params))
                        .collect(Collectors.joining(" AND ")));
            }

            return sql.executeUpdate(query.toString(), params.toArray());
        }
    }

    // ==================== WHERE CLAUSE ====================

    private static class WhereClause {
        private final String field;
        private final String operator;
        private final Object value;

        public WhereClause(String field, String operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        public String toSql(List<Object> params) {
            addParams(params);
            String snakeField = "`" + ModelMapper.toSnakeCase(field) + "`";

            return switch (operator) {
                case "==" -> snakeField + " = ?";
                case "!=" -> snakeField + " != ?";
                case ">" -> snakeField + " > ?";
                case ">=" -> snakeField + " >= ?";
                case "<" -> snakeField + " < ?";
                case "<=" -> snakeField + " <= ?";
                case "in" -> buildInClause(snakeField, true);
                case "not-in" -> buildInClause(snakeField, false);
                case "contains", "like" -> snakeField + " LIKE ?";
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            };
        }

        public void addParams(List<Object> params) {
            if (operator.equals("in") || operator.equals("not-in")) {
                if (value instanceof Collection) {
                    params.addAll((Collection<?>) value);
                } else {
                    params.add(value);
                }
            } else if (operator.equals("contains") || operator.equals("like")) {
                params.add("%" + value + "%");
            } else {
                params.add(value);
            }
        }

        private String buildInClause(String field, boolean isIn) {
            if (value instanceof Collection) {
                Collection<?> col = (Collection<?>) value;
                String placeholders = col.stream()
                        .map(v -> "?")
                        .collect(Collectors.joining(", "));
                return field + (isIn ? " IN (" : " NOT IN (") + placeholders + ")";
            }
            return field + (isIn ? " IN (?)" : " NOT IN (?)");
        }
    }

    // ==================== INTERNAL METHODS ====================

    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP closed");
        }
    }

    private <T> T executeQuery(String query, ResultSetHandler<T> handler, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            setParameters(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                return handler.handle(rs);
            }

        } catch (SQLException e) {
            log.error("Query failed: {}", query, e);
            throw e;
        }
    }

    private int executeUpdate(String query, Object... params) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(query);

            setParameters(ps, params);
            int rows = ps.executeUpdate();
            conn.commit();

            ps.close();
            conn.close();
            return rows;

        } catch (SQLException e) {
            log.error("Update failed: {}", query, e);
            rollback(conn);
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Failed to close connection", ex);
                }
            }
            throw e;
        }
    }

    private long executeInsert(String table, Map<String, Object> values) throws SQLException {
        if (values == null || values.isEmpty()) {
            throw new SQLException("Cannot insert with empty values");
        }

        String columns = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream()
                .map(k -> "?")
                .collect(Collectors.joining(", "));

        String query = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";

        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            setParameters(ps, values.values().toArray());

            int rows = ps.executeUpdate();
            conn.commit();

            long generatedId = -1;
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                    }
                }
            }

            ps.close();
            conn.close();

            if (generatedId > 0) {
                return generatedId;
            } else if (rows > 0) {
                return 0; // Insert successful but no generated key
            } else {
                throw new SQLException("Insert failed: no rows affected");
            }

        } catch (SQLException e) {
            log.error("Insert failed: {}", table, e);
            rollback(conn);
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    log.error("Failed to close connection", ex);
                }
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> boolean saveInternal(T model, String idField, String customTableName) throws SQLException {
        Map<String, Object> data = ModelMapper.toMap(model, true);
        String tableName = customTableName != null ? customTableName : ModelMapper.getTableName(model.getClass());

        Object idValue = ModelMapper.getField(model, idField);

        // If ID exists and is valid, check if row exists in database
        if (idValue != null && idValue instanceof Number && ((Number) idValue).longValue() > 0) {
            boolean exists = from((Class<T>) model.getClass())
                    .table(tableName)
                    .where(idField, idValue)
                    .exists();

            if (exists) {
                // Row exists - UPDATE
                Map<String, Object> updates = new HashMap<>(data);
                updates.remove(ModelMapper.toSnakeCase(idField));

                int rows = update((Class<T>) model.getClass())
                        .table(tableName)
                        .set(updates)
                        .where(idField, idValue)
                        .execute();

                return rows > 0;
            }
        }

        // ID is null/zero OR row doesn't exist - INSERT
        if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() <= 0)) {
            // No ID provided, let DB generate it
            data.remove(ModelMapper.toSnakeCase(idField));
            long generatedId = executeInsert(tableName, data);

            if (generatedId > 0) {
                ModelMapper.setField(model, idField, generatedId);
                return true;
            }
            return false;
        } else {
            // ID provided but row doesn't exist, insert with specified ID
            long insertedRows = executeInsert(tableName, data);
            return insertedRows >= 0;
        }
    }

    private void setParameters(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                log.error("Rollback failed", e);
            }
        }
    }
}