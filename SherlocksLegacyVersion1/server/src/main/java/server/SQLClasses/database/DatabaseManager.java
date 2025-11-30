package server.SQLClasses.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This to manage the connection to the PostgreSQL database. It provides a
// centralized method for obtaining a connection.
public class DatabaseManager {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

  private static final String DATABASE_URL =
      Objects.requireNonNull(
          System.getenv("DB_URL"),
          "FATAL: Database URL not set. Please set the DB_URL environment variable.");

  private static final String DATABASE_USER =
      Objects.requireNonNull(
          System.getenv("DB_USER"),
          "FATAL: Database user not set. Please set the DB_USER environment variable.");

  private static final String DATABASE_PASSWORD =
      Objects.requireNonNull(
          System.getenv("DB_PASSWORD"),
          "FATAL: Database password not set. Please set the DB_PASSWORD environment variable.");

  // Private constructor to prevent instantiation.
  private DatabaseManager() {}

  // This establishes and returns a new connection to the database. The calling
  // method is responsible for closing this connection.
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
