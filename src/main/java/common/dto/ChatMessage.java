package common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * A DTO representing a single chat message sent between players or by the system. It is timestamped
 * on creation to maintain a chronological record.
 */
public class ChatMessage implements Serializable {
  private static final long serialVersionUID = 2L;

  private final String senderName;
  private final String message;
  private final LocalDateTime timestamp;

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  public ChatMessage(String senderName, String message) {
    Objects.requireNonNull(senderName, "Sender name cannot be null");
    Objects.requireNonNull(message, "Chat message content cannot be null");
    this.senderName = senderName;
    this.message = message;
    // The timestamp is automatically generated on the server upon message creation.
    this.timestamp = LocalDateTime.now();
  }

  // This constructor allows for creating messages with a specific timestamp,
  // which can be useful for testing or re-creating events.
  public ChatMessage(String senderName, String message, LocalDateTime timestamp) {
    Objects.requireNonNull(senderName, "Sender name cannot be null");
    Objects.requireNonNull(message, "Chat message content cannot be null");
    Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    this.senderName = senderName;
    this.message = message;
    this.timestamp = timestamp;
  }

  public String getSenderName() {
    return senderName;
  }

  public String getMessage() {
    return message;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  // Provides a standardized, user-friendly format for displaying the chat message.
  public String getFormattedMessage() {
    return String.format("[%s] %s: %s", timestamp.format(TIME_FORMATTER), senderName, message);
  }

  @Override
  public String toString() {
    return getFormattedMessage();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChatMessage that = (ChatMessage) o;
    // Two chat messages are considered equal if the sender and content match;
    // the timestamp is ignored for this comparison.
    return senderName.equals(that.senderName) && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(senderName, message);
  }
}
