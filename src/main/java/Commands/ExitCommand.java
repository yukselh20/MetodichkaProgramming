package Commands;

import Core.GameContext;

public class ExitCommand implements Command {
  @Override
  public void execute(String[] args, GameContext context) {

    System.out.println("Exiting the game. Goodbye!");

    // Signal to exit the current game loop
    context.setExitCurrentGame(true);
  }

  @Override
  public String getDescription() {
    return "Exits the current case or the game entirely.";
  }
}
