package Core;

import java.util.*;

public class DoctorWatson {
    private Random random = new Random();
    private Room currentRoom;
    private List<String> hints; // All hints loaded from JSON
    private List<String> remainingHints; // Hints not yet provided

    public DoctorWatson(List<String> hints) {
        this.hints = new ArrayList<>(hints); // Store all hints
        this.remainingHints = new ArrayList<>(hints); // Initialize with all hints
    }

    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { currentRoom = room; }

    public void randomMove(Building building) {
        Map<String, Room> neighbors = currentRoom.getNeighbors();
        List<Room> allowedRooms = new ArrayList<>(neighbors.values());
        if (!allowedRooms.isEmpty()) {
            currentRoom = allowedRooms.get(random.nextInt(allowedRooms.size()));
        }
    }

    public void provideHint() {
        if (remainingHints.isEmpty()) {
            // Reset the remaining hints if all have been used
            remainingHints.addAll(hints);
        }

        // Randomly select a hint from the remaining list
        int index = random.nextInt(remainingHints.size());
        String hint = remainingHints.remove(index); // Remove the hint after providing it

        System.out.println("Watson: " + hint);
    }
}