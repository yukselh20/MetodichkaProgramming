package Core;

import JsonDTO.CaseFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Building {
    protected Map<String, Room> rooms = new HashMap<>(); // Runtime room instances
    protected List<Suspect> suspects = new ArrayList<>(); // Live suspect positions
    protected DoctorWatson watson;
    protected Room currentRoom; // Player's current location
    protected List<CaseFile> cases = new ArrayList<>();

    public Building() {
        rooms = new HashMap<>();
    }

    public Room move(String direction) {
        direction = direction.toLowerCase();
        Room nextRoom = currentRoom.getNeighbor(direction);
        if (nextRoom != null) {
            currentRoom = nextRoom;
            return nextRoom;
        }
        return null;
    }

    public Suspect getSuspect(String name) {
        for (Suspect suspect : suspects) {
            if (suspect.getName().equalsIgnoreCase(name)) { // Case-insensitive match
                return suspect;
            }
        }
        return null;
    }

    // Update suspect and Watson positions
    public void updateMovements(DoctorWatson watson) {
        // Move suspects
        for (Suspect suspect : suspects) {
            suspect.randomMove(); // No parameter needed
        }

        // Move Watson
        if (watson != null) {
            watson.randomMove(); // No parameter needed
        }
    }

    // Set Watson in the building
    public void setWatson(DoctorWatson watson) {
        this.watson = watson;
        this.watson.setCurrentRoom(this.currentRoom); // Sync with building's current room
    }

    public String getOccupantsDescription() {
        StringBuilder sb = new StringBuilder();
        List<String> occupants = new ArrayList<>();

        // Check suspects in the current room
        for (Suspect suspect : suspects) {
            if (suspect.getCurrentRoom() == currentRoom) {
                occupants.add(suspect.getName());
            }
        }

        // Check if Watson is in the current room
        if (watson != null && watson.getCurrentRoom() == currentRoom) {
            occupants.add("Dr. Watson");
        }

        if (!occupants.isEmpty()) {
            sb.append("Occupants: ").append(String.join(", ", occupants));
        } else {
            sb.append("Occupants: None");
        }

        return sb.toString();
    }

    public void addRoom(Room room) {
        rooms.put(room.getName(), room); // Requires Room.getName()
    }

    public Room getRoomByName(String name) {
        return rooms.get(name);
    }

    public void addSuspect(Suspect suspect) {
        suspects.add(suspect);
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        currentRoom = room;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }
}
