package Core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// This class represents a player's character in the game world. I use it to track
// player-specific progress like rank, deductions, and score.
public class Detective {
  private final String nameOrId;
  private String rank;
  private int currentCaseDeduceCount;
  private int currentCaseFinalExamScore;
  private Room currentRoom;
  private Set<String> currentCaseDeducedObjects = new HashSet<>();

  public Detective(String nameOrId) {
    this.nameOrId = Objects.requireNonNull(nameOrId, "Detective name/ID cannot be null.");
    resetForNewCase();
  }

  // This method resets a player's state to default values at the start of a new case,
  // providing a clean slate for each investigation.
  public void resetForNewCase() {
    this.rank = "Junior Investigator";
    this.currentCaseDeduceCount = 0;
    this.currentCaseFinalExamScore = 0;
    this.currentCaseDeducedObjects.clear();
  }

  public void incrementCaseDeduceCount(String objectName) {
    // I only increment the count if the object has not been deduced before.
    if (objectName != null && currentCaseDeducedObjects.add(objectName.toLowerCase())) {
      this.currentCaseDeduceCount++;
    }
  }

  public int getCaseDeduceCount() {
    return currentCaseDeduceCount;
  }

  public String getName() {
    return nameOrId;
  }

  public String getRank() {
    return rank;
  }

  public void setCaseFinalExamScore(int score) {
    this.currentCaseFinalExamScore = score;
    // I re-evaluate the player's rank immediately after the exam score is set.
    evaluateRank();
  }

  public int getCaseFinalExamScore() {
    return currentCaseFinalExamScore;
  }

  public Room getCurrentRoom() {
    return currentRoom;
  }

  public void setCurrentRoom(Room room) {
    this.currentRoom = room;
  }

  // I determine the player's rank by a combination of their final exam score and
  // the number of deductions they made, which rewards both accuracy and efficiency.
  public void evaluateRank() {
    if (currentCaseFinalExamScore >= 3 && currentCaseDeduceCount <= 5) {
      rank = "Senior Investigator";
    } else if (currentCaseFinalExamScore >= 2 && currentCaseDeduceCount <= 10) {
      rank = "Intermediate Investigator";
    } else {
      rank = "Junior Investigator";
    }
  }

  public boolean hasDeducedInCase(String objectName) {
    return objectName != null && currentCaseDeducedObjects.contains(objectName.toLowerCase());
  }
}
