package server;

import Core.*;
import JsonDTO.CaseFile;
import common.Commands.Command;
import common.ICommandContext;
import common.dto.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the authoritative game state and logic for a single, specific GameSession. It is the
 * heart of the game world, holding all rooms, players, and NPCs, and serving as the implementation
 * for the ICommandContext interface.
 */
public class GameContextServer implements ICommandContext {

  private final GameSession parentSession;
  private final String casesDir;

  private CaseFile selectedCase;
  private Map<String, Room> rooms = new HashMap<>();
  private Journal journal = new Journal();
  private TaskList taskList;
  private Map<String, Detective> players = new HashMap<>();

  private Set<String> deducedObjectsSession = new HashSet<>();
  private int deduceCountSession = 0;
  private boolean caseStarted = false;
  // The context delegates management of NPCs and the Final Exam to specialized manager classes.
  private final NpcManager npcManager;
  private final FinalExamManager examManager;
  private final GameSessionManager sessionManager;

  public GameContextServer(
      GameSession parentSession, String casesDir, GameSessionManager sessionManager) {
    this.parentSession = Objects.requireNonNull(parentSession);
    this.casesDir = Objects.requireNonNull(casesDir);
    this.sessionManager = Objects.requireNonNull(sessionManager);
    this.npcManager = new NpcManager(this);
    this.examManager = new FinalExamManager(this, parentSession);
  }

  public GameSession getParentSession() {
    return parentSession;
  }

