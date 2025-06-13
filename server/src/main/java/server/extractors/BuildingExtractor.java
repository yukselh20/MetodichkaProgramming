package server.extractors;

import Core.Room;
import JsonDTO.CaseFile;
import java.util.*;
import java.util.stream.Collectors;
import server.GameContextServer;
import server.GameSession;

/**
 * A utility class responsible for parsing room and building structure data from a CaseFile object
 * and populating the game world within a GameContext.
 */
public class BuildingExtractor {

  private static final Set<String> VALID_DIRECTIONS =
      Set.of("north", "south", "east", "west", "up", "down");

  private BuildingExtractor() {}

  /**
   * Loads all rooms from a CaseFile, creates Room objects, and links them together. This method
   * performs a two-pass process: the first pass creates all Room instances, and the second pass
   * connects them as neighbors.
   */
  public static boolean loadBuilding(CaseFile caseFile, GameContextServer context) {
    Objects.requireNonNull(caseFile, "CaseFile cannot be null.");
    Objects.requireNonNull(context, "GameContextServer cannot be null.");

    GameSession parent = context.getParentSession();
    String sessionId = (parent != null) ? parent.getSessionId() : "[NoSession]";

    Map<String, Room> roomMap = new HashMap<>();
    Set<String> roomNames = new HashSet<>();
    boolean hasErrors = false;

    if (caseFile.getRooms() == null || caseFile.getRooms().isEmpty()) {
      System.err.println("Session [" + sessionId + "]: No rooms in CaseFile.");
      return false;
    }

    // First pass: Create all Room objects to ensure they exist before we try to link them.
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      if (roomData.getName() == null || roomData.getName().isBlank()) {
        System.err.println("Session [" + sessionId + "]: Room with blank name. Skipping.");
        hasErrors = true;
        continue;
      }
      String roomName = roomData.getName();
      String roomNameLower = roomName.toLowerCase();

      if (!roomNames.add(roomNameLower)) {
        System.err.println(
            "Session [" + sessionId + "]: Duplicate room name '" + roomName + "'. Skipping.");
        hasErrors = true;
        continue;
      }
      Room room = new Room(roomName, roomData.getDescription());
      roomMap.put(roomName, room);
      context.addRoom(room);
    }

    // Second pass: Link all the newly created rooms together as neighbors.
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      Room currentRoom = roomMap.get(roomData.getName());
      if (currentRoom == null || roomData.getNeighbors() == null) continue;

      for (Map.Entry<String, String> neighborEntry : roomData.getNeighbors().entrySet()) {
        String direction = neighborEntry.getKey();
        String neighborName = neighborEntry.getValue();

        if (direction == null
            || direction.isBlank()
            || neighborName == null
            || neighborName.isBlank()) {
          hasErrors = true;
          continue;
        }
        String directionLower = direction.toLowerCase();

        if (!VALID_DIRECTIONS.contains(directionLower)) {
          System.err.println(
              "Session ["
                  + sessionId
                  + "]: Invalid direction '"
                  + direction
                  + "' for room '"
                  + currentRoom.getName()
                  + "'.");
          hasErrors = true;
          continue;
        }
        Room neighbor = roomMap.get(neighborName);
        if (neighbor != null) {
          currentRoom.setNeighbor(directionLower, neighbor);
        } else {
          System.err.println(
              "Session ["
                  + sessionId
                  + "]: Neighbor room '"
                  + neighborName
                  + "' for room '"
                  + currentRoom.getName()
                  + "' not found.");
          hasErrors = true;
        }
      }
    }

    String startingRoomName = caseFile.getStartingRoom();
    if (startingRoomName == null || startingRoomName.isBlank()) {
      System.err.println("Session [" + sessionId + "]: No starting room defined in CaseFile.");
      return false;
    }
    Room startingRoom = context.getRoomByName(startingRoomName);
    if (startingRoom == null) {
      System.err.println(
          "Session ["
              + sessionId
              + "]: Starting room '"
              + startingRoomName
              + "' not loaded/found.");
      return false;
    }

    // After loading, a connectivity check ensures all rooms are reachable from the start.
    if (!hasErrors && !context.getRooms().isEmpty()) {
      try {
        validateRoomConnectivity(context, startingRoom, sessionId);
      } catch (IllegalStateException e) {
        System.err.println(
            "Session [" + sessionId + "]: Connectivity Validation Failed: " + e.getMessage());
        hasErrors = true;
      }
    } else if (context.getRooms().isEmpty()) {
      hasErrors = true;
    }

    return !hasErrors;
  }

  /**
   * Validates that all loaded rooms are interconnected and reachable from the starting room. It
   * performs a Breadth-First Search (BFS) traversal of the room graph.
   */
  private static void validateRoomConnectivity(
      GameContextServer context, Room startRoom, String sessionId) {
    Set<Room> visited = new HashSet<>();
    Queue<Room> queue = new LinkedList<>();
    Map<String, Room> allLoadedRooms = context.getRooms();

    if (startRoom == null || allLoadedRooms.isEmpty()) {
      throw new IllegalStateException(
          "Cannot validate connectivity: null start room or no rooms loaded.");
    }
    queue.add(startRoom);
    visited.add(startRoom);

    while (!queue.isEmpty()) {
      Room currentRoom = queue.poll();
      if (currentRoom.getNeighbors() != null) {
        for (Room neighbor : currentRoom.getNeighbors().values()) {
          if (neighbor != null && !visited.contains(neighbor)) {
            // This check ensures we don't try to traverse to a neighbor
            // that was defined in the JSON but failed to load.
            if (allLoadedRooms.containsValue(neighbor)) {
              visited.add(neighbor);
              queue.add(neighbor);
            } else {
              System.err.println(
                  "Session ["
                      + sessionId
                      + "]: Conn. Check Warning: Room '"
                      + currentRoom.getName()
                      + "' links to unloaded neighbor '"
                      + neighbor.getName()
                      + "'.");
            }
          }
        }
      }
    }

    // If the number of visited rooms does not match the total number of loaded
    // rooms, it means some rooms are unreachable, which is a critical map error.
    if (visited.size() != allLoadedRooms.size()) {
      Set<String> unreachable =
          allLoadedRooms.values().stream()
              .filter(r -> !visited.contains(r))
              .map(Room::getName)
              .collect(Collectors.toSet());
      throw new IllegalStateException(
          "Unreachable rooms from '" + startRoom.getName() + "': " + unreachable);
    }
  }
}
