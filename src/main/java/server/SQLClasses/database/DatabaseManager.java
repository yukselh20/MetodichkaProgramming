package server.SQLClasses.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages the connection to the PostgreSQL database. This class provides a centralized method for
 * obtaining a database connection, configured for the university's 'studs' database server.
 */
public class DatabaseManager {

  private static final String DATABASE_URL = "jdbc:postgresql://pg:5432/studs";

  private static final String DATABASE_USER = "s407888";
  private static final String DATABASE_PASSWORD = null;

  /** Private constructor to prevent instantiation of this utility class. */
  private DatabaseManager() {}

  /**
   * Establishes and returns a new connection to the database. The calling method is responsible for
   * closing this connection.
   *
   * @return A new Connection object to the database.
   * @throws SQLException if a database access error occurs (e.g., wrong password, server down).
   */
  public static Connection getConnection() throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      System.err.println(
          "FATAL: PostgreSQL JDBC Driver not found. Make sure it's in your pom.xml or build.gradle.");
      throw new RuntimeException("PostgreSQL JDBC Driver not found.", e);
    }
    return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
  }
}
