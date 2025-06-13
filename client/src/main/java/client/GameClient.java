package client;

import common.Commands.*;
import common.dto.*;
import java.io.Serializable;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The central coordinator (Mediator pattern) for the client-side application. It orchestrates all
 * major components: the network manager, input handler, state manager, and user interface. It is
 * responsible for routing user input and server responses to the correct handlers based on the
 * client's current state.
 */
public class GameClient {
  private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
  private final ClientStateManager stateManager;
  private final ClientUserInterface ui;
  private final ClientNetworkManager networkManager;
  private ClientInputHandler inputHandler;

  // A shared lock to synchronize all console I/O, preventing display
  // corruption from concurrent threads writing to the console.
  private final Object consoleLock = new Object();
  private Thread networkThread;
  private Thread inputThread;

  public GameClient(String host, int port) {
    this.stateManager = new ClientStateManager();
    this.ui = new ClientUserInterface(stateManager, consoleLock);
    // Callbacks (this::processServerResponse, this::handleDisconnect) are passed
    // to the network manager, allowing it to communicate events back to this
    // coordinator without a direct dependency.
    this.networkManager =
        new ClientNetworkManager(
            host, port, stateManager, this::processServerResponse, this::handleDisconnect);
  }

  public void start(Scanner scanner) {
    this.inputHandler = new ClientInputHandler(scanner, this, stateManager);

    networkThread = new Thread(networkManager, "ClientNetworkThread");
    inputThread = new Thread(this.inputHandler, "ClientInputThread");

    // Threads are set as daemons so they don't prevent the application
    // from exiting when the main thread finishes.
    networkThread.setDaemon(true);
    inputThread.setDaemon(true);

    logger.info("Starting network and input threads...");
    networkThread.start();
    inputThread.start();

    changeState(ClientState.INITIAL_MENU);

    try {
      // The main thread waits for the input thread to terminate. This happens
      // when the user quits or the input stream closes.
      inputThread.join();
    } catch (InterruptedException e) {
      logger.warn("Main thread interrupted while waiting for input thread to finish.");
      Thread.currentThread().interrupt();
    } finally {
      // The shutdown sequence is crucial for cleaning up resources,
      // regardless of how the application exits.
      shutdown();
    }
  }

  /**
   * The primary hub for processing all user input, routed from the Input Handler. It uses the
   * client's current state to determine how to interpret the command.
   */
  public void processUserInput(String input) {
    synchronized (consoleLock) {
      if (!stateManager.isRunning() || input == null) return;

      logger.debug("Processing user input '{}' in state {}", input, stateManager.getCurrentState());

      // Global commands like 'quit' and '/chat' are checked first because they
      // can be used in almost any state.
      if (input.equalsIgnoreCase("quit")) {
        handleQuitTypedCommand();
        return;
      }
      if (input.toLowerCase().startsWith("/chat ")) {
        handleChatCommand(input);
        ui.printPrompt();
        return;
      }

      // After handling global commands, the input is processed based on the
      // specific state of the client's state machine.
      ClientState state = stateManager.getCurrentState();
      switch (state) {
        case INITIAL_MENU:
          handleInitialMenuInput(input);
          break;
        case AUTH_MENU:
          handleAuthMenuInput(input);
          break;
        case AWAITING_LOGIN_INPUT:
        case AWAITING_REGISTER_INPUT:
          if (input.equalsIgnoreCase("back") || input.equalsIgnoreCase("cancel")) {
            changeState(ClientState.AUTH_MENU);
            break;
          }
          Serializable lobbyCommand = parseLobbyCommand(input);
          if (lobbyCommand != null) {
            networkManager.sendMessage(lobbyCommand);
            // Give user feedback that the command was sent
            if (lobbyCommand instanceof LoginCommand) {
              ui.displayMessage("Sending login request...");
            } else if (lobbyCommand instanceof RegisterCommand) {
              ui.displayMessage("Sending registration request...");
            }
          }
          break;
        case ADDING_CASE:
          handleAddCasePathInput(input);
          break;
        case MULTIPLAYER_MENU:
          handleMultiplayerMenuInput(input);
          break;
        case JOIN_GAME_MENU:
          handleJoinGameMenuInput(input);
          break;
        case SELECTING_CASE:
          handleCaseSelectionInput(input);
          break;
        case CHOOSING_HOST_MODE:
          handleHostModeChoiceInput(input);
          break;
        case CHOOSING_JOIN_MODE:
          handleJoinModeChoiceInput(input);
          break;
        case BROWSING_PUBLIC_GAMES:
          handleBrowsePublicInput(input);
          break;
        case ENTERING_PRIVATE_CODE:
          handlePrivateCodeInput(input);
          break;
          // Several in-game states share the same command parsing logic.
        case GUEST_READY_TO_START:
        case READY_TO_START_GAME_HOST:
        case IN_GAME:
          Serializable command = createInGameCommand(input);
          if (command != null) {
            networkManager.sendMessage(command);
          } else {
            ui.printPrompt();
          }
          break;
        case AWAITING_EXAM_QUESTION_HOST:
          handleExamAnswerSubmissionInput(input);
          break;
        case DISCONNECTED:
          if (input.equalsIgnoreCase("menu")) {
            changeState(ClientState.INITIAL_MENU);
          } else {
            ui.printCurrentStateInfo();
          }
          break;
        default:
          // This default case handles all "waiting" states where the user
          // can't do much besides quit or chat.
          ui.displayMessage("Waiting... ('/chat' or 'quit' available)");
          ui.printPrompt();
          break;
      }
    }
  }

