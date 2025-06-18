package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This command represents a client's request for a list of open public games.
public class ListPublicGamesCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(ListPublicGamesCommand.class);
  private static final long serialVersionUID = 115L;

  public ListPublicGamesCommand() {
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    logger.warn(
        "ListPublicGamesCommand reached GameContextServer for player {}. This is a routing error.",
        getPlayerId());
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot list public games while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Lists currently open public game lobbies you can join.";
  }
}
