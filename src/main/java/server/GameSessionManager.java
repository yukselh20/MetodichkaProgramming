package server;

import common.Commands.*;
import common.NetworkConstants;
import common.dto.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import server.SQLClasses.auth.PasswordHasher;
import server.SQLClasses.database.GameSessionDAO;
import server.SQLClasses.database.UserDAO;
import server.SQLClasses.model.User;

/**
 * The master manager for all game sessions. It handles the client lobby, matchmaking, creation and
 * joining of games, and routing initial messages before a client is assigned to a specific
 * GameSession.
 */
public class GameSessionManager {

  private final GameServer gameServer;
  private final String casesDir;

  // These concurrent collections are essential for managing a thread-safe state
  // across multiple clients and game sessions.
  private final Map<String, CaseInfoDTO> uniqueAvailableCases = new ConcurrentHashMap<>();
  private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
  private final Queue<ClientSession> lobby = new ConcurrentLinkedQueue<>();
  private final Map<String, GameSession> pendingPublicGames = new ConcurrentHashMap<>();
  private final Map<String, String> privateGameCodes = new ConcurrentHashMap<>();
  private final SecureRandom codeGenerator = new SecureRandom();
  private final UserDAO userDAO;
  private final GameSessionDAO gameSessionDAO;

  public GameSessionManager(GameServer gameServer, String casesDir) {
    this.gameServer = Objects.requireNonNull(gameServer);
    this.casesDir = Objects.requireNonNull(casesDir);
    this.userDAO = new UserDAO();
    this.gameSessionDAO = new GameSessionDAO();
    System.out.println("GameSessionManager initialized.");
  }

  public synchronized void handleNewClientConnection(ClientSession newClient) {
    System.out.println(
        "SessionManager: New client " + newClient.getPlayerId() + ". Waiting for their case list.");
    gameServer.queueWrite(
        newClient.getChannel(),
        new TextMessage("Welcome! Connected to server. Sending your case list..."));
  }

  /**
   * Routes an incoming message from a client. If the client is in a game, the message is passed to
   * their GameSession. If they are in the lobby, it's handled by the lobby message processor.
   */
  public void routeMessage(ClientSession senderSession, Object message) {
    String gameSessionId = senderSession.getGameSessionId();
    if (gameSessionId != null) {
      GameSession targetSession = activeSessions.get(gameSessionId);
      if (targetSession != null) {
        targetSession.processMessage(senderSession.getPlayerId(), message);
      } else {
        // This handles cases where a client thinks it's in a session that no longer exists.
        System.err.println(
            "Error: Client "
                + senderSession.getPlayerId()
                + " msg for unknown session "
                + gameSessionId);
        gameServer.queueWrite(
            senderSession.getChannel(), new TextMessage("Error: Your game session ended."));
        senderSession.setGameSessionId(null);
        handleNewClientConnection(senderSession);
      }
    } else {
      handleLobbyOrSetupMessage(senderSession, message);
    }
  }