  /**
   * The callback method for processing all objects received from the server. It determines the
   * object's type and delegates to the appropriate handler.
   */
  private void processServerResponse(Object response) {
    synchronized (consoleLock) {
      // A newline is printed to cleanly separate server output from user input.
      System.out.println();
      logger.debug("Processing server response of type: {}", response.getClass().getSimpleName());

      ClientState stateBeforeProcessing = stateManager.getCurrentState();

      // The long if-else-if chain acts as a router, directing each DTO type
      // to a specialized handler method.
      if (response instanceof TextMessage) {
        String content = ((TextMessage) response).getContent();
        ui.displayMessage("[Server] " + content);


        // On successful registration, we return the user to the auth menu to log in
        if (content.startsWith("Registration successful")) {
          changeState(ClientState.AUTH_MENU);
          return; // Exit early
        }

        // Certain text messages from the server are used as cues to change the
        // client's state, synchronizing the client and server state machines.
        String contentLower = content.toLowerCase();
        if (contentLower.contains("session has ended")
            || contentLower.contains("session aborted")) {
          ui.displayMessage("Returning to main menu...");
          networkManager.close();
          changeState(ClientState.INITIAL_MENU);
          return;
        } else if (contentLower.contains("please type 'start case' to begin")) {
          changeState(ClientState.READY_TO_START_GAME_HOST);
        } else if (contentLower.contains("waiting for the host to start the game")) {
          changeState(ClientState.GUEST_READY_TO_START);
        } else if (contentLower.startsWith("host, please submit your answer for q")) {
          changeState(ClientState.AWAITING_EXAM_QUESTION_HOST);
        } else if (contentLower.contains("is answering question")) {
          changeState(ClientState.VIEWING_EXAM_GUEST);
        } else if (contentLower.contains("the investigation begins!")) {
          changeState(ClientState.IN_GAME);
        }

      } else if (response instanceof ChatMessage) {
        ui.displayMessage(((ChatMessage) response).getFormattedMessage());
      } else if (response instanceof AvailableCasesDTO) {
        stateManager.setAvailableCasesCache(((AvailableCasesDTO) response).getUniqueCases());
        changeState(ClientState.MULTIPLAYER_MENU);
      } else if (response instanceof HostGameResponseDTO) {
        handleHostGameResponse((HostGameResponseDTO) response);
      } else if (response instanceof PublicGamesListDTO) {
        stateManager.setPublicGamesCache(((PublicGamesListDTO) response).getPublicGames());
        changeState(ClientState.BROWSING_PUBLIC_GAMES);
      } else if (response instanceof JoinGameResponseDTO) {
        handleJoinGameResponse((JoinGameResponseDTO) response);
      } else if (response instanceof LobbyUpdateDTO) {
        handleLobbyUpdateResponse((LobbyUpdateDTO) response);
      } else if (response instanceof RoomDescriptionDTO) {
        ui.displayRoomDescription((RoomDescriptionDTO) response);
        if (stateManager.getCurrentState() != ClientState.IN_GAME) {
          changeState(ClientState.IN_GAME);
        }
      } else if (response instanceof ExamQuestionInfoDTO) {
        handleExamQuestionInfo((ExamQuestionInfoDTO) response);
      } else if (response instanceof ExamResultDTO) {
        handleExamResult((ExamResultDTO) response);
      } else if (response instanceof JournalEntryDTO) {
        ui.displayMessage("[Journal Update] " + ((JournalEntryDTO) response).getFormattedEntry());
      } else {
        logger.warn(
            "Received unknown response type from server: {}", response.getClass().getSimpleName());
        ui.displayMessage(
            "[Unknown Response Type] " + response.getClass().getSimpleName() + ": " + response);
      }

      // This logic prevents re-printing the prompt if the state just changed,
      // as the new state's menu handles its own prompt. It only re-prompts if
      // the state was unchanged (e.g., after a chat message).
      if (stateManager.getCurrentState() == stateBeforeProcessing
          && stateManager.getCurrentState() != ClientState.INITIAL_MENU) {
        ui.printPrompt();
      }
    }
  }

