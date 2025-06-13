package common.Commands;

import Core.Detective;
import Core.Room;
import Core.Suspect;
import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/** A command for a player to interrogate a suspect who is in the same room. */
public class QuestionCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 110L;
  private final String suspectName;

  public QuestionCommand(String suspectName) {
    super(true);
    this.suspectName = Objects.requireNonNull(suspectName, "Suspect name cannot be null");
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

    // The context is responsible for finding the suspect in the game world.
    Optional<Suspect> suspectOpt = context.findSuspect(suspectName);

    if (suspectOpt.isPresent()) {
      Suspect suspect = suspectOpt.get();
      // The command succeeds only if the suspect is physically present in the player's room.
      if (suspect.getCurrentRoom().equals(currentRoom)) {
        String statement = suspect.getStatement();
        String responseMsg = suspect.getName() + " says: \"" + statement + "\"";
        context.sendResponseToPlayer(playerId, new TextMessage(responseMsg));

        // The suspect's statement is automatically recorded in the journal for later review.
        String journalEntry = "Questioned " + suspect.getName() + ": " + statement;
        context.addEntryToJournal(journalEntry, playerId);
      } else {
        context.sendResponseToPlayer(
            playerId, new TextMessage(suspect.getName() + " is not in this room."));
      }
    } else {
      context.sendResponseToPlayer(
          playerId,
          new TextMessage("There is no suspect named '" + suspectName + "' in this case."));
    }
  }

  @Override
  public String getDescription() {
    return "Interrogate a suspect in your current room: question [suspect_name]";
  }
}
