package server;

import JsonDTO.CaseFile;
import common.Commands.*;
import common.NetworkConstants;
import common.dto.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the lifecycle and state of a single game instance, from lobby creation to game
 * completion. It holds the players for one game and owns the corresponding GameContextServer that
 * contains the actual game world.
 */
public class GameSession {

  // The state machine defines the lifecycle of a single game session.
  public enum SessionState {
    CREATED,
    WAITING_FOR_PLAYER,
    LOADING_CASE,
    READY_TO_START,
    ACTIVE,
    FINAL_EXAM,
    ENDED_VICTORY,
    ENDED_FAILURE,
    ENDED_ABORTED
  }

  private final String sessionId;
  private final GameServer gameServer;
  private final String casesDir;
  private final GameContextServer gameContext;
  private final List<ClientSession> players =
      new ArrayList<>(NetworkConstants.MAX_PLAYERS_PER_GAME);
  private SessionState currentState = SessionState.CREATED;
  private String hostPlayerId = null;
  private CaseInfoDTO selectedCaseInfo = null;

  public GameSession(
      String sessionId, GameServer gameServer, String casesDir, GameSessionManager sessionManager) {
    this.sessionId = Objects.requireNonNull(sessionId);
    this.gameServer = Objects.requireNonNull(gameServer);
    this.casesDir = Objects.requireNonNull(casesDir);
    this.gameContext = new GameContextServer(this, casesDir, sessionManager);
  }

  public void setSelectedCaseInfo(CaseInfoDTO caseInfo) {
    if (currentState == SessionState.CREATED || currentState == SessionState.WAITING_FOR_PLAYER) {
      this.selectedCaseInfo = Objects.requireNonNull(caseInfo);
    }
  }

  // A synchronized method to safely add players to the session.
  public synchronized void addPlayer(ClientSession playerClientSession) {
    if (players.size() < NetworkConstants.MAX_PLAYERS_PER_GAME
        && players.stream()
            .noneMatch(p -> p.getPlayerId().equals(playerClientSession.getPlayerId()))) {
      players.add(playerClientSession);
      gameContext.addPlayer(playerClientSession.getPlayerId());
      // The first player to join a session is designated as the host.
      if (players.size() == 1) {
        this.hostPlayerId = playerClientSession.getPlayerId();
        setSessionState(SessionState.WAITING_FOR_PLAYER);
        System.out.println(
            "Player "
                + this.hostPlayerId
                + " is HOST. Session ["
                + sessionId
                + "] state: "
                + currentState);
      }
    }
  }

  // Kicks off the process of loading the case data into the game context.
  public void initializeSession() {
    if (currentState != SessionState.WAITING_FOR_PLAYER
        || players.size() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
      System.err.println(
          "Session ["
              + sessionId
              + "] initialize called in wrong state or not enough players. State: "
              + currentState);
      return;
    }

    if (selectedCaseInfo == null) {
      handleInitializationFailure("No case was selected for the session.");
      return;
    }

    setSessionState(SessionState.LOADING_CASE);
    broadcastToSession(
        new TextMessage(
            "All players have joined. Loading case: " + selectedCaseInfo.getTitle() + "..."),
        null);

    // The server authoritatively loads the case from its own file system to ensure consistency.
    Optional<CaseFile> caseToLoadOpt =
        server.extractors.CaseLoader.loadCases(casesDir).stream()
            .filter(
                cf ->
                    cf.getTitle() != null
                        && cf.getTitle().equalsIgnoreCase(selectedCaseInfo.getTitle()))
            .findFirst();

    if (caseToLoadOpt.isPresent()) {
      CaseFile loadedCase = caseToLoadOpt.get();

      if (gameContext.initializeCase(loadedCase)) {
        setSessionState(SessionState.READY_TO_START);
        String invitation = loadedCase.getInvitation();
        if (invitation != null && !invitation.isBlank()) {
          broadcastToSession(new TextMessage("\n" + invitation + "\n"), null);
        }

        String msg = "Case '" + selectedCaseInfo.getTitle() + "' is ready.";
        sendMessageToPlayer(
            hostPlayerId, new TextMessage(msg + " Please type 'start case' to begin."));
        players.stream()
            .filter(p -> !p.getPlayerId().equals(hostPlayerId))
            .forEach(
                guest ->
                    sendMessageToPlayer(
                        guest.getPlayerId(),
                        new TextMessage(msg + " Waiting for the host to start the game.")));

        System.out.println("Session [" + sessionId + "] is loaded and ready to start.");

      } else {
        handleInitializationFailure(
            "The game context failed to initialize the case '"
                + selectedCaseInfo.getTitle()
                + "'. The case file might be corrupted.");
      }
    } else {
      handleInitializationFailure(
          "The server does not have the file for case '" + selectedCaseInfo.getTitle() + "'.");
    }
  }