  private void handleInitialMenuInput(String input) {
    switch (input.toLowerCase()) {
      case "1":
        changeState(ClientState.ADDING_CASE);
        break;
      case "2":
        initiateMultiplayerConnection();
        break;
      case "3":
        stateManager.setRunning(false);
        break;
      default:
        ui.displayMessage("Invalid choice. Please enter 1, 2, or 3.");
        ui.printPrompt();
        break;
    }
  }

  private void handleAuthMenuInput(String input) {
    switch (input.toLowerCase()) {
      case "1":
        changeState(ClientState.AWAITING_LOGIN_INPUT);
        break;
      case "2":
        changeState(ClientState.AWAITING_REGISTER_INPUT);
        break;
      case "3":
        changeState(ClientState.INITIAL_MENU);
        break;
      default:
        ui.displayMessage("Invalid choice. Please enter 1, 2, or 3.");
        ui.printPrompt();
        break;
    }
  }

  private void handleCredentialInput(String input, String commandType) {
    String[] parts = input.trim().split("\\s+", 3);
    String commandWord = parts.length > 0 ? parts[0].toLowerCase() : "";

    // Check if the user typed the correct command (login or register)
    if (commandWord.equals(commandType) && parts.length == 3) {
      String username = parts[1];
      String password = parts[2];

      if (commandType.equals("login")) {
        networkManager.sendMessage(new LoginCommand(username, password));
        ui.displayMessage("Sending login request...");
      } else { // register
        networkManager.sendMessage(new RegisterCommand(username, password));
        ui.displayMessage("Sending registration request...");
      }
    } else if (input.equalsIgnoreCase("back") || input.equalsIgnoreCase("cancel")) {
      // Allow user to go back to the auth menu
      changeState(ClientState.AUTH_MENU);
    } else {
      ui.displayMessage("Invalid format. Usage: " + commandType + " <username> <password>");
      ui.printPrompt();
    }
  }

  // This method kicks off the connection process without blocking the input thread.
  // The outcome of the connection attempt will be handled by a callback.
// In GameClient.java
  private void initiateMultiplayerConnection() {
    // Show the user we are attempting to connect.
    changeState(ClientState.CONNECTING);

    if (!networkManager.connect()) {
      // Connection failed immediately, inform the user and go back.
      ui.displayMessage("\nConnection to the server failed. Please try again later.");
      changeState(ClientState.INITIAL_MENU);
    } else {
      // Connection successful, now proceed to the authentication menu.
      changeState(ClientState.AUTH_MENU);
    }
  }