  // A synchronized router for all messages from clients who are not yet in an active game.
  private synchronized void handleLobbyOrSetupMessage(ClientSession senderSession, Object message) {
    // --- DEBUG ---
    System.out.println("[DEBUG] SessionManager: Received message of type " + message.getClass().getSimpleName());

    if (message instanceof RegisterCommand cmd) {
      // --- DEBUG ---
      System.out.println("[DEBUG] SessionManager: Identified RegisterCommand. Calling handleRegister...");
      handleRegister(senderSession, cmd.getUsername(), cmd.getPassword());
      return;
    }


    if (message instanceof LoginCommand cmd) {
      handleLogin(senderSession, cmd.getUsername(), cmd.getPassword());
      return;
    }

    // --- Lobby and Game Setup Commands (require login) ---
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
      // It now sends a clear error message back to the client.
      System.err.println(
          "SessionManager: Received unhandled message type '"
              + message.getClass().getSimpleName()
              + "' from player "
              + senderSession.getPlayerId()
              + " in lobby.");
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage("Error: Invalid command for your current state."));
    }
  }

  /**
   * Aggregates case information from all connected clients into a single, unique list of available
   * cases on the server.
   */
  private synchronized void registerClientCases(ClientCaseListDTO caseListDTO) {
    if (caseListDTO == null || caseListDTO.getCases() == null) return;

    boolean changed = false;
    for (CaseInfoDTO clientCase : caseListDTO.getCases()) {
      if (clientCase.getTitle() != null && !clientCase.getTitle().isBlank()) {
        // Using a lower-case key allows for case-insensitive matching of case titles.
        String key = clientCase.getTitle().toLowerCase();
        if (!uniqueAvailableCases.containsKey(key)) {
          uniqueAvailableCases.put(key, clientCase);
          changed = true;
        }
      }
    }

    if (changed) {
      System.out.println(
          "SessionManager: Unique case registry updated. Total unique cases: "
              + uniqueAvailableCases.size());
    }
  }

  private void sendAvailableCases(ClientSession client) {
    List<CaseInfoDTO> sortedCases =
        uniqueAvailableCases.values().stream()
            .sorted(Comparator.comparing(CaseInfoDTO::getTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());

    System.out.println(
        "SessionManager: Sending available cases list ("
            + sortedCases.size()
            + " cases) to "
            + client.getPlayerId());
    gameServer.queueWrite(client.getChannel(), new AvailableCasesDTO(sortedCases));
  }

  // Add these two new private methods anywhere inside the GameSessionManager class.

  /** Handles a registration request from a client. */
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

    // --- DEBUG ---
    System.out.println("[DEBUG] handleRegister: Calling userDAO.addUser for user '" + username + "'...");

    Optional<User> newUser = userDAO.addUser(username, password);

    // --- DEBUG ---
    System.out.println("[DEBUG] handleRegister: userDAO.addUser has returned.");

    if (newUser.isPresent()) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage(
              "Registration successful. You can now log in with 'login "
                  + username
                  + " [password]'"));
      System.out.println("New user registered: " + username);
    } else {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage("Error: Username '" + username + "' is already taken."));
    }
  }

  /** Handles a login request from a client. */
  private void handleLogin(ClientSession senderSession, String username, String password) {
    if (senderSession.getAuthenticatedUser() != null) {
      gameServer.queueWrite(
          senderSession.getChannel(),
          new TextMessage(
              "Error: You are already logged in as "
                  + senderSession.getAuthenticatedUser().getUsername()));
      return;
    }

    // Use the DAO to get the stored hash for the user.
    Optional<String> storedHashOpt = userDAO.getPasswordHashForUser(username);

    if (storedHashOpt.isPresent()) {
      // If the user exists, verify the provided password against the stored hash.
      boolean passwordMatches = PasswordHasher.verify(password, storedHashOpt.get());

      if (passwordMatches) {
        // On successful login, find the full User object and attach it to the ClientSession.
        userDAO
            .findUserByUsername(username)
            .ifPresent(
                user -> {
                  senderSession.setAuthenticatedUser(user);
                  gameServer.queueWrite(
                      senderSession.getChannel(),
                      new TextMessage("Login successful. Welcome, " + user.getUsername() + "!"));
                  System.out.println(
                      "Player "
                          + senderSession.getPlayerId()
                          + " logged in as user "
                          + user.getUsername());
                });
      } else {
        gameServer.queueWrite(
            senderSession.getChannel(), new TextMessage("Error: Invalid username or password."));
      }
    } else {
      gameServer.queueWrite(
          senderSession.getChannel(), new TextMessage("Error: Invalid username or password."));
    }
  }

  /**
   * The core logic for handling a host request. It validates the request, creates a new
   * GameSession, and sets up the lobby.
   */
  private synchronized void handleHostRequest(
      ClientSession hostClient, String caseTitle, boolean isPublic) {
    // --- SECURITY CHECK ---
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

    System.out.println(
        "SessionManager: Player "
            + hostClient.getPlayerId()
            + " created session "
            + newSessionId
            + " for case '"
            + caseTitle
            + "'.");
  }

  // Generates a unique 4-digit code for private games.
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
                session ->
                    new PublicGameInfoDTO(
                        session.getHostPlayerId(),
                        session.getSelectedCaseInfo().getTitle(),
                        session.getSessionId()))
            .sorted(
                Comparator.comparing(
                    PublicGameInfoDTO::getCaseTitle, String.CASE_INSENSITIVE_ORDER))
            .collect(Collectors.toList());
    gameServer.queueWrite(client.getChannel(), new PublicGamesListDTO(publicGames));
  }

  private synchronized void handleJoinPublicGame(
      ClientSession joiningClient, String targetSessionId) {
    // Security Check: Ensure the user is logged in before allowing them to join a game.
    if (joiningClient.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You must be logged in to join a game."));
      return;
    }

    // Validation: Ensure the player is not already in another game session.
    if (joiningClient.getGameSessionId() != null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You are already in a game or lobby."));
      return;
    }

    GameSession targetSession = pendingPublicGames.get(targetSessionId);

    // Check if the target session exists and is not full.
    if (targetSession != null
        && targetSession.getPlayerCount() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
      // Since the game is now full, remove it from the list of pending public games.
      pendingPublicGames.remove(targetSessionId);

      // Update the client's state to associate them with this session.
      joiningClient.setGameSessionId(targetSessionId);
      targetSession.addPlayer(joiningClient);

      // Notify the joining player that they have successfully joined.
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(
              true,
              "Joined public game '" + targetSession.getSelectedCaseInfo().getTitle() + "'!",
              targetSessionId));

      // Notify the existing players (the host) that a new player has joined.
      targetSession.broadcastToSession(
          new LobbyUpdateDTO(
              joiningClient.getPlayerId() + " joined! Starting game...",
              targetSession.getPlayerIds()),
          joiningClient.getPlayerId());

      // Kick off the case loading process now that the lobby is full.
      targetSession.initializeSession();

      // The client is no longer in the general lobby.
      lobby.remove(joiningClient);
    } else {
      // The session was not found or was already full.
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "Game lobby not found or full.", null));
    }
  }

  private synchronized void handleJoinPrivateGame(ClientSession joiningClient, String privateCode) {
    // Security Check: Ensure the user is logged in before allowing them to join a game.
    if (joiningClient.getAuthenticatedUser() == null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new TextMessage("Error: You must be logged in to join a game."));
      return;
    }

    // Validation: Ensure the player is not already in another game session.
    if (joiningClient.getGameSessionId() != null) {
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "You are already in a game or lobby.", null));
      return;
    }

    String targetSessionId = privateGameCodes.get(privateCode);

    if (targetSessionId != null) {
      // For private games, the session is in the main `activeSessions` map from the moment it's
      // created.
      GameSession targetSession = activeSessions.get(targetSessionId);

      // Check if the target session exists and is not full.
      if (targetSession != null
          && targetSession.getPlayerCount() < NetworkConstants.MAX_PLAYERS_PER_GAME) {
        // The private code is single-use; remove it once a player successfully joins.
        privateGameCodes.remove(privateCode);

        // Update the client's state to associate them with this session.
        joiningClient.setGameSessionId(targetSessionId);
        targetSession.addPlayer(joiningClient);

        // Notify the joining player that they have successfully joined.
        gameServer.queueWrite(
            joiningClient.getChannel(),
            new JoinGameResponseDTO(
                true,
                "Joined private game '" + targetSession.getSelectedCaseInfo().getTitle() + "'!",
                targetSessionId));

        // Notify the existing players (the host) that a new player has joined.
        targetSession.broadcastToSession(
            new LobbyUpdateDTO(
                joiningClient.getPlayerId() + " joined! Starting game...",
                targetSession.getPlayerIds()),
            joiningClient.getPlayerId());

        // Kick off the case loading process now that the lobby is full.
        targetSession.initializeSession();

        // The client is no longer in the general lobby.
        lobby.remove(joiningClient);
      } else {
        // This can happen if the host disconnected or if another player joined first.
        gameServer.queueWrite(
            joiningClient.getChannel(),
            new JoinGameResponseDTO(false, "Private game full or code expired.", null));
      }
    } else {
      // The provided code does not match any active private games.
      gameServer.queueWrite(
          joiningClient.getChannel(),
          new JoinGameResponseDTO(false, "Invalid private game code.", null));
    }
  }

  // A synchronized method to handle all logic related to a client disconnecting.
  public synchronized void handleClientDisconnect(ClientSession disconnectedClient) {
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
      // Logic is delegated to the session itself, which knows if the host left.
      boolean sessionShouldBeTerminated =
          session.handlePlayerDisconnect(disconnectedClient.getPlayerId());
      if (sessionShouldBeTerminated) {
        removeSession(gameSessionId);
      }
    }
  }

  /** Completely removes a game session from all tracking lists, ensuring a clean state. */
  public synchronized void removeSession(String sessionId) {
    if (sessionId == null) return;
    GameSession removedSession = activeSessions.remove(sessionId);
    if (removedSession != null) {
      System.out.println("SessionManager: Removing ended session " + sessionId);
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

  // A helper method to find a client's session object using their player ID.
  public ClientSession getClientSessionByPlayerId(String playerId) {
    for (ClientSession session : gameServer.getClients().values()) {
      if (session.getPlayerId().equals(playerId)) {
        return session;
      }
    }
    return null;
  }
}
