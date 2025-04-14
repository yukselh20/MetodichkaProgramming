package Extractors;

import Core.Building;
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

    public static Building loadBuilding(CaseFile caseFile) {
        Building building = new Building() {}; // Anonymous class for generic building
        Map<String, Room> roomMap = new HashMap<>();
        boolean hasErrors = false; // Flag to track if there are any errors

        Set<String> roomNames = new HashSet<>();

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
            building.addRoom(room);
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

        // If there were errors, return null to indicate failure
        if (hasErrors) {
            System.out.println(
                    "Errors encountered while loading the case. Exiting to case selection menu.");
            return null;
        }

        // Set starting room
        Room startingRoom = roomMap.get(caseFile.getStartingRoom());
        if (startingRoom == null) {
            System.out.println(
                    "Starting room '"
                            + caseFile.getStartingRoom()
                            + "' not found. Exiting to case selection menu.");
            return null;
        }
        building.setCurrentRoom(startingRoom);

        return building;
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
     * @param building The building containing all rooms.
     * @param startRoom The starting room for traversal.
     */
    private static void validateRoomConnectivity(Building building, Room startRoom) {
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
        for (Room room : building.getRooms().values()) {
            if (!visited.contains(room)) {
                throw new IllegalStateException(
                        "Room '" + room.getName() + "' is unreachable from the starting room.");
            }
        }
    }
}
