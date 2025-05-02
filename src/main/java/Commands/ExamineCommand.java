package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;

public class ExamineCommand extends BaseCommand {
  public ExamineCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    if (args.length < 2) {
      System.out.println("Usage: examine [object]");
      return;
    }

    String objectName = args[1].toLowerCase(); // Normalize object name
    Room currentRoom = context.getCurrentRoom();

    // Retrieve the object from the current room
    GameObject obj = currentRoom.getObject(objectName);

    if (obj != null) {
      // Display the detailed description of the object
      System.out.println("You examine the " + objectName + ": " + obj.getExamine());

      // Add the object's description to the journal only if it's not already there
      String journalEntry = "Examined: " + objectName + " - " + obj.getExamine();
      context.getJournal().addEntry(journalEntry);
    } else {
      System.out.println("No such object in this room.");
    }
  }

  @Override
  public String getDescription() {
    return "Inspect an item for clues.";
  }
}
