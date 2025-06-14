package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A command representing a client's request for a list of open public games. */
public class ListPublicGamesCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(ListPublicGamesCommand.class);
  private static final long serialVersionUID = 115L;

  public ListPublicGamesCommand() {
    // This command is used in the lobby, so it can be executed anytime.
    super(false);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    // This action should be handled by the GameSessionManager, which oversees
    // all lobbies. If it reaches a specific GameContext, the player is already
    // in a game, so this block handles the error condition.
    logger.warn(
        "ListPublicGamesCommand reached GameContextServer for player {}. This indicates a message routing error and should be handled by GameSessionManager.",
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
