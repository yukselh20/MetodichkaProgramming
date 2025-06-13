package server.SQLClasses.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.SQLClasses.auth.PasswordHasher;
import server.SQLClasses.model.User;

/**
 * Data Access Object (DAO) for the 'users' table. This class encapsulates all database operations
 * related to user accounts, such as creating new users and finding existing ones.
 */
public class UserDAO {

  private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

  /**
   * Finds a user in the database by their username.
   *
   * @param username The username to search for.
   * @return An Optional containing the User object if found, otherwise an empty Optional.
   */
  public Optional<User> findUserByUsername(String username) {
    String sql = "SELECT id, username FROM users WHERE username = ?";

    // A try-with-resources block ensures the connection and statement are always closed.
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        int id = rs.getInt("id");
        String foundUsername = rs.getString("username");
        return Optional.of(new User(id, foundUsername));
      }
    } catch (SQLException e) {
      logger.error("Database error in findUserByUsername for user '{}'", username, e);
    }
    return Optional.empty();
  }

  /**
   * Retrieves the stored password hash for a given username. This is kept separate from
   * findUserByUsername to avoid fetching the hash unless needed.
   *
   * @param username The username whose password hash is needed.
   * @return An Optional containing the password hash, or an empty Optional if the user doesn't
   *     exist.
   */
  public Optional<String> getPasswordHashForUser(String username) {
    String sql = "SELECT password_hash FROM users WHERE username = ?";
    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      ResultSet rs = pstmt.executeQuery();

      if (rs.next()) {
        return Optional.of(rs.getString("password_hash"));
      }
    } catch (SQLException e) {
      logger.error("Database error in getPasswordHashForUser for user '{}'", username, e);
    }
    return Optional.empty();
  }

  /**
   * Adds a new user to the database. First checks if the username already exists.
   *
   * @param username The username for the new user.
   * @param plainPassword The plain-text password for the new user.
   * @return An Optional containing the newly created User object on success, or an empty Optional
   *     if the username is already taken.
   */
  public Optional<User> addUser(String username, String plainPassword) {
    // First, ensure the username is not already taken.
    if (findUserByUsername(username).isPresent()) {
      logger.warn("Attempt to register a user that already exists: '{}'", username);
      return Optional.empty();
    }
    String hashedPassword = PasswordHasher.hash(plainPassword);
    String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?) RETURNING id";

    try (Connection conn = DatabaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql)) {

      pstmt.setString(1, username);
      pstmt.setString(2, hashedPassword);

      // Execute the insert and get the auto-generated ID back.
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        int newId = rs.getInt(1);
        logger.info("New user '{}' created with ID {}.", username, newId);
        return Optional.of(new User(newId, username));
      }
    } catch (SQLException e) {
      logger.error("Database error in addUser for username '{}'", username, e);
    }
    return Optional.empty();
  }
}
