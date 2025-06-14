package common.Commands;

import common.ICommandContext;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command object representing a client's request to host a new game. It contains the necessary
 * data for the server to set up a new session.
 */
public class HostGameCommand extends BaseCommand implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(HostGameCommand.class);
  private static final long serialVersionUID = 114L;

  private final String caseTitle;
  private final boolean isPublic;

  public HostGameCommand(String caseTitle, boolean isPublic) {
    // This command can be executed from the lobby, before a case has started.
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
    // This command should be intercepted and handled by the GameSessionManager
    // before it ever reaches a specific GameContext. This block acts as a
    // safety net to catch logical errors in message routing.
    logger.warn(
        "HostGameCommand reached GameContextServer for player {}. This indicates a message routing error and should be handled by GameSessionManager.",
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
