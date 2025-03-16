package Core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Building {
    protected Map<String, Room> rooms = new HashMap<>();
    protected List<Suspect> suspects = new ArrayList<>();
    protected DoctorWatson watson;
    protected Room currentRoom;
    protected List<CaseFile> cases;

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

    public void updateMovements(DoctorWatson watson) {
        // Move suspects
        for (Suspect suspect : suspects) {
            suspect.randomMove(this);
        }

        // Move Watson
        if (watson != null) {
            watson.randomMove(this);
        }
    }

    // Set Watson in the building
    public void setWatson(DoctorWatson watson) {
        this.watson = watson;
        this.watson.setCurrentRoom(this.currentRoom);
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

    public void addCase(CaseFile newCase) {
        cases.add(newCase);
    }

    public Room getRoomByName(String name) {
        return rooms.get(name);
    }
    public void addSuspect(Suspect suspect) {
        suspects.add(suspect);
    }

    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { currentRoom = room; }

    public Map<String, Room> getRooms() { return rooms; }
}