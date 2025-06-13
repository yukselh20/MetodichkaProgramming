package common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO sent from the server to a client in response to a request to join a game. It confirms
 * whether the join attempt was successful and provides the session ID.
 */
public class JoinGameResponseDTO implements Serializable {
  private static final long serialVersionUID = 16L;

  private final boolean success;
  private final String message;
  private final String gameSessionId;

  public JoinGameResponseDTO(boolean success, String message, String gameSessionId) {
    this.success = success;
    this.message = Objects.requireNonNull(message, "Message cannot be null");
    // The game session ID is only sent if the join operation was successful.
    this.gameSessionId = success ? gameSessionId : null;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public String getGameSessionId() {
    return gameSessionId;
  }
}
