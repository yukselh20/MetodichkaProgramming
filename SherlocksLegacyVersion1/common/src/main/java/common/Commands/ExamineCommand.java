package common.Commands;

import Core.Detective;
import Core.GameObject;
import Core.Room;
import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;

// This command allows a player to inspect an object in their current room,
// revealing a description and adding the finding to their journal.
public class ExamineCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 103L;
  private final String targetObjectName;

  public ExamineCommand(String targetObjectName) {
    super(true);
    this.targetObjectName =
        Objects.requireNonNull(targetObjectName, "Target object name cannot be null").toLowerCase();
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

    GameObject obj = currentRoom.getObject(targetObjectName);

    if (obj != null) {
      // If a specific 'examine' text exists, use it; otherwise, fall back
      // to the object's general description for a consistent user experience.
      String examineText = obj.getExamine();
      if (examineText == null || examineText.trim().isEmpty()) {
        examineText = obj.getDescription();
      }

      String responseMsg = "You examine the " + targetObjectName + ": " + examineText;
      context.sendResponseToPlayer(playerId, new TextMessage(responseMsg));

      String journalEntry = "Examined " + targetObjectName + ": " + examineText;
      context.addEntryToJournal(journalEntry, playerId);
    } else {
      context.sendResponseToPlayer(
          playerId, new TextMessage("There is no '" + targetObjectName + "' to examine here."));
    }
  }

  @Override
  public String getDescription() {
    return "Inspect an item or feature for clues: examine [object_name]";
  }
}
