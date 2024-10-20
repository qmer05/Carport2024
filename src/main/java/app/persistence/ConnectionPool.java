package app.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * Singleton pattern applied to handling a Hikari ConnectionPool
 */
public class ConnectionPool {

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=public";
    private static final String DB = "carport";
    public static ConnectionPool instance = null;
    public static HikariDataSource ds = null;

    /***
     * Private constructor due to singleton pattern.
     */
    private ConnectionPool() {}

    /***
     * Singleton instance method to retrieve the connection pool.
     * This checks whether the app is deployed and fetches the connection information
     * from environment variables if necessary.
     *
     * @return A ConnectionPool object
     */
    public static ConnectionPool getInstance() {
        // Check if we're in a deployed environment (e.g., GitHub Actions or production)
        if (System.getenv("DEPLOYED") != null) {
            return getInstance(
                    System.getenv("JDBC_USER"),
                    System.getenv("JDBC_PASSWORD"),
                    System.getenv("JDBC_CONNECTION_STRING"),
                    System.getenv("JDBC_DB")
            );
        } else {
            // Local development defaults
            return getInstance(USER, PASSWORD, URL, DB);
        }
    }

    /***
     * Singleton instance method with credentials and connection info.
     * @param user for PostgreSQL database user
     * @param password for PostgreSQL database user
     * @param url connection string for PostgreSQL database
     * @param db database name for connection
     * @return A ConnectionPool object
     */
    public static ConnectionPool getInstance(String user, String password, String url, String db) {
        if (instance == null) {
            ds = createHikariConnectionPool(user, password, url, db);
            instance = new ConnectionPool();
        }
        return instance;
    }

    /***
     * Method to get a live connection from the connection pool.
     * @return a database connection to be used in SQL requests
     * @throws SQLException
     */
    public synchronized Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    /***
     * Closing the Hikari connection pool after use.
     */
    public synchronized void close() {
        Logger.getLogger("web").log(Level.INFO, "Shutting down connection pool");
        ds.close();
    }

    /***
     * Configuring the Hikari DataSource ConnectionPool.
     * @param user for PostgreSQL database user
     * @param password for PostgreSQL database user
     * @param url connection string for PostgreSQL database
     * @param db database name for connection
     * @return a Hikari DataSource
     */
    private static HikariDataSource createHikariConnectionPool(String user, String password, String url, String db) {
        Logger.getLogger("web").log(Level.INFO, String.format("Connection Pool created for: (%s, %s, %s, %s)", user, password, url, db));

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format(url, db));  // %s is replaced by the DB name
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setPoolName("Postgresql Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }
}
