package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

/**
 * A command for a player to initiate the final exam to solve the case. The logic for handling this
 * is complex (e.g., checking if the player is the host), so it is delegated to the execution
 * context.
 */
public class FinalExamCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 104L;

  public FinalExamCommand() {
    // The final exam can only be started after the main investigation has begun.
    super(true);
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String initiatingPlayerId = getPlayerId();

    // By delegating, this command object remains simple. The context is
    // responsible for determining if the player is the host (who can start
    // the exam) or a guest (who can only request it).
    context.processFinalExamAttempt(initiatingPlayerId);
  }

  @Override
  public String getDescription() {
    return "Initiate or request the final exam to solve the case.";
  }
}
