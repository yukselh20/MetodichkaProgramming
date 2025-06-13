package common.dto;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A DTO that carries a complete, formatted description of a room, including its name, descriptive
 * text, and lists of objects, occupants, and exits. It is generated on the server and sent to the
 * client for display.
 */
public class RoomDescriptionDTO implements Serializable {
  private static final long serialVersionUID = 4L;

  private final String roomName;
  private final String description;
  private final List<String> objects;
  private final List<String> occupants;
  private final Map<String, String> exits;

  public RoomDescriptionDTO(
      String roomName,
      String description,
      List<String> objects,
      List<String> occupants,
      Map<String, String> exits) {
    this.roomName = Objects.requireNonNull(roomName, "Room name cannot be null");
    this.description = Objects.requireNonNull(description, "Room description cannot be null");
    this.objects =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(objects, "Objects list cannot be null")));
    this.occupants =
        Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(occupants, "Occupants list cannot be null")));
    this.exits =
        Collections.unmodifiableMap(
            new HashMap<>(Objects.requireNonNull(exits, "Exits map cannot be null")));
  }

  public String getRoomName() {
    return roomName;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getObjects() {
    return objects;
  }

  public List<String> getOccupants() {
    return occupants;
  }

  public Map<String, String> getExits() {
    return exits;
  }

  // This method centralizes the logic for formatting the room description. By
  // doing this on the server-side within the DTO, it ensures a consistent
  // display format without duplicating logic on the client.
  public String getFormattedDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== ").append(roomName).append(" ===\n");
    sb.append(description).append("\n\n");

    sb.append("Objects present: ");
    if (objects.isEmpty()) {
      sb.append("None\n");
    } else {
      sb.append(String.join(", ", objects)).append("\n");
    }

    sb.append("Occupants: ");
    if (occupants.isEmpty()) {
      sb.append("None\n");
    } else {
      sb.append(String.join(", ", occupants)).append("\n");
    }

    sb.append("Exits: ");
    if (exits.isEmpty()) {
      sb.append("None\n");
    } else {
      String exitString =
          exits.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
              .collect(Collectors.joining(", "));
      sb.append(exitString).append("\n");
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return getFormattedDescription();
  }
}
