package server;

import common.Commands.*;
import common.NetworkConstants;
import common.dto.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.SQLClasses.auth.PasswordHasher;
import server.SQLClasses.database.GameSessionDAO;
import server.SQLClasses.database.UserDAO;
import server.SQLClasses.model.User;

// This as the master manager for all game sessions. It handles the client lobby,
// matchmaking, creation and joining of games, and routing initial messages before a client
// is assigned to a specific GameSession.
public class GameSessionManager {

  private static final Logger logger = LoggerFactory.getLogger(GameSessionManager.class);

  private final GameServer gameServer;
  private final String casesDir;
  private final Map<String, CaseInfoDTO> uniqueAvailableCases = new ConcurrentHashMap<>();
  private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
  private final Queue<ClientSession> lobby = new ConcurrentLinkedQueue<>();
  private final Map<String, GameSession> pendingPublicGames = new ConcurrentHashMap<>();
  private final Map<String, String> privateGameCodes = new ConcurrentHashMap<>();
  private final SecureRandom codeGenerator = new SecureRandom();
  private final UserDAO userDAO;
  private final GameSessionDAO gameSessionDAO;

  private final Map<Integer, String> loggedInUserToPlayerId = new ConcurrentHashMap<>();

  public GameSessionManager(GameServer gameServer, String casesDir) {
    this.gameServer = Objects.requireNonNull(gameServer);
    this.casesDir = Objects.requireNonNull(casesDir);
    this.userDAO = new UserDAO();
    this.gameSessionDAO = new GameSessionDAO();
    logger.info("GameSessionManager initialized.");
  }

  public synchronized void handleNewClientConnection(ClientSession newClient) {
    logger.info(
        "SessionManager: New client {}. Waiting for them to log in.", newClient.getPlayerId());
    gameServer.queueWrite(
        newClient.getChannel(), new TextMessage("Welcome! Please log in or register to continue."));
  }

  // This is my main router for incoming messages from a client. If the client is in a game,
  // I pass the message to their GameSession. If they are in the lobby, I handle it here.
  public void routeMessage(ClientSession senderSession, Object message) {
    String gameSessionId = senderSession.getGameSessionId();
    if (gameSessionId != null) {
      GameSession targetSession = activeSessions.get(gameSessionId);
      if (targetSession != null) {
        targetSession.processMessage(senderSession.getPlayerId(), message);
      } else {
        // This handles cases where a client thinks it's in a session that no longer exists.
        logger.error(
            "Client {} sent a message for an unknown session ID: {}",
            senderSession.getPlayerId(),
            gameSessionId);
        gameServer.queueWrite(
            senderSession.getChannel(), new TextMessage("Error: Your game session ended."));
        senderSession.setGameSessionId(null);
        handleNewClientConnection(senderSession);
      }
    } else {
      handleLobbyOrSetupMessage(senderSession, message);
    }
  }

  private String getUsernameFromPlayerId(String playerId) {
    ClientSession client = getClientSessionByPlayerId(playerId);
    if (client != null && client.getAuthenticatedUser() != null) {
      return client.getAuthenticatedUser().getUsername();
    }
    // Fallback to the player ID if the user isn't found or authenticated.
    return playerId;
  }

  // This is my synchronized router for all messages from clients who are not yet in an active game.
  private synchronized void handleLobbyOrSetupMessage(ClientSession senderSession, Object message) {
    logger.debug("SessionManager: Received message of type {}", message.getClass().getSimpleName());

    if (message instanceof RegisterCommand cmd) {
      logger.debug("SessionManager: Identified RegisterCommand. Calling handleRegister...");
      handleRegister(senderSession, cmd.getUsername(), cmd.getPassword());
      return;
    }

    if (message instanceof LoginCommand cmd) {
      handleLogin(senderSession, cmd.getUsername(), cmd.getPassword());
      return;
    }

    // Check for authentication for all subsequent lobby actions.
    if (senderSession.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage("Error: You must be logged in to perform this action."));
      return;
    }

