package Core;

import java.util.*;

public class Suspect {
    private String name;
    private String statement;
    private String clue;
    protected Room currentRoom;
    protected Random random = new Random();

    public Suspect(String name, String statement, String clue) {
        this.name = name;
        this.statement = statement;
        this.clue = clue;
    }

    public String getName() { return name; }
    public String getStatement() { return statement; }
    public String getClue() { return clue; }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { currentRoom = room; }

    //randomMove method to allow movement to any neighboring room only
    public void randomMove(Building building) {
        Map<String, Room> neighbors = currentRoom.getNeighbors(); // Get neighboring rooms
        if (!neighbors.isEmpty()) {
            List<Room> allowedRooms = new ArrayList<>(neighbors.values()); // Convert to list
            int index = random.nextInt(allowedRooms.size()); // Randomly select a neighbor
            currentRoom = allowedRooms.get(index); // Move to the selected neighbor
        }
    }
}