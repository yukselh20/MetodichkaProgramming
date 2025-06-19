package client;

import common.dto.CaseInfoDTO;
import common.dto.PublicGameInfoDTO;
import common.dto.RoomDescriptionDTO;

// this was created to manage all user-facing console output. It renders information
// based on the current client state, separating display logic from core processing.
public class ClientUserInterface {

  private final ClientStateManager stateManager;
  private final Object consoleLock;

  public ClientUserInterface(ClientStateManager stateManager, Object consoleLock) {
    this.stateManager = stateManager;
    this.consoleLock = consoleLock;
  }

  // synchronizes all console output on a shared lock. This prevents messages
  // from different threads from interleaving and corrupting the display.
  public void displayMessage(String message) {
    synchronized (consoleLock) {
      System.out.println(message);
    }
  }

  public void displayRoomDescription(RoomDescriptionDTO dto) {
    synchronized (consoleLock) {
      System.out.print(dto.getFormattedDescription());
    }
  }

  // This central method displays the right menu/info by querying the ClientStateManager.
  public void printCurrentStateInfo() {
    synchronized (consoleLock) {
      ClientState state = stateManager.getCurrentState();

      switch (state) {
        case INITIAL_MENU:
          System.out.println("\n--- Main Menu ---");
          System.out.println("1. Add a Local Case File");
          System.out.println("2. Join/Host Multiplayer Game");
          System.out.println("3. Quit Game");
          break;
        case AUTH_MENU:
          System.out.println("\n--- Account ---");
          System.out.println("1. Login (Existing User)");
          System.out.println("2. Register (New User)");
          System.out.println("3. Back to Main Menu");
          break;
        case AWAITING_LOGIN_INPUT:
          System.out.println("\nPlease enter your credentials.");
          System.out.println("Usage:<username> <password>, or type 'back'");
          break;
        case AWAITING_REGISTER_INPUT:
          System.out.println("\nPlease choose a username and password.");
          System.out.println("Usage:<username> <password>, or type 'back'");
          break;
        case ADDING_CASE:
          System.out.println("\n--- Add Local Case ---");
          System.out.println(
              "Enter the full file path to the case JSON file (or type 'back'/'cancel'):");
          break;
        case MULTIPLAYER_MENU:
          System.out.println("\n--- Multiplayer Menu ---");
          System.out.println("1. Host Game");
          System.out.println("2. Join Game");
          System.out.println("3. Back to Main Menu");
          break;
        case JOIN_GAME_MENU:
          System.out.println("\n--- Join Game ---");
          System.out.println("1. Join Public Game");
          System.out.println("2. Join Private Game");
          System.out.println("3. Back to Multiplayer Menu");
          break;
        case SELECTING_CASE:
          System.out.println("\n--- Available Cases ---");
          var cases = stateManager.getAvailableCasesCache();
          if (cases.isEmpty()) {
            System.out.println("No cases found on the server. Type 'refresh' or 'back'.");
          } else {
            int i = 1;
            for (CaseInfoDTO caseInfo : cases) {
              System.out.printf("%d. %s\n", i++, caseInfo.getTitle());
            }
            System.out.println("Enter case number to select, or type 'refresh' or 'back'.");
          }
          break;
        case CHOOSING_HOST_MODE:
          System.out.println("\n--- Host Game ---");
          System.out.println("1. Host Public Game");
          System.out.println("2. Host Private Game");
          System.out.println("3. Back to Multiplayer Menu");
          break;
        case BROWSING_PUBLIC_GAMES:
          System.out.println("\n--- Public Games Available ---");
          var games = stateManager.getPublicGamesCache();
          if (games.isEmpty()) {
            System.out.println("No public games. Type 'back', 'refresh', or 'quit'.");
          } else {
            int i = 1;
            for (PublicGameInfoDTO g : games) {
              System.out.printf(
                  "%d. Case: %s (Host: %s)\n", i++, g.getCaseTitle(), g.getHostPlayerId());
            }
            System.out.println("Enter game number to join, 'back', 'refresh', or 'quit'.");
          }
          break;
        case ENTERING_PRIVATE_CODE:
          System.out.println("Enter 4-digit private code (or 'back'):");
          break;
        case CONNECTING:
          System.out.println("Attempting to connect to the server...");
          break;
        case WAITING_FOR_SERVER_INFO:
          System.out.println("Waiting for available case list from server...");
          break;
        case SUBMITTING_HOST_REQUEST:
          System.out.println("Sending host request...");
          break;
        case SUBMITTING_JOIN_REQUEST:
          System.out.println("Sending join request...");
          break;
        case HOST_LOBBY_WAITING:
        case GUEST_LOBBY_AWAITING_START:
          System.out.println(
              "In lobby, waiting for game to start... ('/chat [message]' or 'quit' available)");
          break;
        case GAME_STARTING:
          System.out.println("Game starting... Waiting for initial data from server.");
          break;
        case READY_TO_START_GAME_HOST:
          System.out.println("Game ready! As HOST, type 'start case' to begin.");
          break;
        case GUEST_READY_TO_START:
          System.out.println("Game ready! Waiting for the host to start the case.");
          break;
        case IN_GAME:
        case FINAL_EXAM_ACTIVE:
          break;
        case AWAITING_EXAM_QUESTION_HOST:
          System.out.println(
              "Host, enter your answer for Question "
                  + stateManager.getCurrentExamQuestionNumberBeingAnswered()
                  + ":");
          break;
        case SUBMITTING_EXAM_ANSWER_HOST:
          System.out.println("Submitting your answer... Please wait.");
          break;
        case VIEWING_EXAM_GUEST:
          System.out.println("Waiting for host to answer exam question... ('/chat' available)");
          break;
        case DISCONNECTED:
          System.out.println(
              "\nYou are disconnected. Type 'menu' to return to the main menu or 'quit' to exit.");
          break;
        case EXITING:
          System.out.println("Exiting game...");
          break;
        default:
          System.out.println("Current state: " + state);
          break;
      }
      // I print the prompt only after the state-specific info has been displayed.
      printPrompt();
    }
  }

  public void printPrompt() {
    synchronized (consoleLock) {
      if (shouldDisplayPrompt()) {
        System.out.print("> ");
      }
    }
  }

  // This helper determines if I should show a ">" prompt to the user.
  // I only display it for states that actively await user input.
  private boolean shouldDisplayPrompt() {
    ClientState state = stateManager.getCurrentState();
    return state == ClientState.INITIAL_MENU
        || state == ClientState.AUTH_MENU
        || state == ClientState.MULTIPLAYER_MENU
        || state == ClientState.JOIN_GAME_MENU
        || state == ClientState.ADDING_CASE
        || state == ClientState.SELECTING_CASE
        || state == ClientState.CHOOSING_HOST_MODE
        || state == ClientState.BROWSING_PUBLIC_GAMES
        || state == ClientState.ENTERING_PRIVATE_CODE
        || state == ClientState.AWAITING_LOGIN_INPUT
        || state == ClientState.AWAITING_REGISTER_INPUT
        || state == ClientState.HOST_LOBBY_WAITING
        || state == ClientState.GUEST_LOBBY_AWAITING_START
        || state == ClientState.READY_TO_START_GAME_HOST
        || state == ClientState.IN_GAME
        || state == ClientState.GUEST_READY_TO_START
        || state == ClientState.AWAITING_EXAM_QUESTION_HOST;
  }
}