    if (message instanceof ClientCaseListDTO) {
      registerClientCases((ClientCaseListDTO) message);
      sendAvailableCases(senderSession);
    } else if (message instanceof RequestCaseListCommand) {
      sendAvailableCases(senderSession);
    } else if (message instanceof HostGameCommand cmd) {
      handleHostRequest(senderSession, cmd.getCaseTitle(), cmd.isPublic());
    } else if (message instanceof ListPublicGamesCommand) {
      handleListPublicGames(senderSession);
    } else if (message instanceof JoinPublicGameCommand cmd) {
      handleJoinPublicGame(senderSession, cmd.getTargetGameSessionId());
    } else if (message instanceof JoinPrivateGameCommand cmd) {
      handleJoinPrivateGame(senderSession, cmd.getPrivateCode());
    } else if (message instanceof ChatMessage chat && lobby.contains(senderSession)) {
      ChatMessage lobbyChat =
          new ChatMessage("[Lobby] " + senderSession.getPlayerId(), chat.getMessage());
      lobby.stream()
          .filter(cs -> !cs.getPlayerId().equals(senderSession.getPlayerId()))
          .forEach(cs -> gameServer.queueWrite(cs.getChannel(), lobbyChat));
    } else {
      // This is the catch-all for any other message type.
      logger.error(
          "SessionManager: Received unhandled message type '{}' from player {} in lobby.",
          message.getClass().getSimpleName(),
          senderSession.getPlayerId());
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage("Error: Invalid command for your current state."));
    }
  }

  // This aggregates case information from all connected clients into a single, unique list of
  // available cases.
  private synchronized void registerClientCases(ClientCaseListDTO caseListDTO) {
    if (caseListDTO == null || caseListDTO.getCases() == null) {
      return;
    }

    boolean changed = false;
    for (CaseInfoDTO clientCase : caseListDTO.getCases()) {
      if (clientCase.getTitle() != null && !clientCase.getTitle().isBlank()) {
        // I use a lower-case key for case-insensitive matching of case titles.
        String key = clientCase.getTitle().toLowerCase();
        if (!uniqueAvailableCases.containsKey(key)) {
          uniqueAvailableCases.put(key, clientCase);
          changed = true;
        }
      }
    }

    if (changed) {
      logger.info(
          "Unique case registry updated. Total unique cases: {}", uniqueAvailableCases.size());
    }
  }

  private void sendAvailableCases(ClientSession client) {
    List<CaseInfoDTO> sortedCases =
        uniqueAvailableCases.values().stream()
            .sorted(Comparator.comparing(CaseInfoDTO::getTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

    logger.info(
        "SessionManager: Sending available cases list ({} cases) to {}",
        sortedCases.size(),
        client.getPlayerId());
    gameServer.queueWrite(client.getChannel(), new AvailableCasesDTO(sortedCases));
  }

  // This is how I handle a registration request from a client.
  private void handleRegister(ClientSession senderSession, String username, String password) {
    if (senderSession.getAuthenticatedUser() != null) {
      gameServer.queueWrite(
          senderSession.getChannel(), new TextMessage("Error: You are already logged in."));
      return;
    }

    // Basic validation for username and password length.
    if (username.length() < 3 || password.length() < 4) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage(
              "Error: Username must be at least 3 characters and password at least 4."));
      return;
    }

    logger.debug("handleRegister: Calling userDAO.addUser for user '{}'...", username);
    Optional<User> newUser = userDAO.addUser(username, password);
    logger.debug("handleRegister: userDAO.addUser has returned.");

    if (newUser.isPresent()) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage(
              "Registration successful. You can now log in with 'login "
                  + username
                  + " [password]'"));
      logger.info("New user registered: {}", username);
    } else {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage("Error: Username '" + username + "' is already taken."));
    }
  }

  // This handles a login request from a client, enforcing my single-session policy.
  private void handleLogin(ClientSession senderSession, String username, String password) {
    if (senderSession.getAuthenticatedUser() != null) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage(
              "Error: You are already logged in as "
                  + senderSession.getAuthenticatedUser().getUsername()));
      return;
    }

    Optional<User> userOpt = userDAO.findUserByUsername(username);

    if (userOpt.isEmpty()) {
      gameServer.queueWrite(
          senderSession.getChannel(), new TextMessage("Error: Invalid username or password."));
      return;
    }

    User user = userOpt.get();

    if (loggedInUserToPlayerId.containsKey(user.getId())) {
      logger.warn(
          "Login rejected for user '{}': already logged in under session {}",
          username,
          loggedInUserToPlayerId.get(user.getId()));
      gameServer.queueWrite(
          senderSession.getChannel(), new TextMessage("Error: This user is already logged in."));
      return;
    }

    Optional<String> storedHashOpt = userDAO.getPasswordHashForUser(username);

    if (storedHashOpt.isPresent()) {
      boolean passwordMatches = PasswordHasher.verify(password, storedHashOpt.get());

      if (passwordMatches) {
        senderSession.setAuthenticatedUser(user);
        loggedInUserToPlayerId.put(user.getId(), senderSession.getPlayerId());

        gameServer.queueWrite(
            senderSession.getChannel(),
            new TextMessage("Login successful. Welcome, " + user.getUsername() + "!"));
        logger.info(
            "Player {} logged in as user {} (ID: {}).",
            senderSession.getPlayerId(),
            user.getUsername(),
            user.getId());

      } else {
        gameServer.queueWrite(
            senderSession.getChannel(), new TextMessage("Error: Invalid username or password."));
      }
    } else {
      gameServer.queueWrite(
          senderSession.getChannel(), new TextMessage("Error: Could not verify password."));
    }
  }

  // This is my core logic for handling a host request. It validates the request,
  // creates a new GameSession, and sets up the lobby.
  private synchronized void handleHostRequest(
      ClientSession hostClient, String caseTitle, boolean isPublic) {
    if (hostClient.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          hostClient.getChannel(), new TextMessage("Error: You must be logged in to host a game."));
      return;
    }

    if (hostClient.getGameSessionId() != null) {
      gameServer.queueWrite(
          hostClient.getChannel(),
          new HostGameResponseDTO(false, "You are already in a game or lobby.", null, null));
      return;
    }

    CaseInfoDTO selectedCaseInfo = uniqueAvailableCases.get(caseTitle.toLowerCase());
    if (selectedCaseInfo == null) {
      gameServer.queueWrite(
          hostClient.getChannel(),
          new HostGameResponseDTO(
              false,
              "Error: Case '" + caseTitle + "' is not available on this server.",
              null,
              null));
      return;
    }

    String newSessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
    GameSession newSession = new GameSession(newSessionId, gameServer, casesDir, this);
    newSession.setSelectedCaseInfo(selectedCaseInfo);
    activeSessions.put(newSessionId, newSession);
    hostClient.setGameSessionId(newSessionId);
    newSession.addPlayer(hostClient);
    lobby.remove(hostClient);

    String privateCode = null;
    String responseMessage;
    if (isPublic) {
      pendingPublicGames.put(newSessionId, newSession);
      responseMessage =
          "Public lobby for '" + caseTitle + "' created. Waiting for another player...";
    } else {
      privateCode = generatePrivateCode();
      privateGameCodes.put(privateCode, newSession.getSessionId());
      responseMessage = "Private lobby for '" + caseTitle + "' created. Code: " + privateCode;
    }

    HostGameResponseDTO response =
        new HostGameResponseDTO(true, responseMessage, privateCode, newSessionId);
    gameServer.queueWrite(hostClient.getChannel(), response);

    logger.info(
        "SessionManager: Player {} created session {} for case '{}'.",
        hostClient.getPlayerId(),
        newSessionId,
        caseTitle);
  }

  // I created this to generate a unique 4-digit code for private games.
  private synchronized String generatePrivateCode() {
    String code;
    do {
      code = String.format("%04d", codeGenerator.nextInt(10000));
    } while (privateGameCodes.containsKey(code));
    return code;
  }

  private void handleListPublicGames(ClientSession client) {
    List<PublicGameInfoDTO> publicGames =
        pendingPublicGames.values().stream()
            .filter(s -> s.getPlayerCount() < NetworkConstants.MAX_PLAYERS_PER_GAME)
            .map(
                session -> {
                  String hostUsername = getUsernameFromPlayerId(session.getHostPlayerId());
                  return new PublicGameInfoDTO(
                      hostUsername,
                      session.getSelectedCaseInfo().getTitle(),
                      session.getSessionId());
                })
            .sorted(
                Comparator.comparing(
                    PublicGameInfoDTO::getCaseTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    gameServer.queueWrite(client.getChannel(), new PublicGamesListDTO(publicGames));
  }

  private synchronized void handleJoinPublicGame(
      ClientSession joiningClient, String targetSessionId) {
    if (joiningClient.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You must be logged in to join a game."));
      return;
    }

    if (joiningClient.getGameSessionId() != null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You are already in a game or lobby."));
      return;
    }

    GameSession targetSession = pendingPublicGames.get(targetSessionId);

    if (targetSession != null
        && targetSession.getPlayerCount() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
      pendingPublicGames.remove(targetSessionId);
      joiningClient.setGameSessionId(targetSessionId);
      targetSession.addPlayer(joiningClient);
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(
              true,
              "Joined public game '" + targetSession.getSelectedCaseInfo().getTitle() + "'!",
              targetSessionId));
      String joiningUsername = getUsernameFromPlayerId(joiningClient.getPlayerId());

      // I'll notify the existing players (the host) that a new player has joined, using their
      // username.
      targetSession.broadcastToSession(
          new LobbyUpdateDTO(
              joiningUsername + " joined! Starting game...", targetSession.getPlayerIds()),
          joiningClient.getPlayerId());
      targetSession.initializeSession();
      lobby.remove(joiningClient);
    } else {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "Game lobby not found or full.", null));
    }
  }

  private synchronized void handleJoinPrivateGame(ClientSession joiningClient, String privateCode) {
    if (joiningClient.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You must be logged in to join a game."));
      return;
    }

    if (joiningClient.getGameSessionId() != null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "You are already in a game or lobby.", null));
      return;
    }

    String targetSessionId = privateGameCodes.get(privateCode);

    if (targetSessionId != null) {
      GameSession targetSession = activeSessions.get(targetSessionId);

      if (targetSession != null
          && targetSession.getPlayerCount() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
        privateGameCodes.remove(privateCode);
        joiningClient.setGameSessionId(targetSessionId);
        targetSession.addPlayer(joiningClient);
        gameServer.queueWrite(
            joiningClient.getChannel(),
            new JoinGameResponseDTO(
                true,
                "Joined private game '" + targetSession.getSelectedCaseInfo().getTitle() + "'!",
                targetSessionId));
        String joiningUsername = getUsernameFromPlayerId(joiningClient.getPlayerId());

        // I'll notify the existing players (the host) that a new player has joined.
        targetSession.broadcastToSession(
            new LobbyUpdateDTO(
                joiningUsername + " joined! Starting game...", targetSession.getPlayerIds()),
            joiningClient.getPlayerId());
        targetSession.initializeSession();
        lobby.remove(joiningClient);
      } else {
        gameServer.queueWrite(
            joiningClient.getChannel(),
            new JoinGameResponseDTO(false, "Private game full or code expired.", null));
      }
    } else {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "Invalid private game code.", null));
    }
  }

  // This handles all logic for a client disconnecting, including logging them out.
  public synchronized void handleClientDisconnect(ClientSession disconnectedClient) {
    // I log the user out from my centralized active user map.
    User authenticatedUser = disconnectedClient.getAuthenticatedUser();
    if (authenticatedUser != null) {
      loggedInUserToPlayerId.remove(authenticatedUser.getId());
      logger.info(
          "User '{}' (ID: {}) has been logged out from session {}.",
          authenticatedUser.getUsername(),
          authenticatedUser.getId(),
          disconnectedClient.getPlayerId());
    }

    lobby.remove(disconnectedClient);
    String gameSessionId = disconnectedClient.getGameSessionId();
    if (gameSessionId == null) {
      return;
    }

    GameSession session = activeSessions.get(gameSessionId);
    if (session == null) {
      session = pendingPublicGames.get(gameSessionId);
    }

    if (session != null) {
      boolean sessionShouldBeTerminated =
          session.handlePlayerDisconnect(disconnectedClient.getPlayerId());
      if (sessionShouldBeTerminated) {
        removeSession(gameSessionId);
      }
    }
  }

  public synchronized void removeSession(String sessionId) {
    if (sessionId == null) return;
    GameSession removedSession = activeSessions.remove(sessionId);
    if (removedSession != null) {
      logger.info("SessionManager: Removing ended session {}", sessionId);
      pendingPublicGames.remove(sessionId);
      privateGameCodes.entrySet().removeIf(entry -> entry.getValue().equals(sessionId));
      removedSession
          .getPlayerIds()
          .forEach(
              playerId -> {
                ClientSession client = getClientSessionByPlayerId(playerId);
                if (client != null) {
                  client.setGameSessionId(null);
                }
              });
    }
  }

  public GameSessionDAO getGameSessionDAO() {
    return this.gameSessionDAO;
  }

  public ClientSession getClientSessionByPlayerId(String playerId) {
    for (ClientSession session : gameServer.getClients().values()) {
      if (session.getPlayerId().equals(playerId)) {
        return session;
      }
    }
    return null;
  }
}
