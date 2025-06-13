package common.Commands;

import Core.Detective;
import Core.DoctorWatson;
import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;

/** A command that allows the player to ask Dr. Watson for a hint. */
public class AskWatsonCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 101L;

  public AskWatsonCommand() {
    // This command is only valid after the investigation has started.
    super(true);
  }

  @Override
  public void executeCommand(String[] args, ICommandContext context) {
    String playerId = getPlayerId();
    Detective detective = context.getPlayerDetective(playerId);
    DoctorWatson watson = context.getWatson();

    if (detective == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error: Could not find your detective data."));
      return;
    }
    if (watson == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Dr. Watson seems to be unavailable in this case."));
      return;
    }

    // The command only succeeds if Dr. Watson is in the same room as the player.
    if (watson.getCurrentRoom() != null
        && watson.getCurrentRoom().equals(detective.getCurrentRoom())) {
      String hint = watson.provideHint();
      context.sendResponseToPlayer(playerId, new TextMessage("Watson: " + hint));
    } else {
      context.sendResponseToPlayer(playerId, new TextMessage("Dr. Watson is not in this room."));
    }
  }

  @Override
  public String getDescription() {
    return "Ask Doctor Watson for a hint or his point of view (if he is in the room).";
  }
}