  private void handleInitializationFailure(String errorMessage) {
    setSessionState(SessionState.ENDED_ABORTED);
    broadcastToSession(new TextMessage("Error: " + errorMessage + " Session aborted."), null);
    System.err.println("Session [" + sessionId + "] init failed: " + errorMessage);
  }

  /**
   * The main message router for a session. It directs incoming commands and chat messages to the
   * appropriate handlers based on the current game state.
   */
  public void processMessage(String senderPlayerId, Object message) {
    if (currentState.name().startsWith("ENDED_")) {
      sendMessageToPlayer(senderPlayerId, new TextMessage("This game session has already ended."));
      return;
    }

    if (message instanceof Command) {
      Command command = (Command) message;
      command.setPlayerId(senderPlayerId);

      // This logic block routes special, state-changing commands to the context.
      if (command instanceof StartCaseCommand) {
        gameContext.processStartCaseAttempt(senderPlayerId);
      } else if (command instanceof FinalExamCommand) {
        gameContext.processFinalExamAttempt(senderPlayerId);
      } else if (command instanceof SubmitExamAnswerCommand) {
        if (currentState == SessionState.FINAL_EXAM && isHost(senderPlayerId)) {
          gameContext.processExamAnswer(
              senderPlayerId,
              ((SubmitExamAnswerCommand) command).getQuestionNumber(),
              ((SubmitExamAnswerCommand) command).getAnswerText());
        } else {
          sendMessageToPlayer(senderPlayerId, new TextMessage("Cannot submit exam answer now."));
        }
      } else if (isCommandAllowedInActiveState(command)) {
        if (currentState == SessionState.ACTIVE || currentState == SessionState.FINAL_EXAM) {
          gameContext.handleCommand(command);
        } else {
          sendMessageToPlayer(
              senderPlayerId,
              new TextMessage(
                  "Game is not active. Cannot perform: " + command.getClass().getSimpleName()));
        }
      } else if (command instanceof HelpCommand) {
        gameContext.handleCommand(command);
      } else {
        sendMessageToPlayer(
            senderPlayerId,
            new TextMessage(
                "Command '"
                    + command.getClass().getSimpleName()
                    + "' not allowed in current state: "
                    + currentState));
      }

    } else if (message instanceof ChatMessage) {
      if (currentState != SessionState.CREATED
          && currentState != SessionState.LOADING_CASE
          && !currentState.name().startsWith("ENDED_")) {
        ChatMessage chat = (ChatMessage) message;
        broadcastToSession(new ChatMessage(senderPlayerId, chat.getMessage()), null);
      } else {
        sendMessageToPlayer(
            senderPlayerId,
            new TextMessage("Chat not available in current session state (" + currentState + ")."));
      }
    } else {
      System.err.println(
          "Session ["
              + sessionId
              + "] received unknown message type: "
              + message.getClass().getName());
      sendMessageToPlayer(
          senderPlayerId, new TextMessage("Error: Unknown message type received by session."));
    }
  }

  // A helper to determine if a command is a standard in-game action.
  private boolean isCommandAllowedInActiveState(Command command) {
    return !(command instanceof StartCaseCommand
        || command instanceof FinalExamCommand
        || command instanceof SubmitExamAnswerCommand
        || command instanceof HelpCommand);
  }

  /**
   * Handles the logic for a player disconnecting, which differs significantly depending on whether
   * the player was the host or a guest.
   *
   * @return true if the session should be terminated as a result.
   */
  public synchronized boolean handlePlayerDisconnect(String playerId) {
    Optional<ClientSession> playerToRemoveOpt =
        players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst();

    if (playerToRemoveOpt.isEmpty()) {
      return false;
    }

    players.remove(playerToRemoveOpt.get());
    gameContext.handlePlayerDisconnect(playerId);

    System.out.println(
        "Player "
            + playerId
            + " removed from session ["
            + sessionId
            + "]. Remaining: "
            + players.size());

    // If the host disconnects, the game is over for everyone.
    if (isHost(playerId)) {
      System.out.println(
          "Host has disconnected from session [" + sessionId + "]. Terminating session.");
      broadcastToSession(
          new TextMessage("The host has disconnected. The game session has ended."), null);
      return true;
    } else {
      // If a guest disconnects, the game continues for the remaining players.
      System.out.println(
          "Guest "
              + playerId
              + " has disconnected from session ["
              + sessionId
              + "]. Game continues.");
      broadcastToSession(new TextMessage("Player " + playerId + " has left the game."), playerId);
      return false;
    }
  }

