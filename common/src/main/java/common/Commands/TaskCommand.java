package common.Commands;

import Core.TaskList;
import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.List;

/** A command that allows the player to view their current objectives for the case. */
public class TaskCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 112L;

  public TaskCommand() {
    // Viewing tasks is a core in-game action.
    super(true);
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    // The command requests the TaskList from the context, keeping it separate
    // from the game's state storage details.
    TaskList taskList = context.getTaskList();

    if (taskList == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error: Task list is not available for this case."));
      return;
    }

    List<String> tasks = taskList.getTasks();

    // The response is formatted into a user-friendly list before being sent.
    StringBuilder response = new StringBuilder("--- Current Tasks ---");
    if (tasks.isEmpty()) {
      response.append("\nNo specific tasks available right now.");
    } else {
      for (int i = 0; i < tasks.size(); i++) {
        response.append(String.format("\n%d. %s", i + 1, tasks.get(i)));
      }
    }
    response.append("\n--- End of Tasks ---");

    context.sendResponseToPlayer(playerId, new TextMessage(response.toString()));
  }

  @Override
  public String getDescription() {
    return "View your current investigation objectives/tasks.";
  }
}
