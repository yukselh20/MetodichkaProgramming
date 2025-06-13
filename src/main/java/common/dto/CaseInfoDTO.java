package common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO that carries basic, lightweight information about a case, specifically its title and
 * description. This is used to populate lists of available cases without sending the entire, large
 * case file.
 */
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
    // Ensures the description field is never null, simplifying client-side handling.
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
    // Case titles are considered equal regardless of their case for
    // user-friendliness and duplicate checking.
    return title.equalsIgnoreCase(that.title);
  }

  @Override
  public int hashCode() {
    // The hash code is based on the lower-cased title to be consistent
    // with the case-insensitive `equals` method.
    return Objects.hash(title.toLowerCase());
  }
}
