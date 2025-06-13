package common.Commands;

import Core.Detective;
import Core.Room;
import common.ICommandContext;
import common.dto.RoomDescriptionDTO;
import common.dto.TextMessage;
import java.io.Serializable;

/** A command that allows a player to get a description of their current surroundings. */
public class LookCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 108L;

  public LookCommand() {
    // Looking around is a core in-game action.
    super(true);
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    Detective detective = context.getPlayerDetective(playerId);
    if (detective == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error: Could not find your detective data."));
      return;
    }

    Room currentRoom = detective.getCurrentRoom();
    if (currentRoom == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error: You don't seem to be in any room."));
      return;
    }

    // This command delegates the complex task of creating a room description
    // to the context. This keeps the command simple and centralizes the logic for
    // what a player sees in a room (e.g., objects, other players, NPCs).
    RoomDescriptionDTO roomDTO = context.generateRoomDescriptionDTO(currentRoom, playerId);
    if (roomDTO != null) {
      context.sendResponseToPlayer(playerId, roomDTO);
    } else {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error: Could not generate room description."));
    }
  }

  @Override
  public String getDescription() {
    return "Look around your current location.";
  }
}
