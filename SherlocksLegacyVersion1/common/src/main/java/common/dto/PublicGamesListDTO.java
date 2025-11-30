package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// I send this DTO from the server to the client. It contains a list of all
// open public game lobbies that a player can join.
public class PublicGamesListDTO implements Serializable {
  private static final long serialVersionUID = 13L;

  private final List<PublicGameInfoDTO> publicGames;

  public PublicGamesListDTO(List<PublicGameInfoDTO> publicGames) {
    Objects.requireNonNull(publicGames, "Public games list cannot be null");
    this.publicGames = Collections.unmodifiableList(new ArrayList<>(publicGames));
  }

  public List<PublicGameInfoDTO> getPublicGames() {
    return publicGames;
  }
}
