package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This command represents a client's request to refresh the list of available cases.
public class RequestCaseListCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(RequestCaseListCommand.class);
  private static final long serialVersionUID = 113L;

  public RequestCaseListCommand() {
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    logger.warn(
        "RequestCaseListCommand reached GameContextServer for player {}. This is a routing error.",
        getPlayerId());
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Case list can only be requested from the lobby."));
  }

  @Override
  public String getDescription() {
    return "Refreshes and shows the list of available cases from the server.";
  }
}
