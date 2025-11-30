package common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// this DTO is sent from a client to the server. It contains a list of basic info
// about all the case files the client has stored locally.
public class ClientCaseListDTO implements Serializable {
  private static final long serialVersionUID = 7L;

  private final List<CaseInfoDTO> cases;

  public ClientCaseListDTO(List<CaseInfoDTO> cases) {
    Objects.requireNonNull(cases, "Case list cannot be null");
    this.cases = Collections.unmodifiableList(new ArrayList<>(cases));
  }

  public List<CaseInfoDTO> getCases() {
    return cases;
  }
}
