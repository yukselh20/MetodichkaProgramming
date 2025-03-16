package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;

import java.util.Map;

public class MoveCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        // Validate input
        if (args.length < 2) {
            System.out.println("Usage: move [north|south|east|west|up|down]");
            return;
        }

        // Attempt to move the player
        String direction = args[1].toLowerCase();
        Room newRoom = context.getBuilding().move(direction);
        if (newRoom == null) {
            System.out.println("You can't move in that direction.");
            return;
        }

        // Update suspects and Watson movements
        context.getBuilding().updateMovements(context.getWatson());

        // Display updated room information
        System.out.println(newRoom.getDescription()); // Room description

        // Print notable features (if any)
        System.out.println("\nNotable features:");
        for (GameObject obj : newRoom.getObjects().values()) {
            System.out.println("- " + obj.getDescription());
        }

        // Print objects present
        System.out.println("\n" + newRoom.getObjectsDescription());

        // Print exits
        System.out.println(newRoom.getExitsDescription());

        // Print occupants
        System.out.println(context.getBuilding().getOccupantsDescription());
    }
}