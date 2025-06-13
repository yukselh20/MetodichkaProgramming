package Core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a single location in the game world. A room has a name, a description, and contains
 * collections of interactable objects and exits.
 */
public class Room {
  protected final String name;
  protected String description;
  // Maps are used for efficient lookup of neighbors and objects by their names.
  protected final Map<String, Room> neighbors = new HashMap<>();
  protected final Map<String, GameObject> objects = new HashMap<>();

  public Room(String name, String description) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Room name cannot be null or blank.");
    }
    this.name = name;
    this.description = (description != null) ? description : "An undescribed area.";
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  // All returned collections are wrapped in unmodifiable views to protect
  // the room's internal state from external modification.
  public Map<String, Room> getNeighbors() {
    return Collections.unmodifiableMap(neighbors);
  }

  public void setNeighbor(String direction, Room neighbor) {
    Objects.requireNonNull(direction, "Direction cannot be null");
    Objects.requireNonNull(neighbor, "Neighbor room cannot be null");
    if (direction.isBlank()) throw new IllegalArgumentException("Direction cannot be blank.");
    neighbors.put(direction.toLowerCase(), neighbor);
  }

  public Room getNeighbor(String direction) {
    if (direction == null) return null;
    return neighbors.get(direction.toLowerCase());
  }

  public GameObject getObject(String name) {
    if (name == null) return null;
    return objects.get(name.toLowerCase());
  }

  public void addObject(String name, GameObject object) {
    Objects.requireNonNull(name, "Object name cannot be null");
    Objects.requireNonNull(object, "GameObject cannot be null");
    if (name.isBlank()) throw new IllegalArgumentException("Object name cannot be blank.");
    objects.put(name.toLowerCase(), object);
  }

  public Map<String, GameObject> getObjects() {
    return Collections.unmodifiableMap(objects);
  }

  // Generates a formatted string of all exits for display to the user.
  public String getExitsDescription() {
    if (neighbors.isEmpty()) return "Exits: None";
    return "Exits: "
        + neighbors.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + " (" + e.getValue().getName() + ")")
            .collect(Collectors.joining(", "));
  }

  // Generates a formatted string of all objects in the room.
  public String getObjectsDescription() {
    if (objects.isEmpty()) return "No objects of interest here.";
    return "Objects present: "
        + objects.values().stream()
            .map(GameObject::getName)
            .sorted()
            .collect(Collectors.joining(", "));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Room room = (Room) o;
    // Two rooms are considered the same if they have the same name.
    return name.equals(room.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return "Room{name='" + name + "'}";
  }
}
