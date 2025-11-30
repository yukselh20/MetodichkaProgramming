package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// This DTO is sent from the server to the client. It contains a list of all unique cases
// available for hosting a game.
public class AvailableCasesDTO implements Serializable {
  private static final long serialVersionUID = 8L;

  private final List<CaseInfoDTO> uniqueCases;

  public AvailableCasesDTO(List<CaseInfoDTO> uniqueCases) {
    Objects.requireNonNull(uniqueCases, "Unique case list cannot be null");
    // I wrap the list in an unmodifiable view to ensure immutability.
    this.uniqueCases = Collections.unmodifiableList(new ArrayList<>(uniqueCases));
  }

  public List<CaseInfoDTO> getUniqueCases() {
    return uniqueCases;
  }
}
