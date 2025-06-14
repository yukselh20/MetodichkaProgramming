package common.Commands;

import common.ICommandContext;
import common.dto.TextMessage;
import java.io.Serializable;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base for all command objects, implementing the Template Method pattern. It handles
 * shared functionality like pre-execution checks and error handling, allowing concrete command
 * subclasses to focus solely on their specific logic.
 */
public abstract class BaseCommand implements Command, Serializable {

  private static final Logger logger = LoggerFactory.getLogger(BaseCommand.class);

  private static final long serialVersionUID = 100L;

  // This flag determines if the command can be used before the 'start case' command.
  private final boolean requiresCaseStarted;

  // The server sets this ID after receiving the command, so the command
  // knows which player to attribute its actions and responses to.
  private String playerId;

  protected BaseCommand(boolean requiresCaseStarted) {
    this.requiresCaseStarted = requiresCaseStarted;
  }

  /**
   * The final, overarching execute method that performs pre-flight checks. This method is marked
   * 'final' to prevent subclasses from overriding it, ensuring that all commands go through the
   * same validation and error handling.
   */
  @Override
  public final void execute(String[] args, ICommandContext context) {
    Objects.requireNonNull(context, "Command context cannot be null for command execution.");

    if (this.playerId == null) {
      logger.error(
          "CRITICAL SERVER ERROR: Command executed with null playerId! Command: {}",
          this.getClass().getSimpleName());
      return;
    }

    // Using the ICommandContext abstraction allows this check to be performed
    // without the command needing to know about any specific server implementation.
    if (requiresCaseStarted && !context.isCaseStarted(this.playerId)) {
      context.sendResponseToPlayer(
          this.playerId,
          new TextMessage("Error: The case has not started yet. Use 'start case' command."));
      return;
    }

    try {
      // After validation, this method delegates to the abstract `executeCommand`,
      // which contains the unique logic for each concrete command.
      executeCommand(args, context);
    } catch (Exception e) {
      // This generic catch block ensures that an error in a single command
      // does not crash the entire game session for the player.
      logger.error(
          "Exception during execution of command {} for player {}",
          this.getClass().getSimpleName(),
          playerId,
          e);
      context.sendResponseToPlayer(
          playerId, new TextMessage("An internal error occurred while processing your command."));
    }
  }

  /**
   * The abstract method that concrete subclasses must implement. This is where the core logic of
   * the command resides.
   */
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
