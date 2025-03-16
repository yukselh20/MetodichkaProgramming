package Commands;

import Core.GameContext;

public class JournalAddCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        if(args.length < 3) {
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
}

