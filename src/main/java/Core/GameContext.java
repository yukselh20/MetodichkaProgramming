package Core;

import JsonDTO.CaseFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameContext {
    private Map<String, Room> rooms = new HashMap<>(); // Runtime room instances
    private List<Suspect> suspects = new ArrayList<>(); // Live suspect positions
    private DoctorWatson watson;
    private Room currentRoom; // Player's current location
    private List<CaseFile> cases = new ArrayList<>();
    private Detective detective;
    private Journal journal;
    private TaskList taskList;
    private CaseFile selectedCase; // Centralized case reference
    private boolean exitCurrentGame = false;
    private boolean caseStarted = false;
    private boolean inCaseSelectionMenu;

    public GameContext(
            Detective detective,
            DoctorWatson watson,
            Journal journal,
            TaskList taskList,
            CaseFile selectedCase) {
        this.detective = detective;
        this.watson = watson;
        this.journal = journal;
        this.taskList = taskList;
        this.selectedCase = selectedCase;
    }

    /**
     * Moves the player to a neighboring room.
     *
     * @param direction The direction to move (e.g., "north", "south").
     * @return The new room if the move is valid, or null otherwise.
     */
    public Room move(String direction) {
        direction = direction.toLowerCase();
        Room nextRoom = currentRoom.getNeighbor(direction);
        if (nextRoom != null) {
            currentRoom = nextRoom;
            return nextRoom;
        }
        return null;
    }

    /**
     * Retrieves a suspect by name.
     *
     * @param name The name of the suspect.
     * @return The suspect object if found, or null otherwise.
     */
    public Suspect getSuspect(String name) {
        for (Suspect suspect : suspects) {
            if (suspect.getName().equalsIgnoreCase(name)) { // Case-insensitive match
                return suspect;
            }
        }
        return null;
    }

    /**
     * Updates the positions of suspects and Watson.
     */
    public void updateMovements() {
        // Move suspects
        for (Suspect suspect : suspects) {
            suspect.randomMove(this); // Pass the context to allow access to rooms
        }

        // Move Watson
        if (watson != null) {
            watson.randomMove(this); // Pass the context to allow access to rooms
        }
    }

    /**
     * Sets Watson's starting room and registers him with the game context.
     *
     * @param watson The DoctorWatson instance.
     */
    public void setWatson(DoctorWatson watson) {
        this.watson = watson;
        this.watson.setCurrentRoom(this.currentRoom); // Sync with the current room
    }

    /**
     * Provides a description of occupants in the current room.
     *
     * @return A string describing the occupants in the current room.
     */
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

    /**
     * Adds a room to the game context.
     *
     * @param room The room to add.
     */
    public void addRoom(Room room) {
        rooms.put(room.getName(), room);
    }

    /**
     * Retrieves a room by its name.
     *
     * @param name The name of the room.
     * @return The room object if found, or null otherwise.
     */
    public Room getRoomByName(String name) {
        return rooms.get(name);
    }

    /**
     * Adds a suspect to the game context.
     *
     * @param suspect The suspect to add.
     */
    public void addSuspect(Suspect suspect) {
        suspects.add(suspect);
    }

    // Getters and Setters
    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        currentRoom = room;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Detective getDetective() {
        return detective;
    }

    public DoctorWatson getWatson() {
        return watson;
    }

    public Journal getJournal() {
        return journal;
    }

    public TaskList getTaskList() {
        return taskList;
    }

    public CaseFile getSelectedCase() {
        return selectedCase;
    }

    public boolean isInCaseSelectionMenu() {
        return inCaseSelectionMenu;
    }

    public void setInCaseSelectionMenu(boolean inCaseSelectionMenu) {
        this.inCaseSelectionMenu = inCaseSelectionMenu;
    }

    public boolean isExitCurrentGame() {
        return exitCurrentGame;
    }

    public void setExitCurrentGame(boolean exit) {
        this.exitCurrentGame = exit;
    }

    public boolean isCaseStarted() {
        return caseStarted;
    }

    public void setCaseStarted(boolean caseStarted) {
        this.caseStarted = caseStarted;
    }
}