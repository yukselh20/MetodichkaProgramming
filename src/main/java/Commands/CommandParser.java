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

    // Normalize input by trimming, collapsing multiple spaces, and converting to lowercase
    String normalizedInput = input.trim().replaceAll("\\s+", " ").toLowerCase();

    // Handle multi-word commands explicitly
    if (normalizedInput.startsWith("final exam")) return "final exam";
    if (normalizedInput.startsWith("ask watson")) return "ask watson";
    if (normalizedInput.startsWith("start case")) return "start case";
    if (normalizedInput.startsWith("journal add")) return "journal add";

    // Handle single-word commands with optional arguments
    String[] tokens = normalizedInput.split(" ");
    String commandName = tokens[0];

    // For "move", ensure the direction is extracted correctly even with extra spaces
    if (commandName.equals("move") && tokens.length > 1) {
      return "move"; // Return "move" as the command name
    }

    // Default: Use the first word as the command name
    return commandName;
  }
}