  /**
   * Initializes or resets the entire game world based on a selected CaseFile. This method is
   * synchronized to prevent race conditions if multiple players trigger initialization at the same
   * time.
   */
  public synchronized boolean initializeCase(CaseFile caseFile) {
    String sessionId = parentSession.getSessionId();
    System.out.println(
        "Context for session [" + sessionId + "] initializing case: " + caseFile.getTitle());

    // Reset all state to ensure a clean slate for the new case.
    this.selectedCase = caseFile;
    this.rooms.clear();
    this.deducedObjectsSession.clear();
    this.deduceCountSession = 0;
    this.journal = new Journal();
    this.taskList = new TaskList(caseFile.getTasks() != null ? caseFile.getTasks() : List.of());
    this.caseStarted = false;
    players.values().forEach(Detective::resetForNewCase);

    try {
      // Loading is delegated to extractor utilities, separating parsing from game logic.
      if (!server.extractors.BuildingExtractorServer.loadBuilding(caseFile, this)) return false;
      server.extractors.GameObjectExtractorServer.loadObjects(caseFile, this);
      npcManager.initializeNpcs(caseFile, this.rooms);

      // All players are placed in the designated starting room.
      Room playerStartRoom = getRoomByName(caseFile.getStartingRoom());
      // A fallback ensures the game can start even if the starting room is misconfigured.
      if (playerStartRoom == null)
        playerStartRoom = this.rooms.values().stream().findFirst().orElse(null);

      if (playerStartRoom != null) {
        final Room finalStartRoom = playerStartRoom;
        players.values().forEach(p -> p.setCurrentRoom(finalStartRoom));
      } else {
        System.err.println(
            "CRITICAL: No valid start room for players in session [" + sessionId + "]");
        return false;
      }

      System.out.println(
          "Case '"
              + caseFile.getTitle()
              + "' initialized in context for session ["
              + sessionId
              + "]");
      return true;
    } catch (Exception e) {
      System.err.println(
          "ERROR initializing case '"
              + caseFile.getTitle()
              + "' in context for session ["
              + sessionId
              + "]: "
              + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  // A synchronized wrapper for executing commands to ensure thread safety.
  public synchronized void handleCommand(Command command) {
    try {
      command.execute(new String[] {}, this);
    } catch (Exception e) {
      System.err.println(
          "Exception during command exec in session ["
              + parentSession.getSessionId()
              + "] for "
              + command.getPlayerId()
              + ": "
              + e.getMessage());
      e.printStackTrace();
      sendResponseToPlayer(
          command.getPlayerId(), new TextMessage("Internal server error processing command."));
    }
  }

  public synchronized void addPlayer(String playerId) {
    if (!players.containsKey(playerId)) {
      Detective newDetective = new Detective(playerId);
      players.put(playerId, newDetective);
      // If a player joins after a case is selected, they are placed in the correct starting room.
      if (selectedCase != null && selectedCase.getStartingRoom() != null) {
        Room startRoom = getRoomByName(selectedCase.getStartingRoom());
        if (startRoom == null && !rooms.isEmpty()) startRoom = rooms.values().iterator().next();
        if (startRoom != null) newDetective.setCurrentRoom(startRoom);
      }
      System.out.println(
          "Detective for player "
              + playerId
              + " added to context of session ["
              + parentSession.getSessionId()
              + "]");
    }
  }

  public synchronized void handlePlayerDisconnect(String playerId) {
    players.remove(playerId);
  }

  @Override
  public synchronized void addEntryToJournal(String entry, String playerId) {
    JournalEntryDTO newEntry = new JournalEntryDTO(entry, playerId, LocalDateTime.now());
    boolean added = journal.addEntry(newEntry.getFormattedEntry());
    if (added) {
      // The DTO is broadcast to ensure all clients receive the structured journal update.
      parentSession.broadcastToSession(newEntry, null);
      autoSaveGameState();
    }
  }

  @Override
  public synchronized void handleDeduceCommand(String playerId, String targetObjectName) {
    Detective detective = getPlayerDetective(playerId);
    if (detective == null) return;
    Room currentRoom = detective.getCurrentRoom();
    if (currentRoom == null) {
      sendResponseToPlayer(playerId, new TextMessage("Not in a room."));
      return;
    }

    if (deducedObjectsSession.contains(targetObjectName.toLowerCase())) {
      sendResponseToPlayer(
          playerId, new TextMessage("'" + targetObjectName + "' already deduced. Check journal."));
      return;
    }
    GameObject obj = currentRoom.getObject(targetObjectName);
    if (obj != null) {
      String clue = obj.getDeduce();
      if (clue != null && !clue.isBlank()) {
        deducedObjectsSession.add(targetObjectName.toLowerCase());
        deduceCountSession++;
        detective.incrementCaseDeduceCount(targetObjectName);
        addEntryToJournal("Deduced from " + targetObjectName + ": " + clue, playerId);
        sendResponseToPlayer(playerId, new TextMessage("Deduction: " + clue));
        sendResponseToPlayer(
            playerId,
            new TextMessage(
                "Session deductions: "
                    + deduceCountSession
                    + ". Your case deductions: "
                    + detective.getCaseDeduceCount()));
      } else {
        sendResponseToPlayer(
            playerId, new TextMessage("Nothing to deduce from " + targetObjectName + "."));
      }
    } else {
      sendResponseToPlayer(playerId, new TextMessage("No '" + targetObjectName + "' here."));
    }
  }

  @Override
  public synchronized void handlePlayerMovement(String playerId, Room oldRoom, Room newRoom) {
    if (oldRoom != null) {
      // Notifying the NpcManager of player movement allows NPCs to react, for example, by moving
      // into an empty room.
      npcManager.triggerNpcMovementChecks(oldRoom, this.players);
    }
  }

  @Override
  public synchronized void processStartCaseAttempt(String initiatingPlayerId) {
    String sessionId = parentSession.getSessionId();

    if (this.caseStarted) {
      sendResponseToPlayer(
          initiatingPlayerId, new TextMessage("The case investigation has already started."));
      return;
    }
    if (this.selectedCase == null) {
      sendResponseToPlayer(
          initiatingPlayerId, new TextMessage("Error: No case has been loaded for this session."));
      System.err.println(
          "Session [" + sessionId + "]: Attempt to start case when no case is selected.");
      return;
    }
    if (parentSession.getCurrentState() != GameSession.SessionState.READY_TO_START) {
      sendResponseToPlayer(
          initiatingPlayerId,
          new TextMessage(
              "The game is not yet ready to start (e.g., waiting for all players or case loading). Current state: "
                  + parentSession.getCurrentState()));
      return;
    }

    // Differentiates actions based on the player's role (Host vs. Guest).
    if (parentSession.isHost(initiatingPlayerId)) {
      System.out.println(
          "Session ["
              + parentSession.getSessionId()
              + "]: Case '"
              + selectedCase.getTitle()
              + "' officially started by HOST "
              + initiatingPlayerId);
      this.caseStarted = true;
      broadcastInitialCaseDetails();
    } else {
      String hostId = parentSession.getHostPlayerId();
      if (hostId != null) {
        sendResponseToPlayer(
            hostId,
            new TextMessage(
                initiatingPlayerId + " requests you to start the case. Type 'start case'."));
        sendResponseToPlayer(
            initiatingPlayerId,
            new TextMessage("Request sent to the host (" + hostId + ") to start the case."));
      } else {
        sendResponseToPlayer(
            initiatingPlayerId,
            new TextMessage("Error: Could not send start request (host not found)."));
      }
    }
  }

  // Sends the initial case details to all players, marking the official start of the investigation.
  private void broadcastInitialCaseDetails() {
    if (!this.caseStarted || this.selectedCase == null || this.taskList == null) {
      System.err.println(
          "SERVER ERROR: broadcastInitialCaseDetails called with inconsistent state for session "
              + parentSession.getSessionId());
      return;
    }

    List<String> playerIdsInSession = parentSession.getPlayerIds();
    if (playerIdsInSession.isEmpty()) return;

    // The initial broadcast consists of several pieces of information to set the scene.
    TextMessage introMessage =
        new TextMessage(
            "\n--- Case Introduction ---\n"
                + (selectedCase.getDescription() != null
                    ? selectedCase.getDescription()
                    : "No specific case description provided."));

    List<String> tasks = taskList.getTasks();
    StringBuilder taskText = new StringBuilder("\n--- Initial Tasks ---");
    if (tasks.isEmpty()) taskText.append("\nNo specific tasks listed yet.");
    else
      for (int i = 0; i < tasks.size(); i++)
        taskText.append(String.format("\n%d. %s", i + 1, tasks.get(i)));
    taskText.append("\n---------------------");
    TextMessage taskMessage = new TextMessage(taskText.toString());

    TextMessage startInvestigationMessage = new TextMessage("\nThe investigation begins!");
    Room startingRoom =
        players.values().stream().findFirst().map(Detective::getCurrentRoom).orElse(null);
    RoomDescriptionDTO startRoomDTO =
        (startingRoom != null) ? generateRoomDescriptionDTO(startingRoom, null) : null;
    TextMessage helpMessage = new TextMessage("\nType 'help' to see available commands.");

    for (String targetPlayerId : playerIdsInSession) {
      sendResponseToPlayer(targetPlayerId, introMessage);
      sendResponseToPlayer(targetPlayerId, taskMessage);
      sendResponseToPlayer(targetPlayerId, startInvestigationMessage);
      if (startRoomDTO != null) {
        sendResponseToPlayer(targetPlayerId, startRoomDTO);
      } else {
        sendResponseToPlayer(
            targetPlayerId, new TextMessage("Error displaying starting room information."));
      }
      sendResponseToPlayer(targetPlayerId, helpMessage);
    }
    // After broadcasting, the parent session's state is officially changed to ACTIVE.
    parentSession.setSessionState(GameSession.SessionState.ACTIVE);
  }

  @Override
  public synchronized void processFinalExamAttempt(String initiatingPlayerId) {
    examManager.processFinalExamAttempt(initiatingPlayerId);
  }

  @Override
  public synchronized void processExamAnswer(
      String hostPlayerId, int questionNumber, String answerText) {
    examManager.processExamAnswer(hostPlayerId, questionNumber, answerText);
    autoSaveGameState();
  }

  @Override
  public Detective getPlayerDetective(String playerId) {
    return players.get(playerId);
  }

  @Override
  public Optional<Suspect> findSuspect(String name) {
    return npcManager.findSuspect(name);
  }

  // A convenience method for case-insensitive room lookups.
  public Room getRoomByName(String name) {
    if (name == null) return null;
    for (Map.Entry<String, Room> entry : rooms.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
    }
    return null;
  }

  public Map<String, Room> getRooms() {
    return Collections.unmodifiableMap(rooms);
  }

  public Journal getJournal() {
    return journal;
  }

  @Override
  public List<JournalEntryDTO> getJournalEntries() {
    // This method converts the internal list of simple strings into a list of
    // structured DTOs for network transmission.
    return journal.getEntries().stream()
        .map(
            entryString -> {
              try {
                String timestampStr = entryString.substring(1, 9);
                int endOfPlayerId = entryString.indexOf(':', 10);
                if (endOfPlayerId == -1 || endOfPlayerId <= 11)
                  throw new IllegalArgumentException("Bad format");
                String playerIdPart = entryString.substring(11, endOfPlayerId);
                String textPart = entryString.substring(endOfPlayerId + 2);
                LocalDateTime entryTime =
                    LocalDateTime.now()
                        .withHour(Integer.parseInt(timestampStr.substring(0, 2)))
                        .withMinute(Integer.parseInt(timestampStr.substring(3, 5)))
                        .withSecond(Integer.parseInt(timestampStr.substring(6, 8)));
                return new JournalEntryDTO(textPart, playerIdPart, entryTime);
              } catch (Exception e) {
                // A fallback ensures that a malformed journal entry doesn't crash the server.
                return new JournalEntryDTO(entryString, "System", LocalDateTime.now());
              }
            })
        .sorted(Comparator.comparing(JournalEntryDTO::getTimestamp))
        .collect(Collectors.toList());
  }

  @Override
  public TaskList getTaskList() {
    return taskList;
  }

  @Override
  public DoctorWatson getWatson() {
    return npcManager.getWatson();
  }

  @Override
  public boolean isCaseStarted(String playerId_unused) {
    return caseStarted;
  }

  public CaseFile getSelectedCase() {
    return selectedCase;
  }

  public void addRoom(Room room) {
    this.rooms.put(room.getName(), room);
  }

  public void addSuspect(Suspect suspect) {
    npcManager.addSuspect(suspect);
  }

  @Override
  public RoomDescriptionDTO generateRoomDescriptionDTO(Room room, String requestingPlayerId) {
    if (room == null) return null;
    List<String> objectNames =
        room.getObjects().values().stream()
            .map(GameObject::getName)
            .sorted()
            .collect(Collectors.toList());

    List<String> occupantNames = new ArrayList<>();
    players.forEach(
        (pid, det) -> {
          if (room.equals(det.getCurrentRoom())) {
            // The name "You" is used for the requesting player to make the description more
            // personal.
            occupantNames.add(pid.equals(requestingPlayerId) ? "You (" + pid + ")" : pid);
          }
        });
    npcManager.getSuspects().stream()
        .filter(s -> room.equals(s.getCurrentRoom()))
        .forEach(s -> occupantNames.add(s.getName()));
    DoctorWatson watson = npcManager.getWatson();
    if (watson != null && room.equals(watson.getCurrentRoom())) {
      occupantNames.add("Dr. Watson");
    }
    Collections.sort(occupantNames);

    Map<String, String> exitMap =
        room.getNeighbors().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));

    return new RoomDescriptionDTO(
        room.getName(), room.getDescription(), objectNames, occupantNames, exitMap);
  }

  /**
   * Gathers all the necessary data from the current game state and packages it into a GameStateData
   * DTO for persistence.
   */
  public GameStateData getCurrentGameStateData(String sessionId, List<String> currentPlayerIds) {
    Map<String, String> playerLocs =
        players.entrySet().stream()
            .filter(entry -> currentPlayerIds.contains(entry.getKey()))
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e ->
                        e.getValue().getCurrentRoom() != null
                            ? e.getValue().getCurrentRoom().getName()
                            : "UnknownLocation"));

