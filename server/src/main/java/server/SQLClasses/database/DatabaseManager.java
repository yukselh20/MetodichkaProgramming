package server.SQLClasses.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the connection to the PostgreSQL database. This class provides a centralized method for
 * obtaining a database connection, configured for the university's 'studs' database server.
 */
public class DatabaseManager {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

  // Reads the DB_URL environment variable.
  private static final String DATABASE_URL =
      System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://pg:5432/studs");

  // Reads the DB_USER environment variable.
  private static final String DATABASE_USER = System.getenv().getOrDefault("DB_USER", "s407888");

  // Reads the DB_PASSWORD environment variable.
  // FOR THE LAB: The default value is set to the required password.
  private static final String DATABASE_PASSWORD =
      System.getenv().getOrDefault("DB_PASSWORD", "CdZk56wMxVBYV2lE");

  /** Private constructor to prevent instantiation of this utility class. */
  private DatabaseManager() {}

  /**
   * Establishes and returns a new connection to the database using the loaded configuration. The
   * calling method is responsible for closing this connection.
   *
   * @return A new Connection object to the database.
   * @throws SQLException if a database access error occurs (e.g., wrong password, server down).
   */
  public static Connection getConnection() throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      logger.error(
          "FATAL: PostgreSQL JDBC Driver not found. Make sure it's in your pom.xml or build.gradle.",
          e);
      throw new RuntimeException("PostgreSQL JDBC Driver not found.", e);
    }
    return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
  }
}
