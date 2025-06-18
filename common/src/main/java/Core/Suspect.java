package Core;

import java.util.Objects;

// This represents an NPC suspect in the case. A suspect is a movable character who
// has a name, a statement to give when questioned, and may hold a clue.
public class Suspect extends MovableCharacter {
  private final String name;
  private final String statement;
  private final String clue;

  public Suspect(String name, String statement, String clue) {
    this.name = Objects.requireNonNull(name, "Suspect name cannot be null.");
    if (name.isBlank()) throw new IllegalArgumentException("Suspect name cannot be blank.");
    this.statement = (statement != null) ? statement : "No comment.";
    this.clue = clue;
  }

  public String getName() {
    return name;
  }

  public String getStatement() {
    return statement;
  }

  public String getClue() {
    return clue;
  }

  @Override
  public String toString() {
    return "Suspect{name='" + name + "'}";
  }
}