  private void handleHostGameResponse(HostGameResponseDTO dto) {
    ui.displayMessage("[Server] " + dto.getMessage());
    if (dto.isSuccess()) {
      // This is a temporary way to get the player ID. A more robust solution
      // would be a dedicated field in the DTO.
      stateManager.setMyPlayerIdCache(dto.getMessage().split(" ")[0]);
      stateManager.setGameSessionIdCache(dto.getGameSessionId());
      changeState(ClientState.HOST_LOBBY_WAITING);
      if (dto.getPrivateCode() != null) {
        ui.displayMessage("Your private game code is: " + dto.getPrivateCode());
      }
    } else {
      changeState(ClientState.SELECTING_CASE);
    }
  }

  private void handleJoinGameResponse(JoinGameResponseDTO dto) {
    ui.displayMessage("[Server] " + dto.getMessage());
    if (dto.isSuccess()) {
      stateManager.setGameSessionIdCache(dto.getGameSessionId());
      changeState(ClientState.GUEST_LOBBY_AWAITING_START);
    } else {
      changeState(ClientState.MULTIPLAYER_MENU);
    }
  }

  private void handleLobbyUpdateResponse(LobbyUpdateDTO dto) {
    ui.displayMessage("[Lobby] " + dto.getMessage());
    String msgLower = dto.getMessage().toLowerCase();
    if (stateManager.getCurrentState() == ClientState.HOST_LOBBY_WAITING
        && (msgLower.contains("joined! starting game")
            || msgLower.contains("ready. waiting for host"))) {
      changeState(ClientState.READY_TO_START_GAME_HOST);
    } else if (stateManager.getCurrentState() == ClientState.GUEST_LOBBY_AWAITING_START
        && msgLower.contains("investigation begins")) {
      changeState(ClientState.IN_GAME);
    }
  }

  // The behavior of the 'quit' command is context-sensitive. In a game, it
  // disconnects. In menus, it might just navigate back or exit the application.
  private void handleQuitTypedCommand() {
    ClientState state = stateManager.getCurrentState();
    logger.info("'Quit' command typed in state: {}", state);

    switch (state) {
      case INITIAL_MENU:
      case DISCONNECTED:
      case EXITING:
        stateManager.setRunning(false);
        break;

      case IN_GAME:
      case FINAL_EXAM_ACTIVE:
      case HOST_LOBBY_WAITING:
      case GUEST_LOBBY_AWAITING_START:
      case READY_TO_START_GAME_HOST:
        ui.displayMessage("Disconnecting from server and returning to main menu...");
        networkManager.close();
        changeState(ClientState.INITIAL_MENU);
        break;

      default:
        ui.displayMessage("Returning to main menu...");
        if (networkManager != null) {
          networkManager.close();
        }
        changeState(ClientState.INITIAL_MENU);
        break;
    }
  }

  private void handleExamAnswerSubmissionInput(String input) {
    String answerText = input;
    int questionNumber = stateManager.getCurrentExamQuestionNumberBeingAnswered();

    if (answerText.isEmpty()) {
      ui.displayMessage(
          "Answer cannot be empty. Please type your answer for Q" + questionNumber + ".");
      ui.printPrompt();
    } else {
      ui.displayMessage("Submitting answer for Q" + questionNumber + ": " + answerText);
      networkManager.sendMessage(new SubmitExamAnswerCommand(questionNumber, answerText));
      changeState(ClientState.SUBMITTING_EXAM_ANSWER_HOST);
    }
  }

  private void handleAddCasePathInput(String filePath) {
    if (filePath.equalsIgnoreCase("back") || filePath.equalsIgnoreCase("cancel")) {
      ui.displayMessage("Add case cancelled.");
    } else {
      boolean added = ClientAddCaseUtil.addCaseFromFile(filePath);
      if (added) {
        ui.displayMessage("Case added successfully to your local 'cases' folder.");
      } else {
        ui.displayMessage("Failed to add case. Please check the file path and JSON format.");
      }
    }
    changeState(ClientState.INITIAL_MENU);
  }

