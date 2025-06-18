package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// I use this DTO to broadcast updates about the lobby state, like when a player joins.
public class LobbyUpdateDTO implements Serializable {
  private static final long serialVersionUID = 17L;

  private final String message;
  private final List<String> currentPlayerIds;

  public LobbyUpdateDTO(String message, List<String> currentPlayerIds) {
    this.message = Objects.requireNonNull(message, "Update message cannot be null");
    // Making the list unmodifiable ensures this DTO is immutable.
    this.currentPlayerIds =
        (currentPlayerIds != null)
            ? Collections.unmodifiableList(new ArrayList<>(currentPlayerIds))
            : Collections.emptyList();
  }

  public String getMessage() {
    return message;
  }

  public List<String> getCurrentPlayerIds() {
    return currentPlayerIds;
  }
}
