package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

// A command for a player to initiate the final exam. The logic for handling this is
// complex, so it is delegated to the execution context.
public class FinalExamCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 104L;

  public FinalExamCommand() {
    super(true);
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String initiatingPlayerId = getPlayerId();
    context.processFinalExamAttempt(initiatingPlayerId);
  }

  @Override
  public String getDescription() {
    return "Initiate or request the final exam to solve the case.";
  }
}
