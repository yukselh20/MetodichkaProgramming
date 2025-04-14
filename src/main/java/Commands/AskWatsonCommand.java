package Commands;

import Core.GameContext;

public class AskWatsonCommand extends BaseCommand {
  public AskWatsonCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {

    // Use getBuilding() instead of getMansion()
    if (context.getWatson().getCurrentRoom().getName().equals(context.getCurrentRoom().getName())) {
      context.getWatson().provideHint();
    } else {
      System.out.println("Dr. Watson is not in this room.");
    }
  }

  @Override
  public String getDescription() {
    return "Ask Doctor Watson for a hint or his point of view.";
  }
}
