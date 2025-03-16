package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;

public class ExamineCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        if (args.length < 2) {
            System.out.println("Usage: examine [object]");
            return;
        }

        String objectName = args[1].toLowerCase(); // Normalize object name
        Room currentRoom = context.getBuilding().getCurrentRoom();

        // Retrieve the objects from the current room
        GameObject obj = currentRoom.getObject(objectName);

        if (obj != null) {
            // Display the detailed description of the object
            System.out.println("You examine the " + objectName + ": " + obj.getExamine());

            // Optionally, add the object's description to the journal for reference
            context.getJournal().addEntry("Examined: " + objectName + " - " + obj.getExamine());
        } else {
            System.out.println("No such object in this room.");
        }
    }
}