package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;

public class DeduceCommand extends BaseCommand {
  public DeduceCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    if (args.length < 2) {
      System.out.println("Usage: deduce [object]");
      return;
    }

    String objectName = args[1].toLowerCase();
    Room currentRoom = context.getCurrentRoom();

    // Ensure Room has getObject()
    GameObject obj = currentRoom.getObject(objectName);

    if (obj != null) {
      String clue = obj.deduce();
      System.out.println("Deduction: " + clue);

      // Add the object name and clue to the journal
      String journalEntry = "Deduced from " + objectName + ": " + clue;
      context.getJournal().addEntry(journalEntry);

      // Increment deduce count only if the object hasn't been deduced before
      context.getDetective().incrementDeduceCount(objectName);
      System.out.println("Deductions used: " + context.getDetective().getDeduceCount());
    } else {
      System.out.println("No such object in this room.");
    }
  }

  @Override
  public String getDescription() {
    return "Use Sherlock Holmes skills to make a deduction but be careful overusing it will decrease your rank as a detective.";
  }
}
