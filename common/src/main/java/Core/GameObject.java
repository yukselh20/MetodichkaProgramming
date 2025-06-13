package Core;

import java.util.Objects;

/**
 * Represents any interactable object or feature within a room. Each object has a name, a general
 * description, and potentially more detailed text for 'examine' and 'deduce' actions.
 */
public class GameObject {
  private final String name;
  private final String description;
  private final String examineText;
  private final String deduceText;

  public GameObject(String name, String description, String examineText, String deduceText) {
    this.name = Objects.requireNonNull(name, "GameObject name cannot be null.");
    if (name.isBlank()) throw new IllegalArgumentException("GameObject name cannot be blank.");

    this.description =
        Objects.requireNonNull(
            description, "GameObject description for '" + name + "' cannot be null.");

    // If specific 'examine' or 'deduce' text isn't provided, the object falls
    // back to a default value. This makes the case JSON files more flexible.
    this.examineText = (examineText != null && !examineText.isBlank()) ? examineText : description;
    this.deduceText =
        (deduceText != null && !deduceText.isBlank())
            ? deduceText
            : "There's nothing more to deduce from this.";
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getExamine() {
    return examineText;
  }

  public String getDeduce() {
    return deduceText;
  }
}
