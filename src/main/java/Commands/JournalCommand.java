package Commands;

import Core.GameContext;
import Core.Journal;
import java.util.List;

public class JournalCommand extends BaseCommand {
    public JournalCommand() {
        super(true); // Requires the case to be started
    }
    @Override
    public void executeCommand(String[] args, GameContext context) {
        Journal journal = context.getJournal();
        List<String> entries = journal.getEntries();

        if (entries.isEmpty()) {
            System.out.println("╔══════════════════════════════════════════════════════╗");
            System.out.println("║                   Journal is empty.                  ║");
            System.out.println("╚══════════════════════════════════════════════════════╝");
            return;
        }

        // Handle search
        if (args.length > 1 && !args[1].isEmpty()) {
            String keyword = args[1].toLowerCase();
            System.out.println("╔══════════════════════════════════════════════════════╗");
            System.out.printf("║ Search results for '%s':%n", keyword);
            System.out.println("╠══════════════════════════════════════════════════════╣");

            boolean found = false;
            for (String entry : entries) {
                if (entry.toLowerCase().contains(keyword)) {
                    System.out.printf("║ - %s%n", entry);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("║ No matching entries found.                           ║");
            }

            System.out.println("╚══════════════════════════════════════════════════════╝");
        } else {
            // Display all entries with enhanced formatting
            System.out.println("╔══════════════════════════════════════════════════════╗");
            System.out.println("║                  Journal Contents                    ║");
            System.out.println("╠══════════════════════════════════════════════════════╣");

            for (String entry : entries) {
                System.out.printf("║ - %s%n", entry);
            }

            System.out.println("╚══════════════════════════════════════════════════════╝");
        }
    }

    @Override
    public String getDescription() {
        return "Review your collected clues. Usage: journal or journal [keyword] (to search for keywords)";
    }
}