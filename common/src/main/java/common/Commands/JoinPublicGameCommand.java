package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;

/**
 * A command representing a client's request to join a specific public game lobby identified by its
 * session ID.
 */
public class JoinPublicGameCommand extends BaseCommand implements Serializable {
  private static final long serialVersionUID = 116L;
  private final String targetGameSessionId;

  public JoinPublicGameCommand(String targetGameSessionId) {
    // Joining a game happens in the lobby, before a case is started.
    super(false);
    this.targetGameSessionId =
        Objects.requireNonNull(targetGameSessionId, "Target session ID cannot be null");
    if (targetGameSessionId.isBlank())
      throw new IllegalArgumentException("Target session ID cannot be blank.");
  }

  public String getTargetGameSessionId() {
    return targetGameSessionId;
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    // Similar to other lobby commands, this should be handled by the
    // GameSessionManager. This code block serves as a safeguard against
    // incorrect message routing.
    System.err.println(
        "SERVER WARNING: JoinPublicGameCommand reached GameContextServer for player "
            + getPlayerId()
            + ". This command should be handled by GameSessionManager.");
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot join a game while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Attempts to join a specific public game lobby by its ID.";
  }
}
