package Core;

import java.util.Random;

/**
 * An abstract base class for any character (player or NPC) that can occupy and move between rooms
 * in the game world.
 */
public abstract class MovableCharacter {
  protected Room currentRoom;
  // Each character instance gets its own Random object for independent decision-making.
  protected final Random random = new Random();

  public Room getCurrentRoom() {
    return currentRoom;
  }

  public void setCurrentRoom(Room room) {
    // A null room indicates the character has been removed from the game board.
    this.currentRoom = room;
  }
}
