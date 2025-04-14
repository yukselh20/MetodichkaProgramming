package Commands;

import Core.GameContext;
import java.util.Map;

public class HelpCommand extends BaseCommand {
  public HelpCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    System.out.println("Available commands:");

    // Dynamically retrieve the list of commands from CommandFactory
    Map<String, Command> commands = CommandFactory.getCommands();

    // Print each command and its description
    for (Map.Entry<String, Command> entry : commands.entrySet()) {
      String commandName = entry.getKey();
      String description = entry.getValue().getDescription();
      System.out.printf("  %-18s - %s%n", commandName, description);
    }
  }

  @Override
  public String getDescription() {
    return "Display this help message with a list of available commands.";
  }
}