  private void handleCaseSelectionInput(String input) {
    if (input.equalsIgnoreCase("refresh")) {
      networkManager.sendMessage(new RequestCaseListCommand());
      changeState(ClientState.WAITING_FOR_SERVER_INFO);
      return;
    }
    if (input.equalsIgnoreCase("back")) {
      changeState(ClientState.MULTIPLAYER_MENU);
      return;
    }
    try {
      int choice = Integer.parseInt(input) - 1;
      List<CaseInfoDTO> cases = stateManager.getAvailableCasesCache();
      if (choice >= 0 && choice < cases.size()) {
        CaseInfoDTO selected = cases.get(choice);
        stateManager.setSelectedCaseTitleCache(selected.getTitle());
        stateManager.setSelectedCaseDescriptionCache(selected.getDescription());
        changeState(ClientState.CHOOSING_HOST_MODE);
      } else {
        ui.displayMessage("Invalid case number.");
        ui.printPrompt();
      }
    } catch (NumberFormatException e) {
      ui.displayMessage("Invalid input. Enter a number, 'refresh', or 'back'.");
      ui.printPrompt();
    }
  }

  private void handleHostModeChoiceInput(String input) {
    String caseTitle = stateManager.getSelectedCaseTitleCache();
    if (caseTitle == null) {
      ui.displayMessage("Error: No case selected. Returning to menu.");
      changeState(ClientState.SELECTING_CASE);
      return;
    }
    switch (input.toLowerCase()) {
      case "1":
        networkManager.sendMessage(new HostGameCommand(caseTitle, true));
        changeState(ClientState.SUBMITTING_HOST_REQUEST);
        break;
      case "2":
        networkManager.sendMessage(new HostGameCommand(caseTitle, false));
        changeState(ClientState.SUBMITTING_HOST_REQUEST);
        break;
      case "back":
        changeState(ClientState.SELECTING_CASE);
        break;
      default:
        ui.displayMessage("Invalid choice.");
        ui.printPrompt();
        break;
    }
  }

  private void handleJoinModeChoiceInput(String input) {
    switch (input.toLowerCase()) {
      case "1":
        ui.displayMessage("Requesting list of public games...");
        networkManager.sendMessage(new ListPublicGamesCommand());
        changeState(ClientState.WAITING_FOR_SERVER_INFO);
        break;
      case "2":
        changeState(ClientState.ENTERING_PRIVATE_CODE);
        break;
      case "back":
        changeState(ClientState.CHOOSING_HOST_MODE);
        break;
      default:
        ui.displayMessage("Invalid choice. Please enter 1, 2, or back.");
        ui.printPrompt();
        break;
    }
  }

  private void handleExamQuestionInfo(ExamQuestionInfoDTO qInfo) {
    ui.displayMessage("--- Final Exam Question ---");
    ui.displayMessage(
        String.format("Question %d of %d:", qInfo.getQuestionNumber(), qInfo.getTotalQuestions()));
    ui.displayMessage(qInfo.getQuestionText());
    stateManager.setCurrentExamQuestionNumberBeingAnswered(qInfo.getQuestionNumber());
  }

  private void handleExamResult(ExamResultDTO result) {
    ui.displayMessage(result.toString());
    boolean isVictory =
        result.getScore() == result.getTotalQuestions() && result.getTotalQuestions() > 0;
    if (isVictory) {
      ui.displayMessage("\nGame Over. Returning to the main menu...");
      networkManager.close();
      changeState(ClientState.INITIAL_MENU);
    } else {
      ui.displayMessage("\nYou can now continue investigating or type 'help' for commands.");
      changeState(ClientState.IN_GAME);
    }
  }

