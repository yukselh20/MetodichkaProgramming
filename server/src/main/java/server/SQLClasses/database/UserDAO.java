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

// This is the Data Access Object (DAO) for the 'users' table. It encapsulates all
// database operations related to user accounts.
public class UserDAO {

  private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

  // This finds a user in the database by their username.
  public Optional<User> findUserByUsername(String username) {
    String sql = "SELECT id, username FROM users WHERE username = ?";

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

  // This retrieves the stored password hash for a given username. I kept this separate
  // from findUserByUsername to avoid fetching the hash unless I absolutely need it.
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

  // This adds a new user to the database, first checking if the username already exists.
  public Optional<User> addUser(String username, String plainPassword) {
    // First, I'll ensure the username is not already taken.
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
