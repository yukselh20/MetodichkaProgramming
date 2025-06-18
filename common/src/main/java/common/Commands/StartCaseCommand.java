package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

// This command represents a player's attempt to start the investigation.
public class StartCaseCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 111L;

  public StartCaseCommand() {
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    String initiatingPlayerId = getPlayerId();
    context.processStartCaseAttempt(initiatingPlayerId);
  }

  @Override
  public String getDescription() {
    return "Begin the investigation for the currently loaded case.";
  }
}
