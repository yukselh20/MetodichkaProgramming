package client;

// enum to define the set of states for my client-side state machine.
// Each state dictates how I interpret user input and what UI is displayed, guiding the user
// through the game from the main menu to in-game actions.
public enum ClientState {
  // Initial Menus
  INITIAL_MENU,
  AUTH_MENU,
  AWAITING_LOGIN_INPUT,
  AWAITING_REGISTER_INPUT,
  MULTIPLAYER_MENU,
  JOIN_GAME_MENU,

  // Pre-Game Setup and Selection
  ADDING_CASE,
  SELECTING_CASE,
  CHOOSING_HOST_MODE,
  BROWSING_PUBLIC_GAMES,
  ENTERING_PRIVATE_CODE,

  // Transitional / Waiting States
  CONNECTING,
  WAITING_FOR_SERVER_INFO,
  SUBMITTING_HOST_REQUEST,
  SUBMITTING_JOIN_REQUEST,
  HOST_LOBBY_WAITING,
  GUEST_LOBBY_AWAITING_START,
  GAME_STARTING,

  // Active Gameplay
  READY_TO_START_GAME_HOST,
  GUEST_READY_TO_START,
  IN_GAME,

  // Final Exam Flow
  FINAL_EXAM_ACTIVE,
  AWAITING_EXAM_QUESTION_HOST,
  VIEWING_EXAM_GUEST,
  SUBMITTING_EXAM_ANSWER_HOST,

  // Terminal States
  DISCONNECTED,
  EXITING
}
