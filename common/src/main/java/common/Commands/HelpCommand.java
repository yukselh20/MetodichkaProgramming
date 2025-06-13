package common.Commands;

import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;

/**
 * A command that provides the player with a list of all available commands. The help text is static
 * and generated on the server.
 */
public class HelpCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 105L;

  public HelpCommand() {
    // The help command should be available at all times.
    super(false);
  }

  @Override
  public void executeCommand(String[] args_unused, ICommandContext context) {
    // The full help text is maintained here on the server side. This ensures
    // a single source of truth for command documentation.
    String helpText =
        """
            Available commands (Lobby/Setup):
              register [user] [pw] - Register a new user account.
              login [user] [pw]    - Log in to your user account.
              cases              - Refresh and show available cases to host/join.
              host public [case#] - Host a public game for the selected case number.
              host private [case#]- Host a private game for the selected case number.
              games              - List available public games to join.
              join public [game#] - Join a listed public game by its number.
              join private [code] - Join a private game using a 4-digit code.
              /chat [message]    - Send a chat message (in lobby or game).
              quit               - Exit the game client.

            Available commands (In-Game):
              start case         - (Host only) Begin the investigation for the loaded case.
              tasks              - View your current objectives.
              look               - Describe your current surroundings.
              move [direction]   - Move (north, south, east, west, up, down).
              examine [object]   - Inspect an item or feature for clues.
              question [suspect] - Ask a suspect for their statement.
              ask watson         - Ask Dr. Watson for a hint (if present).
              journal add [note] - Add a custom note to your journal.
              journal            - Review your collected journal entries.
              journal [keyword]  - Search journal entries for a keyword.
              deduce [object]    - Attempt a deduction about an object.
              final exam         - (Host only to initiate) Start final case questions.
            Type 'help' to see this list again.
            """;

    context.sendResponseToPlayer(getPlayerId(), new TextMessage(helpText));
  }

  @Override
  public String getDescription() {
    return "Show this list of available commands.";
  }
}
