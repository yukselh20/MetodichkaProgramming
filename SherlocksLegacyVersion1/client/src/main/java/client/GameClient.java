package client;

import common.Commands.*;
import common.dto.*;
import java.io.Serializable;
import java.util.List;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This is the central coordinator for the client-side application. It orchestrates
// the network manager, input handler, state manager, and UI. I use it to route user
// input and server responses to the correct handlers based on the client's current state.
public class GameClient {
  private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
  private final ClientStateManager stateManager;
  private final ClientUserInterface ui;
  private final ClientNetworkManager networkManager;
  private ClientInputHandler inputHandler;
  private final Object consoleLock = new Object();
  private Thread networkThread;
  private Thread inputThread;

  public GameClient(String host, int port) {
    this.stateManager = new ClientStateManager();
    this.ui = new ClientUserInterface(stateManager, consoleLock);
    // this passes callbacks (method references) to the network manager, allowing it to
    // communicate events back to this coordinator without a direct dependency.
    this.networkManager =
        new ClientNetworkManager(
            host, port, stateManager, this::processServerResponse, this::handleDisconnect);
  }

  public void start(Scanner scanner) {
    this.inputHandler = new ClientInputHandler(scanner, this, stateManager);

    networkThread = new Thread(networkManager, "ClientNetworkThread");
    inputThread = new Thread(this.inputHandler, "ClientInputThread");

    // threads are set as daemons so they don't prevent the application
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
      shutdown();
    }
  }

  // This is the primary hub for processing all user input. It uses the
  // client's current state to determine how to interpret the command.
  public void processUserInput(String input) {
    synchronized (consoleLock) {
      if (!stateManager.isRunning() || input == null) return;

      logger.debug("Processing user input '{}' in state {}", input, stateManager.getCurrentState());

      if (input.equalsIgnoreCase("quit")) {
        handleQuitTypedCommand();
        return;
      }
      if (input.toLowerCase().startsWith("/chat ")) {
        handleChatCommand(input);
        ui.printPrompt();
        return;
      }

      ClientState state = stateManager.getCurrentState();
      switch (state) {
          // States that accept standard in-game commands.
        case IN_GAME:
        case READY_TO_START_GAME_HOST:
        case GUEST_READY_TO_START:
          Serializable command = createInGameCommand(input);
          if (command != null) {
            networkManager.sendMessage(command);
          } else {
            ui.printPrompt();
          }
          break;

          // State for Final Exam answer submission.
        case AWAITING_EXAM_QUESTION_HOST:
          handleExamAnswerSubmissionInput(input);
          break;

          // States for Menu Navigation.
        case INITIAL_MENU:
          handleInitialMenuInput(input);
          break;
        case AUTH_MENU:
          handleAuthMenuInput(input);
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
        case BROWSING_PUBLIC_GAMES:
          handleBrowsePublicInput(input);
          break;
        case ENTERING_PRIVATE_CODE:
          handlePrivateCodeInput(input);
          break;
        case ADDING_CASE:
          handleAddCasePathInput(input);
          break;

          // States for typed-in credentials.
        case AWAITING_LOGIN_INPUT:
          if (input.equalsIgnoreCase("back")) {
            changeState(ClientState.AUTH_MENU);
            break;
          }
          String[] loginParts = input.trim().split("\\s+", 2);
          if (loginParts.length == 2) {
            networkManager.sendMessage(new LoginCommand(loginParts[0], loginParts[1]));
            ui.displayMessage("Sending login request...");
          } else {
            ui.displayMessage("Invalid format. Usage: <username> <password>");
            ui.printPrompt();
          }
          break;
        case AWAITING_REGISTER_INPUT:
          if (input.equalsIgnoreCase("back")) {
            changeState(ClientState.AUTH_MENU);
            break;
          }
          String[] registerParts = input.trim().split("\\s+", 2);
          if (registerParts.length == 2) {
            networkManager.sendMessage(new RegisterCommand(registerParts[0], registerParts[1]));
            ui.displayMessage("Sending registration request...");
          } else {
            ui.displayMessage("Invalid format. Usage: <username> <password>");
            ui.printPrompt();
          }
          break;

          // All other states are "waiting" states.
        default:
          ui.displayMessage("Waiting... ('/chat' or 'quit' available)");
          ui.printPrompt();
          break;
      }
    }
  }

  // This is the callback method for processing all objects from the server.
  // It determines the object's type and assigns the appropriate handler.
  private void processServerResponse(Object response) {
    synchronized (consoleLock) {
      System.out.println();
      logger.debug("Processing server response of type: {}", response.getClass().getSimpleName());

      ClientState stateBeforeProcessing = stateManager.getCurrentState();

      if (response instanceof TextMessage) {
        String content = ((TextMessage) response).getContent();
        ui.displayMessage("[Server] " + content);

        if (content.startsWith("Login successful")) {
          logger.info("Login successful. Declaring local cases to server...");
          List<CaseInfoDTO> localCases = ClientCaseLoader.loadLocalCaseInfo("cases");
          networkManager.sendMessage(new ClientCaseListDTO(localCases));
          changeState(ClientState.MULTIPLAYER_MENU);
          return;
        }

        ClientState currentState = stateManager.getCurrentState();

        if ((currentState == ClientState.AWAITING_LOGIN_INPUT
                || currentState == ClientState.AWAITING_REGISTER_INPUT)
            && content.startsWith("Error:")) {
          changeState(ClientState.AUTH_MENU);
          return;
        }

        if (content.startsWith("Registration successful")) {
          changeState(ClientState.AUTH_MENU);
          return;
        }

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
        } else if (contentLower.contains("the investigation begins!")) {
          changeState(ClientState.IN_GAME);
        }

      } else if (response instanceof ChatMessage) {
        ui.displayMessage(((ChatMessage) response).getFormattedMessage());
      } else if (response instanceof AvailableCasesDTO) {
        stateManager.setAvailableCasesCache(((AvailableCasesDTO) response).getUniqueCases());
        if (stateManager.getCurrentState() == ClientState.WAITING_FOR_SERVER_INFO) {
          changeState(ClientState.SELECTING_CASE);
        }
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
      // as the new state's menu handles its own prompt. I only re-prompt if
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

  private void initiateMultiplayerConnection() {
    ui.displayMessage("Attempting to connect to the server...");

    if (networkManager.connect()) {
      changeState(ClientState.AUTH_MENU);
    } else {
      changeState(ClientState.INITIAL_MENU);
    }
  }

  private void handleHostGameResponse(HostGameResponseDTO dto) {
    ui.displayMessage("[Server] " + dto.getMessage());
    if (dto.isSuccess()) {
      stateManager.setIsHost(true);
      stateManager.setMyPlayerIdCache(dto.getMessage().split(" ")[0]);
      stateManager.setGameSessionIdCache(dto.getGameSessionId());
      changeState(ClientState.HOST_LOBBY_WAITING);
      if (dto.getPrivateCode() != null) {
        ui.displayMessage("Your private game code is: " + dto.getPrivateCode());
      }
    } else {
      stateManager.setIsHost(false);
      changeState(ClientState.SELECTING_CASE);
    }
  }

  private void handleJoinGameResponse(JoinGameResponseDTO dto) {
    ui.displayMessage("[Server] " + dto.getMessage());
    if (dto.isSuccess()) {
      stateManager.setIsHost(false);
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
        stateManager.clearSessionData();
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
      case "3":
        changeState(ClientState.SELECTING_CASE);
        break;
      default:
        ui.displayMessage("Invalid choice.");
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
    if (stateManager.isHost()) {
      changeState(ClientState.AWAITING_EXAM_QUESTION_HOST);
    } else {
      changeState(ClientState.VIEWING_EXAM_GUEST);
    }
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
      changeState(ClientState.JOIN_GAME_MENU);
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
      changeState(ClientState.JOIN_GAME_MENU);
      return;
    }
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
      if (state == ClientState.HOST_LOBBY_WAITING
          || state == ClientState.GUEST_LOBBY_AWAITING_START
          || state == ClientState.READY_TO_START_GAME_HOST
          || state == ClientState.GUEST_READY_TO_START
          || state == ClientState.IN_GAME
          || state == ClientState.FINAL_EXAM_ACTIVE
          || state == ClientState.AWAITING_EXAM_QUESTION_HOST
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
      ui.displayMessage("Unknown Command type help to see available commands");
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
        // The "journal" command has sub-commands, so I added another layer of parsing.
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
    if (inputThread != null) {
      logger.info("Interrupting input thread...");
      inputThread.interrupt();
    }
    if (networkThread != null) {
      logger.info("Interrupting network thread...");
      networkThread.interrupt();
    }
  }

  // centralized method for changing the client's state.
  private void changeState(ClientState newState) {
    ClientState oldState = stateManager.getAndSetState(newState);
    boolean forceReprint =
        (newState == ClientState.VIEWING_EXAM_GUEST
            || newState == ClientState.AWAITING_EXAM_QUESTION_HOST);

    if ((oldState != newState || newState == ClientState.INITIAL_MENU) || forceReprint) {
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
        ui.displayMessage("Requesting list of available cases to host...");
        networkManager.sendMessage(new RequestCaseListCommand());
        changeState(ClientState.WAITING_FOR_SERVER_INFO);
        break;
      case "2":
        changeState(ClientState.JOIN_GAME_MENU);
        break;
      case "3":
        ui.displayMessage("Returning to main menu...");
        networkManager.close();
        stateManager.clearSessionData();
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
}
