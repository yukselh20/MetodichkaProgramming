package Commands;

import Core.GameContext;

public class JournalAddCommand extends BaseCommand {
  public JournalAddCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {

    if (args.length < 3) {
      System.out.println("Usage: journal add [note]");
      return;
    }
    StringBuilder note = new StringBuilder();
    for (int i = 2; i < args.length; i++) {
      note.append(args[i]).append(" ");
    }
    context.getJournal().addEntry(note.toString().trim());
    System.out.println("Note added to journal.");
  }

  @Override
  public String getDescription() {
    return "Save a clue in your journal.";
  }
}
