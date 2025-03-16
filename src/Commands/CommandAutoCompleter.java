package Commands;

import java.util.HashMap;
import java.util.Map;

public class CommandAutoCompleter {
    private static final Map<String, String> validCommands = new HashMap<>();

    static {
        validCommands.put("start case", "start case");
        validCommands.put("look", "look");
        validCommands.put("move north", "move north");
        validCommands.put("move south", "move south");
        validCommands.put("move east", "move east");
        validCommands.put("move west", "move west");
        validCommands.put("move up", "move up");
        validCommands.put("move down", "move down");
        validCommands.put("enter", "enter [room]");
        validCommands.put("examine", "examine [object]");
        validCommands.put("question", "question [suspect]");
        validCommands.put("ask watson", "ask watson");
        validCommands.put("journal add", "journal add [note]");
        validCommands.put("journal", "journal");
        validCommands.put("deduce", "deduce");
        validCommands.put("final exam", "final exam");
        validCommands.put("add case", "add case");
        validCommands.put("help", "help");
    }

    public static String getSuggestedCommand(String input) {
        input = input.toLowerCase().trim();
        return validCommands.getOrDefault(input, input); // Return corrected command or original input
    }
}