package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

/**
 * A command that represents a player's attempt to start the investigation. The core logic is
 * delegated to the context to handle host/guest permissions.
 */
public class StartCaseCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 111L;

  public StartCaseCommand() {
    // This command is unique because it's the action that *causes* the case to
    // be started. Therefore, the command itself doesn't require the case to
    // already be in a 'started' state.
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    String initiatingPlayerId = getPlayerId();

    // The command delegates the starting logic to the context. This allows the
    // server to handle the complex rules: if the player is the host, the case
    // starts immediately; if they are a guest, a request is sent to the host.
    context.processStartCaseAttempt(initiatingPlayerId);
  }

  @Override
  public String getDescription() {
    return "Begin the investigation for the currently loaded case.";
  }
}
