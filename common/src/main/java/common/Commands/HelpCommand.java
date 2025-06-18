package common.Commands;

import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;

// This command provides the player with a list of all available commands.
public class HelpCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 105L;

  public HelpCommand() {
    super(true);
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    String helpText =
        """
            --- Available In-Game Commands ---
              look               - Describe your current surroundings.
              move [direction]   - Move (north, south, east, west, up, down).
              tasks              - View your current investigation objectives.

              examine [object]   - Inspect an item or feature for clues.
              question [suspect] - Ask a suspect for their statement.
              ask watson         - Ask Dr. Watson for a hint (if present).
              deduce [object]    - Attempt a deduction about an object.

              journal            - Review all of your collected journal entries.
              journal [keyword]  - Search your journal for a specific keyword.
              journal add [note] - Add a custom note to your journal.

              /chat [message]    - Send a chat message to other players.

              final exam         - (Host only) Initiate the final exam to solve the case.
              start case         - (Host only, at start) Begin the investigation.
              quit               - Disconnect from the game session.
            """;

    context.sendResponseToPlayer(getPlayerId(), new TextMessage(helpText));
  }

  @Override
  public String getDescription() {
    return "Show a list of available in-game commands.";
  }
}
