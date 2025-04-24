package Commands;

import Core.GameContext;
import Core.GameObject;
import Core.Room;
import java.util.Map;

public class MoveCommand extends BaseCommand {
  public MoveCommand() {
    super(true); // Requires the case to be started
  }

  @Override
  public void executeCommand(String[] args, GameContext context) {
    // Extract direction from arguments
    String direction = args[1].toLowerCase();

    // Get the detective's current room
    Room currentRoom = context.getDetective().getCurrentRoom();

    // Attempt to move the player
    Room newRoom = currentRoom.getNeighbor(direction);
    if (newRoom == null) {
      System.out.println("You can't move in that direction.");
      return;
    }

    // Update the detective's current room
    context.getDetective().setCurrentRoom(newRoom);

    // Update suspect and Watson movements
    context.updateMovements();

    // Display updated room information
    displayRoomInformation(newRoom, context);
  }

  private void displayRoomInformation(Room room, GameContext context) {
    // Print room description
    System.out.println("\n" + room.getDescription());

    // Print notable features (objects in the room)
    System.out.println("\nNotable features:");
    Map<String, GameObject> objects = room.getObjects();
    if (objects.isEmpty()) {
      System.out.println("- No notable features in this room.");
    } else {
      for (GameObject obj : objects.values()) {
        System.out.println("- " + obj.getDescription());
      }
    }

    // Print objects present in the room
    System.out.println("\n" + room.getObjectsDescription());

    // Print exits
    System.out.println(room.getExitsDescription());

    // Print occupants in the room
    System.out.println(context.getOccupantsDescription());
  }

  @Override
  public String getDescription() {
    return "Move north, south, east, west, up, or down.";
  }
}
