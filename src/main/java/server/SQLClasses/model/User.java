package server.SQLClasses.model;

/**
 * A simple model class (POJO) to represent an authenticated user. It holds only the non-sensitive
 * information needed by the application after a user has successfully logged in.
 */
public class User {
  private final int id;
  private final String username;

  public User(int id, String username) {
    this.id = id;
    this.username = username;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }
}
