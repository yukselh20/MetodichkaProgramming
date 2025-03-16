package Extractors;

import Core.Building;
import Core.Room;
import Core.CaseFile;
import java.util.HashMap;
import java.util.Map;

public class BuildingExtractor {
    public static Building loadBuilding(CaseFile caseFile) {
        Building building = new Building() {};
        Map<String, Room> roomMap = new HashMap<>();

        // Create rooms using concrete subclasses based on JSON data
        for (CaseFile.RoomData roomData : caseFile.getRooms()) {
            Room room = createRoomFromData(roomData);
            roomMap.put(room.getName(), room);
            building.addRoom(room);
        }

        // Link neighbors
        for (CaseFile.RoomData roomData : caseFile.getRooms()) {
            Room currentRoom = roomMap.get(roomData.getName());
            roomData.getNeighbors().forEach((direction, neighborName) -> {
                Room neighbor = roomMap.get(neighborName);
                if (neighbor != null) {
                    currentRoom.setNeighbor(direction, neighbor);
                }
            });
        }

        // Set starting room
        building.setCurrentRoom(roomMap.get(caseFile.getStartingRoom()));
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
}