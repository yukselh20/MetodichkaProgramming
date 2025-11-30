package common.Commands;

import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// abstract base for all command objects, using the Template Method pattern.
// It handles shared functionality like pre-execution checks and error handling, allowing
// my concrete command subclasses to focus only on their specific logic.
public abstract class BaseCommand implements Command, Serializable {

  private static final Logger logger = LoggerFactory.getLogger(BaseCommand.class);

  private static final long serialVersionUID = 100L;

  private final boolean requiresCaseStarted;

  private String playerId;

  protected BaseCommand(boolean requiresCaseStarted) {
    this.requiresCaseStarted = requiresCaseStarted;
  }

  @Override
  public final void execute(String[] args, ICommandContext context) {
    Objects.requireNonNull(context, "Command context cannot be null for command execution.");

    if (this.playerId == null) {
      logger.error(
          "CRITICAL SERVER ERROR: Command executed with null playerId! Command: {}",
          this.getClass().getSimpleName());
      return;
    }

    if (requiresCaseStarted && !context.isCaseStarted(this.playerId)) {
      context.sendResponseToPlayer(
          this.playerId,
          new TextMessage("Error: The case has not started yet. Use 'start case' command."));
      return;
    }

    try {
      executeCommand(args, context);
    } catch (Exception e) {
      logger.error(
          "Exception during execution of command {} for player {}",
          this.getClass().getSimpleName(),
          playerId,
          e);
      context.sendResponseToPlayer(
          playerId, new TextMessage("An internal error occurred while processing your command."));
    }
  }

  protected abstract void executeCommand(String[] args, ICommandContext context);

  @Override
  public abstract String getDescription();

  @Override
  public final void setPlayerId(String playerId) {
    this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be set to null");
  }

  @Override
  public final String getPlayerId() {
    return this.playerId;
  }
}
