package common.dto;

import java.io.Serializable;
import java.util.Objects;

// This DTO holds public info about an open game lobby. I use it to display a list of available
// games.
public class PublicGameInfoDTO implements Serializable {
  private static final long serialVersionUID = 12L;

  private final String hostPlayerId;
  private final String caseTitle;
  private final String gameSessionId;

  public PublicGameInfoDTO(String hostPlayerId, String caseTitle, String gameSessionId) {
    this.hostPlayerId = Objects.requireNonNull(hostPlayerId, "Host Player ID cannot be null");
    this.caseTitle = Objects.requireNonNull(caseTitle, "Case Title cannot be null");
    this.gameSessionId = Objects.requireNonNull(gameSessionId, "Game Session ID cannot be null");
  }

  public String getHostPlayerId() {
    return hostPlayerId;
  }

  public String getCaseTitle() {
    return caseTitle;
  }

  public String getGameSessionId() {
    return gameSessionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PublicGameInfoDTO that = (PublicGameInfoDTO) o;
    // The game session ID is the unique identifier for a game lobby.
    return gameSessionId.equals(that.gameSessionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gameSessionId);
  }
}
