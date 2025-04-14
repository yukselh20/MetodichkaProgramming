package Commands;

public class CommandParser {
    /**
     * Parses the user input and determines the command name.
     *
     * @param input The raw user input.
     * @return The normalized command name (e.g., "final exam", "journal add").
     */
    public static String parseCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        // Normalize input by trimming and converting to lowercase
        String normalizedInput = input.toLowerCase().trim();

        // Handle multi-word commands explicitly
        if (normalizedInput.startsWith("final exam")) return "final exam";
        if (normalizedInput.startsWith("journal add")) return "journal add";
        if (normalizedInput.startsWith("ask watson")) return "ask watson";
        if (normalizedInput.startsWith("start case")) return "start case";

        // Default: Use the first word as the command name
        return normalizedInput.split(" ")[0];
    }
}
