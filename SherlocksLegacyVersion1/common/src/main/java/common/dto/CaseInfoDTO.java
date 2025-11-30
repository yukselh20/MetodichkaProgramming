package common.dto;

import java.io.Serializable;
import java.util.Objects;

// This DTO carries basic, lightweight info about a case (title and description). I use it
// to populate lists of available cases without sending the entire large case file.
public class CaseInfoDTO implements Serializable {
  private static final long serialVersionUID = 6L;

  private final String title;
  private final String description;

  public CaseInfoDTO(String title, String description) {
    Objects.requireNonNull(title, "Case title cannot be null");
    if (title.isBlank()) {
      throw new IllegalArgumentException("Case title cannot be blank");
    }
    this.title = title;
    this.description = (description != null) ? description : "";
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseInfoDTO that = (CaseInfoDTO) o;
    // I consider case titles equal regardless of case for user-friendliness and duplicate checking.
    return title.equalsIgnoreCase(that.title);
  }

  @Override
  public int hashCode() {
    // I base the hash code on the lower-cased title to be consistent with the case-insensitive
    // `equals` method.
    return Objects.hash(title.toLowerCase());
  }
}
