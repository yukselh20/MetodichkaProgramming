package server;

import JsonDTO.CaseFile;
import common.Commands.*;
import common.NetworkConstants;
import common.dto.*;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.SQLClasses.model.User;

// I created this to manage the lifecycle and state of a single game instance, from lobby creation
// to completion. It holds the players for one game and owns the GameContextServer.
public class GameSession {

  private static final Logger logger = LoggerFactory.getLogger(GameSession.class);

  // This state machine defines the lifecycle of a single game session.
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

  // I synchronized this method to safely add players to the session.
  public synchronized void addPlayer(ClientSession playerClientSession) {
    if (players.size() < NetworkConstants.MAX_PLAYERS_PER_GAME
        && players.stream()
            .noneMatch(p -> p.getPlayerId().equals(playerClientSession.getPlayerId()))) {
      players.add(playerClientSession);
      gameContext.addPlayer(playerClientSession.getPlayerId());
      // I designate the first player to join a session as the host.
      if (players.size() == 1) {
        this.hostPlayerId = playerClientSession.getPlayerId();
        setSessionState(SessionState.WAITING_FOR_PLAYER);
        logger.info(
            "Player {} is HOST. Session [{}] state: {}",
            this.hostPlayerId,
            sessionId,
            currentState);
      }
    }
  }

  // This kicks off the process of loading the case data into the game context.
  public void initializeSession() {
    if (currentState != SessionState.WAITING_FOR_PLAYER
        || players.size() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
      logger.error(
          "Session [{}] initialize called in wrong state or not enough players. State: {}",
          sessionId,
          currentState);
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

    // I authoritatively load the case from the server's own file system to ensure consistency.
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

        logger.info("Session [{}] is loaded and ready to start.", sessionId);

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
    logger.error("Session [{}] init failed: {}", sessionId, errorMessage);
  }

  public String getUsernameFromPlayerId(String playerId) {
    return players.stream()
        .filter(p -> p.getPlayerId().equals(playerId))
        .findFirst()
        .map(ClientSession::getAuthenticatedUser)
        .map(User::getUsername)
        .orElse(playerId); // Fallback to the ID if user/username not found
  }

  // This is my main message router for a session. It directs incoming commands and chat messages
  // to the appropriate handlers based on the current game state.
  public void processMessage(String senderPlayerId, Object message) {
    if (currentState.name().startsWith("ENDED_")) {
      sendMessageToPlayer(senderPlayerId, new TextMessage("This game session has already ended."));
      return;
    }

    if (message instanceof Command) {
      Command command = (Command) message;
      command.setPlayerId(senderPlayerId);

      // I use this block to route special, state-changing commands to the context.
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

        ChatMessage incomingChat = (ChatMessage) message;

        Optional<ClientSession> senderSessionOpt =
            players.stream().filter(p -> p.getPlayerId().equals(senderPlayerId)).findFirst();

        if (senderSessionOpt.isPresent()) {
          ClientSession senderClient = senderSessionOpt.get();
          User authenticatedUser = senderClient.getAuthenticatedUser();

          String displayName =
              (authenticatedUser != null) ? authenticatedUser.getUsername() : senderPlayerId;

          ChatMessage broadcastMessage = new ChatMessage(displayName, incomingChat.getMessage());
          broadcastToSession(broadcastMessage, null);

        } else {
          logger.warn(
              "Received chat message from player '{}' who is not in this session '{}'.",
              senderPlayerId,
              sessionId);
        }
      } else {
        sendMessageToPlayer(
            senderPlayerId,
            new TextMessage("Chat not available in current session state (" + currentState + ")."));
      }
    } else {
      logger.error(
          "Session [{}] received unknown message type: {}",
          sessionId,
          message.getClass().getName());
      sendMessageToPlayer(
          senderPlayerId, new TextMessage("Error: Unknown message type received by session."));
    }
  }

  // This helper determines if a command is a standard in-game action.
  private boolean isCommandAllowedInActiveState(Command command) {
    return !(command instanceof StartCaseCommand
        || command instanceof FinalExamCommand
        || command instanceof SubmitExamAnswerCommand
        || command instanceof HelpCommand);
  }

  // Handles a player disconnecting. The logic differs significantly depending
  // on whether the player was the host or a guest. Returns true if the session
  // should be terminated as a result.
  public synchronized boolean handlePlayerDisconnect(String playerId) {
    Optional<ClientSession> playerToRemoveOpt =
        players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst();

    if (playerToRemoveOpt.isEmpty()) {
      return false;
    }

    ClientSession disconnectedClient = playerToRemoveOpt.get();
    String username = getUsernameFromPlayerId(disconnectedClient.getPlayerId());

    players.remove(disconnectedClient);
    gameContext.handlePlayerDisconnect(playerId);

    logger.info(
        "Player {} ({}) removed from session [{}]. Remaining: {}",
        username,
        playerId,
        sessionId,
        players.size());

    // If the host disconnects, the game is over for everyone.
    if (isHost(playerId)) {
      logger.warn(
          "Host {} has disconnected from session [{}]. Terminating session.", username, sessionId);
      broadcastToSession(
          new TextMessage(
              "The host, " + username + ", has disconnected. The game session has ended."),
          null);
      return true;
    } else {
      // If a guest disconnects, the game continues for the remaining players.
      logger.info(
          "Guest {} has disconnected from session [{}]. Game continues.", username, sessionId);
      broadcastToSession(new TextMessage(username + " has left the game."), playerId);
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
      logger.info(
          "Session [{}] restored. State: {}. Host: {}", sessionId, currentState, this.hostPlayerId);
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
      if (!player.getPlayerId().equals(senderPlayerIdToExclude)) {
        gameServer.queueWrite(player.getChannel(), message);
      }
    }
  }

  // I made this setter synchronized to ensure thread-safe state transitions for the session.
  public synchronized void setSessionState(SessionState newState) {
    if (this.currentState != newState) {
      logger.info(
          "Session [{}] state changing from {} to {}", sessionId, this.currentState, newState);
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

  // This finalizes the game session, setting its end state and triggering cleanup.
  // The 'victory' parameter determines if the game was won or lost.
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

    // After notifying players, I tell the GameSessionManager to remove this session,
    // effectively cleaning up the completed game.
    if (gameServer != null && gameServer.getGameSessionManager() != null) {
      gameServer.getGameSessionManager().removeSession(this.sessionId);
    }
  }

  public List<ClientSession> getPlayers() {
    return Collections.unmodifiableList(players);
  }
}
