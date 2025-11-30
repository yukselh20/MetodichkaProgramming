package common.dto;

import java.io.Serializable;
import java.util.Objects;

// I send this DTO from the server in response to a host game request. It indicates
// success and provides the new game session ID and a private code if applicable.
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