  public GameStateData getGameStateForSaving() {
    if (gameContext == null
        || currentState == SessionState.ENDED_ABORTED
        || currentState == SessionState.CREATED
        || currentState == SessionState.LOADING_CASE) {
      return null;
    }
    return gameContext.getCurrentGameStateData(sessionId, getPlayerIds());
  }

  public void restoreGameState(GameStateData loadedData) {
    if (loadedData == null || !loadedData.getGameSessionId().equals(this.sessionId)) {
      handleInitializationFailure("Invalid or mismatched save data for restore.");
      return;
    }
    if (gameContext.applyLoadedState(loadedData)) {
      setSessionState(
          gameContext.isCaseStarted(null) ? SessionState.ACTIVE : SessionState.READY_TO_START);
      if (!loadedData.getPlayerIds().isEmpty()) {
        String savedHost = loadedData.getPlayerIds().get(0);
        if (this.hostPlayerId == null || !loadedData.getPlayerIds().contains(this.hostPlayerId)) {
          this.hostPlayerId = savedHost;
        }
      }
      System.out.println(
          "Session ["
              + sessionId
              + "] restored. State: "
              + currentState
              + ". Host: "
              + this.hostPlayerId);
      broadcastToSession(new TextMessage("Game session restored from save."), null);
    } else {
      handleInitializationFailure("Failed to apply loaded game state during restore.");
    }
  }

  public void sendMessageToPlayer(String targetPlayerId, Serializable message) {
    players.stream()
        .filter(p -> p.getPlayerId().equals(targetPlayerId))
        .findFirst()
        .ifPresent(playerSession -> gameServer.queueWrite(playerSession.getChannel(), message));
  }

  public void broadcastToSession(Serializable message, String senderPlayerIdToExclude) {
    for (ClientSession player : players) {
      if (senderPlayerIdToExclude == null
          || !player.getPlayerId().equals(senderPlayerIdToExclude)) {
        gameServer.queueWrite(player.getChannel(), message);
      }
    }
  }

  // A synchronized setter to ensure thread-safe state transitions for the session.
  public synchronized void setSessionState(SessionState newState) {
    if (this.currentState != newState) {
      System.out.println(
          "Session ["
              + sessionId
              + "] state changing from "
              + this.currentState
              + " to "
              + newState);
      this.currentState = newState;
    }
  }

  public String getSessionId() {
    return sessionId;
  }

  public SessionState getCurrentState() {
    return currentState;
  }

  public GameContextServer getGameContext() {
    return gameContext;
  }

  public CaseInfoDTO getSelectedCaseInfo() {
    return selectedCaseInfo;
  }

  public String getHostPlayerId() {
    return hostPlayerId;
  }

  public int getPlayerCount() {
    return players.size();
  }

  public List<String> getPlayerIds() {
    return players.stream().map(ClientSession::getPlayerId).collect(Collectors.toList());
  }

  public boolean isHost(String playerId) {
    return Objects.equals(playerId, hostPlayerId);
  }

  /**
   * Finalizes the game session, setting its end state and triggering cleanup.
   *
   * @param victory true if the game was won, false otherwise.
   */
  public void setGameCompleted(boolean victory) {
    if (currentState.name().startsWith("ENDED_")) {
      return;
    }

    SessionState finalState = victory ? SessionState.ENDED_VICTORY : SessionState.ENDED_FAILURE;
    setSessionState(finalState);

    String outcomeMessage =
        victory
            ? "Congratulations! You have solved the case!"
            : "Unfortunately, you failed to solve the case this time.";

    broadcastToSession(new TextMessage(outcomeMessage + " The session will end shortly."), null);

    // After notifying players, the session tells the GameSessionManager to remove it
    // from all tracking lists, effectively cleaning up the completed game.
    if (gameServer != null && gameServer.getGameSessionManager() != null) {
      gameServer.getGameSessionManager().removeSession(this.sessionId);
    }
  }
}
