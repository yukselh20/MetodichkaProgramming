package common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO sent from the server to a client in response to a request to host a game. It indicates
 * whether the hosting was successful and provides necessary details like the new game session ID
 * and a private code if applicable.
 */
public class HostGameResponseDTO implements Serializable {
  private static final long serialVersionUID = 10L;

  private final boolean success;
  private final String message;
  private final String privateCode;
  private final String gameSessionId;

  public HostGameResponseDTO(
      boolean success, String message, String privateCode, String gameSessionId) {
    this.success = success;
    this.message = Objects.requireNonNull(message, "Message cannot be null");
    // The private code and session ID are only relevant on a successful response.
    // This logic ensures they are null otherwise, preventing client-side confusion.
    this.privateCode = (success && privateCode != null) ? privateCode : null;
    this.gameSessionId = (success) ? gameSessionId : null;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }

  public String getPrivateCode() {
    return privateCode;
  }

  public String getGameSessionId() {
    return gameSessionId;
  }

  @Override
  public String toString() {
    // The private code is redacted in the string representation to prevent
    // it from being accidentally logged or displayed in debugging outputs.
    return "HostGameResponseDTO{"
        + "success="
        + success
        + ", message='"
        + message
        + '\''
        + ", privateCode='"
        + (privateCode != null ? "****" : "N/A")
        + '\''
        + ", gameSessionId='"
        + gameSessionId
        + '\''
        + '}';
  }
}
