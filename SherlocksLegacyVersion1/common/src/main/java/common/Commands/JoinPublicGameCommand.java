package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// I created this command to represent a client's request to join a specific public
// game lobby, identified by its session ID.
public class JoinPublicGameCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(JoinPublicGameCommand.class);
  private static final long serialVersionUID = 116L;
  private final String targetGameSessionId;

  public JoinPublicGameCommand(String targetGameSessionId) {
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
    logger.warn(
        "JoinPublicGameCommand reached GameContextServer for player {}. This is a routing error.",
        getPlayerId());
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot join a game while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Attempts to join a specific public game lobby by its ID.";
  }
}
