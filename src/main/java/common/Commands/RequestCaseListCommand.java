package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

/**
 * A command representing a client's request to refresh the list of available cases from the server.
 */
public class RequestCaseListCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 113L;

  public RequestCaseListCommand() {
    // This command is used in the lobby and does not require a case to be started.
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    // This command should always be handled by the GameSessionManager. If it's
    // received by a GameContext, it indicates a logic or routing error, as the
    // player is already inside a game session.
    System.err.println(
        "SERVER WARNING: RequestCaseListCommand reached GameContextServer for player "
            + getPlayerId()
            + ". This command should be handled by GameSessionManager.");
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Case list can only be requested from the lobby."));
  }

  @Override
  public String getDescription() {
    // The description is primarily for the client's 'help' display.
    return "Refreshes and shows the list of available cases from the server.";
  }
}
