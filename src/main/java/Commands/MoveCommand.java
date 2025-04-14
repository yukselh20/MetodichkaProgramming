package Commands;

import Core.Building;
import Core.GameContext;
import Core.GameObject;
import Core.Room;
import java.util.Map;

public class MoveCommand extends BaseCommand {
    public MoveCommand() {
        super(true); // Requires the case to be started
    }
    @Override
    public void executeCommand(String[] args, GameContext context) {

        // Validate input
        if (args.length < 2) {
            System.out.println("Usage: move [north|south|east|west|up|down]");
            return;
        }

        // Extract direction from arguments
        String direction = args[1].toLowerCase();

        // Attempt to move the player
        Building building = context.getBuilding();
        Room newRoom = building.move(direction);

        if (newRoom == null) {
            System.out.println("You can't move in that direction.");
            return;
        }

        // Update suspect and Watson movements
        building.updateMovements(context.getWatson());

        // Display updated room information
        displayRoomInformation(newRoom, building);
    }

    private void displayRoomInformation(Room room, Building building) {
        // Print room description
        System.out.println("\n" + room.getDescription());

        // Print notable features (objects in the room)
        System.out.println("\nNotable features:");
        Map<String, GameObject> objects = room.getObjects();
        if (objects.isEmpty()) {
            System.out.println("- No notable features in this room.");
        } else {
            for (GameObject obj : objects.values()) {
                System.out.println("- " + obj.getDescription());
            }
        }

        // Print objects present in the room
        System.out.println("\n" + room.getObjectsDescription());

        // Print exits
        System.out.println(room.getExitsDescription());

        // Print occupants in the room
        System.out.println(building.getOccupantsDescription());
    }

    public String getDescription() {
        return "Move north, south, east, west, up, or down.";
    }
}