  private void handleBrowsePublicInput(String input) {
    if (input.equalsIgnoreCase("back")) {
      changeState(ClientState.CHOOSING_JOIN_MODE);
      return;
    }
    if (input.equalsIgnoreCase("refresh")) {
      networkManager.sendMessage(new ListPublicGamesCommand());
      changeState(ClientState.WAITING_FOR_SERVER_INFO);
      return;
    }
    try {
      int choice = Integer.parseInt(input) - 1;
      List<PublicGameInfoDTO> games = stateManager.getPublicGamesCache();
      if (choice >= 0 && choice < games.size()) {
        networkManager.sendMessage(new JoinPublicGameCommand(games.get(choice).getGameSessionId()));
        changeState(ClientState.SUBMITTING_JOIN_REQUEST);
      } else {
        ui.displayMessage("Invalid game number.");
        ui.printPrompt();
      }
    } catch (NumberFormatException e) {
      ui.displayMessage("Invalid input.");
      ui.printPrompt();
    }
  }

  private void handlePrivateCodeInput(String input) {
    if (input.equalsIgnoreCase("back")) {
      changeState(ClientState.CHOOSING_JOIN_MODE);
      return;
    }
    // A simple regex check ensures the private code format is correct before sending.
    if (input.matches("\\d{4}")) {
      networkManager.sendMessage(new JoinPrivateGameCommand(input));
      changeState(ClientState.SUBMITTING_JOIN_REQUEST);
    } else {
      ui.displayMessage("Invalid code format (must be 4 digits) or type 'back'.");
      ui.printPrompt();
    }
  }

  private void handleChatCommand(String input) {
    if (input.length() > "/chat ".length()) {
      ClientState state = stateManager.getCurrentState();
      // Chat is only permitted in specific, interactive states.
      if (state == ClientState.HOST_LOBBY_WAITING
          || state == ClientState.GUEST_LOBBY_AWAITING_START
          || state == ClientState.READY_TO_START_GAME_HOST
          || state == ClientState.IN_GAME
          || state == ClientState.FINAL_EXAM_ACTIVE
          || state == ClientState.VIEWING_EXAM_GUEST) {
        String message = input.substring("/chat ".length());
        String playerId = stateManager.getMyPlayerIdCache();
        networkManager.sendMessage(new ChatMessage(playerId != null ? playerId : "Me", message));
      } else {
        ui.displayMessage("Chat is not available in current state: " + state);
      }
    } else {
      ui.displayMessage("Usage: /chat <message>");
    }
  }

  // This method acts as a factory and parser for creating Command objects
  // from raw user input during the main game loop.
  private Serializable createInGameCommand(String rawInput) {
    if (rawInput == null || rawInput.isBlank()) return null;

    String normalizedInput = rawInput.trim().toLowerCase();

    // First, check for simple commands that have no arguments.
    if (normalizedInput.equals("start case")) return new StartCaseCommand();
    if (normalizedInput.equals("ask watson")) return new AskWatsonCommand();
    if (normalizedInput.equals("final exam")) return new FinalExamCommand();
    if (normalizedInput.equals("look")) return new LookCommand();
    if (normalizedInput.equals("tasks")) return new TaskCommand();
    if (normalizedInput.equals("help")) return new HelpCommand();
    if (normalizedInput.equals("journal")) return new JournalCommand(null);

    // Then, handle commands that are expected to have arguments.
    String[] parts = rawInput.trim().split("\\s+", 2);
    String commandWord = parts[0].toLowerCase();
    String args = (parts.length > 1) ? parts[1] : "";

    if (args.isEmpty()) {
      ui.displayMessage("Command '" + commandWord + "' requires an argument.");
      return null;
    }

    switch (commandWord) {
      case "move":
        return new MoveCommand(args.split("\\s+")[0]);
      case "examine":
        return new ExamineCommand(args);
      case "question":
        return new QuestionCommand(args);
      case "deduce":
        return new DeduceCommand(args);
        // The "journal" command has sub-commands, adding another layer of parsing.
      case "journal":
        if (args.toLowerCase().startsWith("add ")) {
          String note = args.substring(4).trim();
          if (note.isEmpty()) {
            ui.displayMessage("Usage: journal add [note text]");
            return null;
          }
          return new JournalAddCommand(note);
        } else {
          return new JournalCommand(args);
        }
      default:
        ui.displayMessage(
            "Unknown command: '" + rawInput.trim() + "'. Type 'help' for a list of commands.");
        return null;
    }
  }

