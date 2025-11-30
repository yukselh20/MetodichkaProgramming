package common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

// This DTO represents a single, structured entry in the game's journal. I use it
// to broadcast journal updates to all players in a session.
public class JournalEntryDTO implements Serializable {
  private static final long serialVersionUID = 5L;

  private final String entryText;
  private final String contributorId;
  private final LocalDateTime timestamp;

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  public JournalEntryDTO(String entryText, String contributorId, LocalDateTime timestamp) {
    this.entryText = Objects.requireNonNull(entryText, "Entry text cannot be null");
    this.contributorId = Objects.requireNonNull(contributorId, "Contributor ID cannot be null");
    this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
  }

  public String getEntryText() {
    return entryText;
  }

  public String getContributorId() {
    return contributorId;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getFormattedEntry() {
    return String.format("[%s] %s: %s", timestamp.format(TIME_FORMATTER), contributorId, entryText);
  }

  @Override
  public String toString() {
    return getFormattedEntry();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JournalEntryDTO that = (JournalEntryDTO) o;
    return entryText.equals(that.entryText)
        && contributorId.equals(that.contributorId)
        && timestamp.equals(that.timestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entryText, contributorId, timestamp);
  }
}
