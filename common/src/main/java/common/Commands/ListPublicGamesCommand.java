package common.Commands;

import common.ICommandContext;
import java.io.Serializable;

/** A command representing a client's request for a list of open public games. */
public class ListPublicGamesCommand extends BaseCommand implements Serializable {
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
    System.err.println(
        "SERVER WARNING: ListPublicGamesCommand reached GameContextServer for player "
            + getPlayerId()
            + ". This command should be handled by GameSessionManager.");
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot list public games while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Lists currently open public game lobbies you can join.";
  }
}
