package common.Commands;

import Core.Detective;
import Core.Room;
import common.ICommandContext;
import common.dto.RoomDescriptionDTO;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;

// This command moves a player from their current room to an adjacent one.
public class MoveCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 109L;
  private final String direction;

  public MoveCommand(String direction) {
    super(true);
    this.direction = Objects.requireNonNull(direction, "Direction cannot be null").toLowerCase();
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
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

    Room nextRoom = currentRoom.getNeighbor(direction);

    if (nextRoom == null) {
      context.sendResponseToPlayer(
          playerId, new TextMessage("You cannot move " + direction + " from here."));
      return;
    }

    // After a successful move, update the player's state and notify the context,
    // which may trigger other game logic like NPC movement.
    Room oldRoom = currentRoom;
    detective.setCurrentRoom(nextRoom);
    context.handlePlayerMovement(playerId, oldRoom, nextRoom);
    context.sendResponseToPlayer(playerId, new TextMessage("You move " + direction + "."));
    RoomDescriptionDTO roomDTO = context.generateRoomDescriptionDTO(nextRoom, playerId);
    if (roomDTO != null) {
      context.sendResponseToPlayer(playerId, roomDTO);
    } else {
      context.sendResponseToPlayer(
          playerId, new TextMessage("Error displaying new room information."));
    }
  }

  @Override
  public String getDescription() {
    return "Move in a direction: move [north|south|east|west|up|down]";
  }
}