    Map<String, String> npcLocs = new HashMap<>();
    npcManager
        .getSuspects()
        .forEach(
            s ->
                npcLocs.put(
                    s.getName(),
                    s.getCurrentRoom() != null ? s.getCurrentRoom().getName() : "UnknownLocation"));
    DoctorWatson watson = npcManager.getWatson();
    if (watson != null) {
      npcLocs.put(
          "Dr. Watson",
          watson.getCurrentRoom() != null ? watson.getCurrentRoom().getName() : "UnknownLocation");
    }

    List<String> completedTasksList =
        (taskList != null && taskList.getTasks() != null)
            ? new ArrayList<>(taskList.getTasks())
            : List.of();
    return new GameStateData(
        sessionId,
        selectedCase != null ? selectedCase.getTitle() : "Unknown Case",
        currentPlayerIds,
        journal.getEntries(),
        deduceCountSession,
        new HashSet<>(deducedObjectsSession),
        playerLocs,
        npcLocs,
        completedTasksList,
        caseStarted);
  }

  /**
   * Rehydrates the game state from a loaded GameStateData object. This method re-initializes the
   * case to its base state from the original JSON file and then layers the saved progress on top.
   */
  public boolean applyLoadedState(GameStateData loadedData) {
    System.out.println(
        "Applying loaded state to context for session [" + parentSession.getSessionId() + "]");
    try {
      // 1. Find the original case file corresponding to the saved data.
      List<CaseFile> cases = server.extractors.CaseLoader.loadCases(casesDir);
      Optional<CaseFile> caseToLoad =
          cases.stream()
              .filter(c -> c.getTitle().equalsIgnoreCase(loadedData.getCaseTitle()))
              .findFirst();

      if (caseToLoad.isEmpty()) {
        System.err.println("Could not find matching case file for saved game state.");
        return false;
      }

      // 2. Re-initialize the world to a clean state.
      if (!initializeCase(caseToLoad.get())) {
        System.err.println("Failed to re-initialize base case from file during state load.");
        return false;
      }

      // 3. Layer the saved progress on top of the clean state.
      this.journal = new Journal();
      loadedData.getJournalEntries().forEach(journal::addEntry);

      this.deduceCountSession = loadedData.getDeduceCountGlobal();
      this.deducedObjectsSession = new HashSet<>(loadedData.getDeducedObjectsGlobal());
      this.caseStarted = loadedData.isCaseStarted();

      // 4. Restore character and NPC locations.
      for (String playerId : loadedData.getPlayerIds()) {
        if (!players.containsKey(playerId)) {
          addPlayer(playerId);
        }
        Detective pDet = players.get(playerId);
        if (pDet != null) {
          String roomName = loadedData.getPlayerLocations().get(playerId);
          Room pRoom = (roomName != null) ? getRoomByName(roomName) : null;
          if (pRoom != null) {
            pDet.setCurrentRoom(pRoom);
          }
        }
      }

      loadedData
          .getNpcLocations()
          .forEach(
              (npcName, roomName) -> {
                Room r = getRoomByName(roomName);
                if (r != null) {
                  if (npcName.equals("Dr. Watson")) {
                    DoctorWatson watson = npcManager.getWatson();
                    if (watson != null) watson.setCurrentRoom(r);
                  } else {
                    npcManager.findSuspect(npcName).ifPresent(s -> s.setCurrentRoom(r));
                  }
                }
              });

      System.out.println(
          "Successfully applied loaded state for session [" + parentSession.getSessionId() + "].");
      return true;

    } catch (Exception e) {
      System.err.println("An unexpected error occurred while applying loaded game state.");
      e.printStackTrace();
      return false;
    }
  }

  // A simple wrapper to send responses via the parent GameSession.
  @Override
  public void sendResponseToPlayer(String playerId, Serializable message) {
    parentSession.sendMessageToPlayer(playerId, message);
  }

  /**
   * Automatically saves the current state of the game to the database. This is triggered by key
   * events like adding a journal entry.
   */
  private void autoSaveGameState() {
    // Find the host player's ClientSession to get their user ID.
    ClientSession hostClient =
        sessionManager.getClientSessionByPlayerId(parentSession.getHostPlayerId());
    if (hostClient == null || hostClient.getAuthenticatedUser() == null) {
      // Cannot save if the host is not found or not logged in.
      return;
    }
    if (selectedCase == null) {
      // Cannot save if no case is active.
      return;
    }

    int ownerId = hostClient.getAuthenticatedUser().getId();
    String caseTitle = selectedCase.getTitle();

    // Get the current state data.
    GameStateData stateData =
        this.getCurrentGameStateData(parentSession.getSessionId(), parentSession.getPlayerIds());

    // Use the DAO to save the state.
    sessionManager.getGameSessionDAO().saveOrUpdateGameState(ownerId, caseTitle, stateData);
    System.out.println(
        "Auto-saved game state for user " + ownerId + " and case '" + caseTitle + "'.");
  }
}
