package Extractors;

import Core.GameContext;
import Core.Room;
import JsonDTO.CaseFile;
import java.util.*;

public class BuildingExtractor {

  // Define a set of valid directions
  private static final Set<String> VALID_DIRECTIONS = new HashSet<>();

  static {
    VALID_DIRECTIONS.add("north");
    VALID_DIRECTIONS.add("south");
    VALID_DIRECTIONS.add("east");
    VALID_DIRECTIONS.add("west");
    VALID_DIRECTIONS.add("up");
    VALID_DIRECTIONS.add("down");
  }

  /**
   * Loads the building structure from the case file and populates the GameContext.
   *
   * @param caseFile The case file containing room and neighbor data.
   * @param context The GameContext to populate with the building structure.
   * @return True if the building was successfully loaded, false otherwise.
   */
  public static boolean loadBuilding(CaseFile caseFile, GameContext context) {
    Map<String, Room> roomMap = new HashMap<>();
    Set<String> roomNames = new HashSet<>();
    boolean hasErrors = false; // Flag to track if there are any errors

    // Create rooms using concrete subclasses based on JSON data
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      String roomName = roomData.getName();

      // Validate for duplicate room names
      if (!roomNames.add(roomName)) { // `add()` returns false if the name already exists
        System.out.println("Duplicate room name '" + roomName + "' found. Skipping this room.");
        hasErrors = true; // Mark that there was an error
        continue; // Skip this room
      }

      Room room = createRoomFromData(roomData);
      roomMap.put(roomName, room);
      context.addRoom(room); // Add room directly to GameContext
    }

    // Link neighbors with validation
    for (CaseFile.RoomData roomData : caseFile.getRooms()) {
      Room currentRoom = roomMap.get(roomData.getName());
      for (Map.Entry<String, String> neighborEntry : roomData.getNeighbors().entrySet()) {
        String direction = neighborEntry.getKey().toLowerCase();
        String neighborName = neighborEntry.getValue();

        // Validate the direction
        if (!VALID_DIRECTIONS.contains(direction)) {
          System.out.println(
              "Invalid direction '"
                  + direction
                  + "' for room '"
                  + roomData.getName()
                  + "'. Skipping.");
          hasErrors = true; // Mark that there was an error
          continue; // Skip this neighbor
        }

        // Validate the neighbor room
        Room neighbor = roomMap.get(neighborName);
        if (neighbor != null) {
          currentRoom.setNeighbor(direction, neighbor);
        } else {
          System.out.println(
              "Neighbor room '"
                  + neighborName
                  + "' not found for room '"
                  + roomData.getName()
                  + "'. Skipping.");
          hasErrors = true; // Mark that there was an error
        }
      }
    }

    // Validate starting room
    Room startingRoom = roomMap.get(caseFile.getStartingRoom());
    if (startingRoom == null) {
      System.out.println(
          "Starting room '"
              + caseFile.getStartingRoom()
              + "' not found. Exiting to case selection menu.");
      return false;
    }
    context.setCurrentRoom(startingRoom);

    // Validate room connectivity
    try {
      validateRoomConnectivity(context, startingRoom);
    } catch (IllegalStateException e) {
      System.out.println(e.getMessage());
      return false;
    }

    // If there were errors, return false to indicate failure
    return !hasErrors;
  }

  private static Room createRoomFromData(CaseFile.RoomData roomData) {
    return new Room(roomData.getName(), roomData.getDescription()) {
      @Override
      public String examine(String objectName) {
        return "Default examination text for " + objectName;
      }
    };
  }

  /**
   * Validates that all rooms in the building are reachable from the starting room.
   *
   * @param context The GameContext containing all rooms.
   * @param startRoom The starting room for traversal.
   * @throws IllegalStateException If any room is unreachable.
   */
  private static void validateRoomConnectivity(GameContext context, Room startRoom) {
    Set<Room> visited = new HashSet<>();
    Queue<Room> queue = new LinkedList<>();

    // Start BFS from the starting room
    queue.add(startRoom);
    visited.add(startRoom);

    while (!queue.isEmpty()) {
      Room currentRoom = queue.poll();
      for (Room neighbor : currentRoom.getNeighbors().values()) {
        if (!visited.contains(neighbor)) {
          visited.add(neighbor);
          queue.add(neighbor);
        }
      }
    }

    // Check if all rooms were visited
    for (Room room : context.getRooms().values()) {
      if (!visited.contains(room)) {
        throw new IllegalStateException(
            "Room '" + room.getName() + "' is unreachable from the starting room.");
      }
    }
  }
}
