package common.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A simple DTO for sending generic, one-off text messages from the server to the client for
 * display, such as error messages or status updates.
 */
public class TextMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String content;

  public TextMessage(String content) {
    Objects.requireNonNull(content, "Message content cannot be null");
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TextMessage that = (TextMessage) o;
    return content.equals(that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content);
  }
}
