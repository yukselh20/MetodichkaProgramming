package server.SQLClasses.model;

// This is the simple model class (POJO) to represent an authenticated user. It holds only the
// non-sensitive information I need after a user has successfully logged in.
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
