package Commands;

import Core.GameContext;
import Core.Room;
import java.util.Map;

public class LookCommand extends BaseCommand {
    public LookCommand() {
        super(true); // Requires the case to be started
    }

    @Override
    protected void executeCommand(String[] args, GameContext context) {
        Room current = context.getCurrentRoom(); // Directly use getCurrentRoom from GameContext
        System.out.println(current.getDescription());
        System.out.println(current.getObjectsDescription());
        System.out.println(context.getOccupantsDescription()); // Get occupants description

        // Format exits
        Map<String, Room> neighbors = current.getNeighbors();
        if (!neighbors.isEmpty()) {
            System.out.print("Exits: ");
            for (Map.Entry<String, Room> entry : neighbors.entrySet()) {
                System.out.print(entry.getKey() + " (" + entry.getValue().getName() + ") ");
            }
            System.out.println(); // Newline after exits
        } else {
            System.out.println("Exits: None");
        }
    }

    @Override
    public String getDescription() {
        return "View your surroundings.";
    }
}