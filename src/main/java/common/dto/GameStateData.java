package common.dto;

import java.io.Serializable;
import java.util.*;

/**
 * A comprehensive DTO that encapsulates the entire state of a game session. Its primary purpose is
 * for persistence, allowing a game's progress to be saved to disk and resumed later.
 */
public class GameStateData implements Serializable {
  private static final long serialVersionUID = 3L;

  private final String gameSessionId;
  private final String caseTitle;
  private final List<String> playerIds;
  private final List<String> journalEntries;
  private final int deduceCountGlobal;
  private final Set<String> deducedObjectsGlobal;
  private final Map<String, String> playerLocations;
  private final Map<String, String> npcLocations;
  private final List<String> completedTasks;
  private final boolean caseStarted;

  public GameStateData(
      String gameSessionId,
      String caseTitle,
      List<String> playerIds,
      List<String> journalEntries,
      int deduceCountGlobal,
      Set<String> deducedObjectsGlobal,
      Map<String, String> playerLocations,
      Map<String, String> npcLocations,
      List<String> completedTasks,
      boolean caseStarted) {

    Objects.requireNonNull(gameSessionId, "Game session ID cannot be null");
    Objects.requireNonNull(caseTitle, "Case title cannot be null");
    Objects.requireNonNull(playerIds, "Player ID list cannot be null");
    Objects.requireNonNull(journalEntries, "Journal entries list cannot be null");
    Objects.requireNonNull(deducedObjectsGlobal, "Deduced objects set cannot be null");
    Objects.requireNonNull(playerLocations, "Player locations map cannot be null");
    Objects.requireNonNull(npcLocations, "NPC locations map cannot be null");
    Objects.requireNonNull(completedTasks, "Completed tasks list cannot be null");

    // The constructor creates defensive copies of all mutable collections. This
    // ensures that the DTO's internal state cannot be altered after creation
    // by modifying the original collections passed to the constructor.
    this.gameSessionId = gameSessionId;
    this.caseTitle = caseTitle;
    this.playerIds = new ArrayList<>(playerIds);
    this.journalEntries = new ArrayList<>(journalEntries);
    this.deduceCountGlobal = deduceCountGlobal;
    this.deducedObjectsGlobal = new HashSet<>(deducedObjectsGlobal);
    this.playerLocations = new HashMap<>(playerLocations);
    this.npcLocations = new HashMap<>(npcLocations);
    this.completedTasks = new ArrayList<>(completedTasks);
    this.caseStarted = caseStarted;
  }

  public String getGameSessionId() {
    return gameSessionId;
  }

  public String getCaseTitle() {
    return caseTitle;
  }

  public List<String> getPlayerIds() {
    return Collections.unmodifiableList(playerIds);
  }

  public List<String> getJournalEntries() {
    return Collections.unmodifiableList(journalEntries);
  }

  public int getDeduceCountGlobal() {
    return deduceCountGlobal;
  }

  public Set<String> getDeducedObjectsGlobal() {
    return Collections.unmodifiableSet(deducedObjectsGlobal);
  }

  public Map<String, String> getPlayerLocations() {
    return Collections.unmodifiableMap(playerLocations);
  }

  public Map<String, String> getNpcLocations() {
    return Collections.unmodifiableMap(npcLocations);
  }

  public List<String> getCompletedTasks() {
    return Collections.unmodifiableList(completedTasks);
  }

  public boolean isCaseStarted() {
    return caseStarted;
  }
}
