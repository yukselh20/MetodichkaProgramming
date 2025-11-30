package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// this command object to represent a client's request to host a new game.
// It contains the necessary data for the server to set up a new session.
public class HostGameCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(HostGameCommand.class);
  private static final long serialVersionUID = 114L;

  private final String caseTitle;
  private final boolean isPublic;

  public HostGameCommand(String caseTitle, boolean isPublic) {
    super(false);
    this.caseTitle = Objects.requireNonNull(caseTitle, "Case title cannot be null");
    if (caseTitle.isBlank()) throw new IllegalArgumentException("Case title cannot be blank.");
    this.isPublic = isPublic;
  }

  public String getCaseTitle() {
    return caseTitle;
  }

  public boolean isPublic() {
    return isPublic;
  }

  @Override
  protected void executeCommand(String[] args_unused, ICommandContext context) {
    // This command should be handled by the GameSessionManager before it reaches a
    // specific GameContext. This block is a safety net to catch message routing errors.
    logger.warn(
        "HostGameCommand reached GameContextServer for player {}. This indicates a routing error.",
        getPlayerId());
    context.sendResponseToPlayer(
        getPlayerId(),
        new common.dto.TextMessage("Error: Cannot host a new game while already in a session."));
  }

  @Override
  public String getDescription() {
    return "Requests to host a new game lobby for a selected case.";
  }
}
