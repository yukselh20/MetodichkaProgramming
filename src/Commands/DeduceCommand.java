package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;

public class DeduceCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        if (args.length < 2) {
            System.out.println("Usage: deduce [object]");
            return;
        }

        String objectName = args[1].toLowerCase();
        Room currentRoom = context.getBuilding().getCurrentRoom();

        GameObject obj = currentRoom.getObject(objectName);

        if (obj != null) {
            String clue = obj.deduce();
            System.out.println("Deduction: " + clue);
            context.getJournal().addEntry("Clue: " + clue);

            // Ensure Detective has incrementDeduceCount() and getDeduceCount()
            context.getDetective().incrementDeduceCount();
            System.out.println("Deductions used: " + context.getDetective().getDeduceCount());
        } else {
            System.out.println("No such object in this room.");
        }
    }
}