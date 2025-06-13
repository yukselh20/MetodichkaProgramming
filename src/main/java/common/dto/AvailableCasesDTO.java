package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Data Transfer Object sent from the server to the client, containing a list of all unique cases
 * available for hosting a game.
 */
public class AvailableCasesDTO implements Serializable {
  private static final long serialVersionUID = 8L;

  private final List<CaseInfoDTO> uniqueCases;

  public AvailableCasesDTO(List<CaseInfoDTO> uniqueCases) {
    Objects.requireNonNull(uniqueCases, "Unique case list cannot be null");
    // The list is wrapped in an unmodifiable view to ensure immutability,
    // which is a good practice for DTOs to prevent unintended side effects.
    this.uniqueCases = Collections.unmodifiableList(new ArrayList<>(uniqueCases));
  }

  public List<CaseInfoDTO> getUniqueCases() {
    return uniqueCases;
  }
}
