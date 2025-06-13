package common.Commands;

import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;

/** A command that allows a player to add a custom note to the shared journal. */
public class JournalAddCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 106L;
  private final String noteToAdd;

  public JournalAddCommand(String noteToAdd) {
    // A player can only add notes to the journal after the case has started.
    super(true);
    this.noteToAdd = Objects.requireNonNull(noteToAdd, "Note cannot be null or empty");
    if (noteToAdd.isBlank()) {
      throw new IllegalArgumentException("Note to add cannot be blank.");
    }
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String playerId = getPlayerId();
    // This command delegates the actual work of adding the entry to the context.
    // The context will handle timestamping and broadcasting the update to all players.
    context.addEntryToJournal(noteToAdd, playerId);
    context.sendResponseToPlayer(playerId, new TextMessage("Note added to journal."));
  }

  @Override
  public String getDescription() {
    return "Save a custom note in your journal: journal add [note text]";
  }
}
