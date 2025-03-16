package Commands;

import Core.GameContext;

public class HelpCommand implements Command {
    @Override
    public void execute(String[] args, GameContext context) {
        System.out.println("Available commands:");
        System.out.println("  start case         - Show the case description.");
        System.out.println("  tasks              - View your investigation guide.");
        System.out.println("  look               - View your surroundings.");
        System.out.println("  move [direction]   - Move north, south, east, west, up, or down.");
        System.out.println("  examine [object]   - Inspect an item for clues.");
        System.out.println("  question [suspect] - Interrogate a suspect.");
        System.out.println("  ask watson         - Ask Doctor Watson for a hint or his point of view.");
        System.out.println("  journal add [note] - Save a clue in your journal.");
        System.out.println("  journal            - Review your collected clues.");
        System.out.println("  deduce             - Use Sherlock Holmes skills to make a deduction but be careful overusing it will decrease your rank as a detective.");
        System.out.println("  final exam         - Answer key questions to solve the case.");
        System.out.println("  add case           - Add a new mystery case.");
    }
}

