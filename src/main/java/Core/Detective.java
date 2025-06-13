package Core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a player's character within the game world. This class tracks player-specific
 * progress, such as their rank, deductions made, and score.
 */
public class Detective {
  private final String nameOrId;
  private String rank;
  private int currentCaseDeduceCount;
  private int currentCaseFinalExamScore;
  private Room currentRoom;
  // A Set is used here to efficiently track which objects have been deduced,
  // preventing a player from getting credit for the same deduction twice.
  private Set<String> currentCaseDeducedObjects = new HashSet<>();

  public Detective(String nameOrId) {
    this.nameOrId = Objects.requireNonNull(nameOrId, "Detective name/ID cannot be null.");
    resetForNewCase();
  }

  // This method ensures a player's state is reset to default values at the
  // start of a new case, providing a clean slate for each investigation.
  public void resetForNewCase() {
    this.rank = "Junior Investigator";
    this.currentCaseDeduceCount = 0;
    this.currentCaseFinalExamScore = 0;
    this.currentCaseDeducedObjects.clear();
  }

  public void incrementCaseDeduceCount(String objectName) {
    // The count is only incremented if the object has not been deduced before.
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
    // The player's rank is re-evaluated immediately after the exam score is set.
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

  // The player's rank is determined by a combination of their final exam score
  // and the number of deductions they made, rewarding both accuracy and efficiency.
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