  public void shutdown() {
    if (!stateManager.isRunning()) {
      return;
    }
    ui.displayMessage("\nShutting down client...");
    stateManager.setRunning(false);

    if (networkManager != null) {
      networkManager.close();
    }

    // Interrupting the threads ensures they break out of any blocking
    // operations (like reading input or network select) and terminate cleanly.
    if (inputThread != null) {
      logger.info("Interrupting input thread...");
      inputThread.interrupt();
    }
    if (networkThread != null) {
      logger.info("Interrupting network thread...");
      networkThread.interrupt();
    }
  }

  // A centralized method for changing the client's state. It also triggers
  // UI updates and handles state-based cache clearing.
  private void changeState(ClientState newState) {
    ClientState oldState = stateManager.getAndSetState(newState);
    if (oldState != newState || newState == ClientState.INITIAL_MENU) {
      logger.debug("Client state changed from {} to {}", oldState, newState);
      // This logic cleans up cached data when navigating away from certain menus
      // to prevent stale information from being used later.
      if (oldState == ClientState.SELECTING_CASE && newState != ClientState.CHOOSING_HOST_MODE) {
        stateManager.clearCaseSelectionCache();
      }
      if (oldState == ClientState.BROWSING_PUBLIC_GAMES
          && newState != ClientState.SUBMITTING_JOIN_REQUEST) {
        stateManager.clearPublicGamesCache();
      }
      ui.printCurrentStateInfo();
    }
  }

  // The callback method for handling disconnection events from the network manager.
  private void handleDisconnect(String reason) {
    synchronized (consoleLock) {
      if (stateManager.getCurrentState() != ClientState.DISCONNECTED
          && stateManager.getCurrentState() != ClientState.EXITING) {
        logger.warn("Handling disconnect event. Reason: {}", reason);
        ui.displayMessage("\n" + reason);
        changeState(ClientState.DISCONNECTED);
      }
    }
  }

  private void handleMultiplayerMenuInput(String input) {
    switch (input.toLowerCase()) {
      case "1":
        changeState(ClientState.SELECTING_CASE);
        break;
      case "2":
        changeState(ClientState.JOIN_GAME_MENU);
        break;
      case "3":
        ui.displayMessage("Returning to main menu...");
        networkManager.close();
        changeState(ClientState.INITIAL_MENU);
        break;
      default:
        ui.displayMessage("Invalid choice. Please enter 1, 2, or 3.");
        ui.printPrompt();
        break;
    }
  }

  private void handleJoinGameMenuInput(String input) {
    switch (input.toLowerCase()) {
      case "1":
        ui.displayMessage("Requesting list of public games...");
        networkManager.sendMessage(new ListPublicGamesCommand());
        changeState(ClientState.WAITING_FOR_SERVER_INFO);
        break;
      case "2":
        changeState(ClientState.ENTERING_PRIVATE_CODE);
        break;
      case "3":
        changeState(ClientState.MULTIPLAYER_MENU);
        break;
      default:
        ui.displayMessage("Invalid choice. Please enter 1, 2, or 3.");
        ui.printPrompt();
        break;
    }
  }

  // Add this new method to GameClient.java
  private Serializable parseLobbyCommand(String rawInput) {
    if (rawInput == null || rawInput.isBlank()) {
      return null;
    }

    String[] parts = rawInput.trim().split("\\s+", 3);
    String commandWord = parts[0].toLowerCase();

    // This handles commands that require two arguments (e.g., login <user> <pass>)
    if (parts.length == 3) {
      String arg1 = parts[1];
      String arg2 = parts[2];
      switch (commandWord) {
        case "register":
          return new RegisterCommand(arg1, arg2);
        case "login":
          return new LoginCommand(arg1, arg2);
        default:
          // It's not a known 2-argument command
          break;
      }
    }

    // Fallback for any other command or invalid format
    ui.displayMessage("Unknown command or invalid format. Type 'help' for a list of commands.");
    ui.printPrompt();
    return null;
  }
}
