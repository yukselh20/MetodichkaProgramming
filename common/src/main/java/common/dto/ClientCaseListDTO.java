package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A DTO sent from a client to the server, containing a list of basic information about all the case
 * files the client has stored locally.
 */
public class ClientCaseListDTO implements Serializable {
  private static final long serialVersionUID = 7L;

  private final List<CaseInfoDTO> cases;

  public ClientCaseListDTO(List<CaseInfoDTO> cases) {
    Objects.requireNonNull(cases, "Case list cannot be null");
    // The list is made unmodifiable to ensure the DTO's state is immutable
    // after creation, a best practice for safe data transfer.
    this.cases = Collections.unmodifiableList(new ArrayList<>(cases));
  }

  public List<CaseInfoDTO> getCases() {
    return cases;
  }
}
