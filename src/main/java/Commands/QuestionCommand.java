package Commands;

import Core.GameContext;
import Core.Suspect;

public class QuestionCommand extends BaseCommand {
  public QuestionCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    if (args.length < 2) {
      System.out.println("Usage: question [suspect]");
      return;
    }

    String suspectName = args[1];
    Suspect suspect = context.getSuspect(suspectName); // Directly use getSuspect from GameContext

    if (suspect != null) {
      if (suspect.getCurrentRoom()
          == context.getCurrentRoom()) { // Use getCurrentRoom from GameContext
        String statement = suspect.getStatement();
        System.out.println(statement);

        // Add the suspect's statement to the journal only if it's not already there
        String journalEntry = suspect.getName() + ": " + statement;
        context.getJournal().addEntry(journalEntry);
      } else {
        System.out.println("That suspect is not in this room.");
      }
    } else {
      System.out.println("Suspect not found: " + suspectName);
    }
  }

  @Override
  public String getDescription() {
    return "Interrogate a suspect.";
  }
}
